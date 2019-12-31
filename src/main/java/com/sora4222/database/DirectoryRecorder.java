package com.sora4222.database;

import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.connectors.DatabaseQuery;
import com.sora4222.database.connectors.Deleter;
import com.sora4222.database.connectors.Inserter;
import com.sora4222.database.connectors.Updater;
import com.sora4222.database.directory.SetupDirectoryScan;
import com.sora4222.file.FileInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectoryRecorder {
  private static Logger logger = LogManager.getLogger();
  public static final HashMap<Path, WatchKey> directoriesWatching = new HashMap<>();
  public static final WatchService subscribeService;
  private static final HashMap<WatchEvent.Kind<Path>, DatabaseCommand> eventToCommandMap = new HashMap<>();
  
  static {
    eventToCommandMap.put(StandardWatchEventKinds.ENTRY_CREATE, DatabaseCommand.Insert);
    eventToCommandMap.put(StandardWatchEventKinds.ENTRY_DELETE, DatabaseCommand.Delete);
    eventToCommandMap.put(StandardWatchEventKinds.ENTRY_MODIFY, DatabaseCommand.Update);
    
    try {
      subscribeService = FileSystems.getDefault().newWatchService();
    } catch (IOException e) {
      logger.error("The subscribe service has failed to start", e);
      throw new RuntimeException(e);
    }
  }
  
  public static void main(String[] args) {
    setupScanning();
    startScanning();
  }
  
  @SuppressWarnings("InfiniteLoopStatement")
  static void startScanning() {
    logger.trace("startScanning");
    while (true) {
      scanOnce();
      sleepXMinutes(2);
    }
  }
  
  public static void sleepXMinutes(long x) {
    try {
      TimeUnit.SECONDS.sleep(x);
    } catch (InterruptedException e) {
      logger.error(e);
    }
  }
  
  static void scanOnce() {
    for (Map.Entry<Path, WatchKey> watchKey : directoriesWatching.entrySet()) {
      List<FileCommand> rowsToProcess = pollForFileChanges(watchKey);
      
      List<FileInformation> toDelete = filterFilesOfCommand(rowsToProcess, DatabaseCommand.Delete);
      List<FileInformation> toUpdate = filterFilesOfCommand(rowsToProcess, DatabaseCommand.Update);
      List<FileInformation> toInsert = filterFilesOfCommand(rowsToProcess, DatabaseCommand.Insert);
      
      Deleter.sendDeletesToDatabase(toDelete);
      Updater.sendUpdatesToDatabase(toUpdate);
      Inserter.insertRecordIntoDatabase(toInsert);
      
      watchKey.getValue().reset();
    }
  }
  
  private static List<FileInformation> filterFilesOfCommand(List<FileCommand> rowsToProcess,
                                                            DatabaseCommand commandType) {
    return rowsToProcess.parallelStream()
      .filter(fileCommand -> fileCommand.command.equals(commandType))
      .map(fileCommand -> fileCommand.information).collect(Collectors.toList());
  }
  
  private static List<FileCommand> pollForFileChanges(Map.Entry<Path, WatchKey> watchKey) {
    return watchKey
      .getValue()
      .pollEvents()
      .parallelStream()
      .map(watchEvent -> watchEventToFileCommand(watchEvent, watchKey.getKey()))
      .filter(fileCommand -> !fileCommand.command.equals(DatabaseCommand.BadEntry))
      .collect(Collectors.toList());
  }
  
  private static FileCommand watchEventToFileCommand(final WatchEvent watchEvent, final Path pathToDirectory) {
    try {
      @SuppressWarnings("unchecked") Path pathToFile = pathToDirectory.resolve(((WatchEvent<Path>) watchEvent).context());
      DatabaseCommand commandForChange = eventToCommandMap.get(watchEvent.kind());
      
      logger.debug("Command for change reported: " + commandForChange.toString());
      if (checkForDeletion(pathToFile, commandForChange))
        return new FileCommand(new FileInformation(pathToFile), commandForChange);
      
      FileInformation file = FileInformation.fromPath(pathToFile);
      if (!file.equals(FileInformation.EmptyFileInformation))
        return new FileCommand(file, commandForChange);
      else
        return new FileCommand(file, DatabaseCommand.BadEntry);
    } catch (ClassCastException e) {
      logger.info("A watch event couldn't be cast to a path, directory path: " + pathToDirectory.toString());
      return new FileCommand(null, DatabaseCommand.BadEntry);
    } catch (RuntimeException e) {
      logger.warn(e);
      return new FileCommand(null, DatabaseCommand.BadEntry);
    }
    
  }
  
  private static boolean checkForDeletion(Path pathToFile, DatabaseCommand commandForChange) {
    return commandForChange.equals(DatabaseCommand.Delete) ||
      (commandForChange.equals(DatabaseCommand.Update) && !pathToFile.toFile().exists());
  }
  
  static void setupScanning() {
    logger.trace("setupScanning");
    SetupDirectoryScan.walkThroughFolders();
  }
  
  
  private static List<FileCommand> findUpdatesToDatabase(final Path confDirPath) {
    logger.trace("findUpdatesToDatabase");
    
    try (Stream<Path> objectsInConfigurationDirectories = Files.walk(confDirPath)) {
      final List<FileInformation> existingFiles = objectsInConfigurationDirectories
        .parallel()
        .filter(path -> path.toFile().isFile())
        .filter(path -> filterPathsBasedOnRegexExcludes(path))
        .map(path -> FileInformation.fromPath(path))
        .filter(fileInformation -> !fileInformation.equals(FileInformation.EmptyFileInformation))
        .collect(Collectors.toList());
      
      return DatabaseQuery
        .queryTheDatabaseForFiles(existingFiles)
        .parallelStream()
        .filter(fileInformation -> !existingFiles.contains(fileInformation))
        .map(path -> getUpdateType(existingFiles, path))
        .collect(Collectors.toList());
    } catch (IOException e) {
      logger.error("Potentially a file walking error: ", e);
    }
    return new ArrayList<>();
  }
  
  private static FileCommand getUpdateType(final List<FileInformation> existingFiles, final FileInformation fileOfInterest) {
    if (existingFiles.contains(fileOfInterest))
      return new FileCommand(
        fileOfInterest,
        DatabaseCommand.Update);
    else
      return new FileCommand(
        fileOfInterest,
        DatabaseCommand.Delete);
  }
  
  
  public static List<FileInformation> gatherAllFilesUnderRootPath(final Path confDirPath) {
    try (Stream<Path> objectsInConfigurationDirectories = Files.walk(confDirPath)) {
      
      // Obtain all files under root folder
      return objectsInConfigurationDirectories
        .filter(path -> path.toFile().isFile())
        .filter(path -> filterPathsBasedOnRegexExcludes(path))
        .map(path -> FileInformation.fromPath(path))
        .filter(fileInformation -> !fileInformation.equals(FileInformation.EmptyFileInformation))
        .collect(Collectors.toList());
    } catch (IOException e) {
      logger.error(String.format("The configured path has had an error: %s", confDirPath.toString()), e);
      throw new RuntimeException(e);
    }
  }
  
  public static boolean filterPathsBasedOnRegexExcludes(Path path) {
    for (Predicate<String> patternCheck : ConfigurationManager.getConfiguration().getExcludeRegex()) {
      if (patternCheck.test(path.toAbsolutePath().toString().replace("\\", "/")))
        return false;
    }
    return true;
  }
}
