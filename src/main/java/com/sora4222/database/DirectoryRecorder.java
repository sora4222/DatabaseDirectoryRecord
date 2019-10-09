package com.sora4222.database;

import com.sora4222.database.configuration.ComputerProperties;
import com.sora4222.database.configuration.Config;
import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.connectors.Deleter;
import com.sora4222.database.connectors.Inserter;
import com.sora4222.database.connectors.Updater;
import com.sora4222.database.connectors.DatabaseQuery;
import com.sora4222.file.FileHasher;
import com.sora4222.file.FileInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
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
  
  public static void main (String[] args) {
    setupScanning();
    startScanning();
  }
  
  @SuppressWarnings("InfiniteLoopStatement")
  static void startScanning () {
    while (true) {
      for(Map.Entry<Path, WatchKey> watchKey : directoriesWatching.entrySet()){
        watchKey.getValue().pollEvents().parallelStream().map(watchEvent -> watchEventToFileCommand(watchEvent, watchKey.getKey())).collect(Collectors.toList());
        
        watchKey.getValue().reset();
      }
    }
  }
  
  private static FileCommand watchEventToFileCommand (final WatchEvent<?> watchEvent, final Path pathToDirectory) {
    
    FileInformation file = convertPathToFileInformation(pathToDirectory);
    DatabaseCommand commandForChange = eventToCommandMap.get(watchEvent.kind());
    
    return new FileCommand(file, commandForChange);
  }
  
  @SuppressWarnings("InfiniteLoopStatement")
  static void setupScanning () {
    
    //Subscribe to directories for the first time
    for (Path confDirPath : config.getRootLocationsAsPaths()) {
      subscribeToChangesFromAllDirectories(confDirPath);
      
      //Seed all directories
      List<FileInformation> allFiles = gatherAllFilesUnderRootPath(confDirPath);
      List<FileInformation> filesNotInTheDatabase =
        DatabaseEntries
          .getComputersFilesFromDatabase()
          .limit(DatabaseEntries.databaseRecordCount())
          .parallel()
          .filter(fileInformation -> !allFiles.contains(fileInformation))
          .collect(Collectors.toList());
      
      // Upload those files.
      Inserter.insertFilesIntoDatabase(filesNotInTheDatabase);
      
      if (Boolean.getBoolean(System.getProperty("skipUpdateAndDelete", "False"))) {
        List<FileCommand> updates = findUpdatesToDatabase(confDirPath);
        Updater.sendUpdatesToDatabase(updates.parallelStream().filter(command -> command.getCommand().equals(DatabaseCommand.Update)).map(fileCommand -> fileCommand.getInformation()).collect(Collectors.toList()));
        Deleter.sendDeletesToDatabase(updates.parallelStream().filter(command -> command.getCommand().equals(DatabaseCommand.Delete)).map(fileCommand -> fileCommand.getInformation()).collect(Collectors.toList()));
        Inserter.insertFilesIntoDatabase(updates.parallelStream().filter(command -> command.getCommand().equals(DatabaseCommand.Insert)).map(fileCommand -> fileCommand.getInformation()).collect(Collectors.toList()));
      }
    }
  }
  
  private static List<FileCommand> findUpdatesToDatabase (final Path confDirPath) {
    try (Stream<Path> objectsInConfigurationDirectories = Files.walk(confDirPath)) {
      final List<FileInformation> existingFiles = objectsInConfigurationDirectories
        .parallel()
        .filter(path -> path.toFile().isFile())
        .map(path -> convertPathToFileInformation(path))
        .collect(Collectors.toList());
      
      final List<FileCommand> updates = DatabaseQuery
        .allFilesInBothComputerAndDatabase(existingFiles)
        .parallelStream()
        .filter(fileInformation -> !existingFiles.contains(fileInformation))
        .map(path -> getUpdateType(existingFiles, path))
        .collect(Collectors.toList());
      
      return updates;
    } catch (IOException e) {
      logger.error("Potentially a file walking error: ", e);
    }
    return new ArrayList<>();
  }
  
  private static FileInformation convertPathToFileInformation (final Path path) {
    FileHasher hasher = new FileHasher(path.toFile());
    return new FileInformation(path, ComputerProperties.computerName.get(), hasher.hashFile());
  }
  
  private static FileCommand getUpdateType (final List<FileInformation> existingFiles, final FileInformation fileOfInterest) {
    if (existingFiles.contains(fileOfInterest))
      return new FileCommand(
        fileOfInterest,
        DatabaseCommand.Update);
    else
      return new FileCommand(
        fileOfInterest,
        DatabaseCommand.Delete);
  }
  
  private static void subscribeToChangesFromAllDirectories (final Path directory) {
    try (Stream<Path> objectsInConfigurationDirectories = Files.walk(directory)) {
      objectsInConfigurationDirectories.parallel().filter(path -> path.toFile().isDirectory()).forEach(path -> {
        try {
          directoriesWatching.put(path, path.register(subscribeService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY));
        } catch (IOException e) {
          logger.error("Subscribing to all directories has failed withing the stream: ",e);
        }
      });
    } catch (IOException e) {
      logger.error("Subscribing to all directories failed: ",e);
    }
  }
  
  
  private static List<FileInformation> gatherAllFilesUnderRootPath (final Path confDirPath) {
    try (Stream<Path> objectsInConfigurationDirectories = Files.walk(confDirPath)) {
      // Obtain all files in database
      return objectsInConfigurationDirectories
        .map(path -> convertPathToFileInformation(path))
        .collect(Collectors.toList());
    } catch (IOException e) {
      logger.error(String.format("The configured path has had an error: %s", confDirPath.toString()), e);
      throw new RuntimeException(e);
    }
  }
}
