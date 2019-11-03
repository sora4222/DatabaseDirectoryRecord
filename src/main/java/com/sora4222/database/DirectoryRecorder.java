package com.sora4222.database;

import com.sora4222.database.configuration.ComputerProperties;
import com.sora4222.database.configuration.Config;
import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.connectors.*;
import com.sora4222.file.FileHasher;
import com.sora4222.file.FileInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectoryRecorder {
  private static Logger logger = LogManager.getLogger();
  private static Config config = ConfigurationManager.getConfiguration();
  private static final HashMap<Path, WatchKey> directoriesWatching = new HashMap<>();
  private static final WatchService subscribeService;
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
      for (Map.Entry<Path, WatchKey> watchKey : directoriesWatching.entrySet()) {
        List<FileCommand> rowsToProcess = watchKey
            .getValue()
            .pollEvents()
            .parallelStream()
            .map(watchEvent -> watchEventToFileCommand(watchEvent, watchKey.getKey()))
            .collect(Collectors.toList());
        
        List<FileInformation> toDelete = rowsToProcess.parallelStream()
            .filter(fileCommand -> fileCommand.command.equals(DatabaseCommand.Delete))
            .map(fileCommand -> fileCommand.information).collect(Collectors.toList());
        
        List<FileInformation> toUpdate = rowsToProcess.parallelStream()
            .filter(fileCommand -> fileCommand.command.equals(DatabaseCommand.Update))
            .map(fileCommand -> fileCommand.information).collect(Collectors.toList());
        
        List<FileInformation> toInsert = rowsToProcess.parallelStream()
            .filter(fileCommand -> fileCommand.command.equals(DatabaseCommand.Insert))
            .map(fileCommand -> fileCommand.information).collect(Collectors.toList());
        
        Deleter.sendDeletesToDatabase(toDelete);
        Updater.sendUpdatesToDatabase(toUpdate);
        Inserter.insertFilesIntoDatabase(toInsert);
        
        watchKey.getValue().reset();
      }
    }
  }
  
  private static FileCommand watchEventToFileCommand(final WatchEvent<?> watchEvent, final Path pathToDirectory) {
    
    FileInformation file = convertPathToFileInformation(pathToDirectory);
    DatabaseCommand commandForChange = eventToCommandMap.get(watchEvent.kind());
    
    return new FileCommand(file, commandForChange);
  }
  
  static void setupScanning() {
    logger.trace("setupScanning");
  
    //Subscribe to directories for the first time and seed
    for (Path confDirPath : config.getRootLocationsAsPaths()) {
      subscribeToChangesFromAllDirectories(confDirPath);
      
      logger.trace("Seeding all directories");
      //Seed all directories
      List<FileInformation> allFilesInThisRoot = gatherAllFilesUnderRootPath(confDirPath);
      List<FileInformation> allFilesInDatabaseForThisComputer = DatabaseEntries
          .getComputersFilesFromDatabase(confDirPath)
          .limit(DatabaseEntries.databaseRecordCount())
          .collect(Collectors.toList());
      
      List<FileInformation> filesNotInTheDatabase =
          allFilesInThisRoot
              .stream()
              .parallel()
              .peek((fileInformation -> logger.debug("FilterIn: " + fileInformation.getFullLocation())))
              .filter(fileInformation -> !allFilesInDatabaseForThisComputer.contains(fileInformation))
              .peek(fileInformation -> logger.debug("FilterOut: " + fileInformation.getFullLocation()))
              .collect(Collectors.toList());
  
      logger.info(String.format("During seeding %d files were not in the database.", filesNotInTheDatabase.size()));
      
      // Upload those files.
      Inserter.insertFilesIntoDatabase(filesNotInTheDatabase);
  
      if (!Boolean.getBoolean("skipUpdateAndDelete")) {
        logger.info("Initial update and delete processing");
        List<FileCommand> updates = findUpdatesToDatabase(confDirPath);
        Updater.sendUpdatesToDatabase(updates.parallelStream().filter(command -> command.getCommand().equals(DatabaseCommand.Update)).map(fileCommand -> fileCommand.getInformation()).collect(Collectors.toList()));
        Deleter.sendDeletesToDatabase(updates.parallelStream().filter(command -> command.getCommand().equals(DatabaseCommand.Delete)).map(fileCommand -> fileCommand.getInformation()).collect(Collectors.toList()));
        Inserter.insertFilesIntoDatabase(updates.parallelStream().filter(command -> command.getCommand().equals(DatabaseCommand.Insert)).map(fileCommand -> fileCommand.getInformation()).collect(Collectors.toList()));
      }
    }
  }
  
  private static List<FileCommand> findUpdatesToDatabase(final Path confDirPath) {
    logger.trace("findUpdatesToDatabase");
  
    try (Stream<Path> objectsInConfigurationDirectories = Files.walk(confDirPath)) {
      final List<FileInformation> existingFiles = objectsInConfigurationDirectories
          .parallel()
          .filter(path -> path.toFile().isFile())
          .map(path -> convertPathToFileInformation(path))
          .collect(Collectors.toList());
  
      return DatabaseQuery
          .allFilesAlreadyInBothComputerAndDatabase(existingFiles)
          .parallelStream()
          .filter(fileInformation -> !existingFiles.contains(fileInformation))
          .map(path -> getUpdateType(existingFiles, path))
          .collect(Collectors.toList());
    } catch (IOException e) {
      logger.error("Potentially a file walking error: ", e);
    }
    return new ArrayList<>();
  }
  
  private static FileInformation convertPathToFileInformation(final Path path) {
    FileHasher hasher = new FileHasher(path.toFile());
    return new FileInformation(path, ComputerProperties.computerName.get(), hasher.hashFile());
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
  
  private static void subscribeToChangesFromAllDirectories(final Path directory) {
    logger.trace("subscribeToChangesFromAllDirectories");
    try (Stream<Path> objectsInConfigurationDirectories = Files.walk(directory)) {
      objectsInConfigurationDirectories.parallel().filter(path -> path.toFile().isDirectory()).forEach(path -> {
        try {
          directoriesWatching.put(path, path.register(subscribeService,
              StandardWatchEventKinds.ENTRY_CREATE,
              StandardWatchEventKinds.ENTRY_DELETE,
              StandardWatchEventKinds.ENTRY_MODIFY));
        } catch (IOException e) {
          logger.error("Subscribing to all directories has failed withing the stream: ", e);
        }
      });
    } catch (IOException e) {
      logger.error("Subscribing to all directories failed: ", e);
    }
  }
  
  
  public static List<FileInformation> gatherAllFilesUnderRootPath(final Path confDirPath) {
    try (Stream<Path> objectsInConfigurationDirectories = Files.walk(confDirPath)) {
      
      // Obtain all files under root folder
      return objectsInConfigurationDirectories
          .filter(path -> path.toFile().isFile())
          .map(path -> convertPathToFileInformation(path))
          .collect(Collectors.toList());
    } catch (IOException e) {
      logger.error(String.format("The configured path has had an error: %s", confDirPath.toString()), e);
      throw new RuntimeException(e);
    }
  }
}
