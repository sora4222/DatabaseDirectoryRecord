package com.sora4222.database.setup;

import com.sora4222.database.DatabaseCommand;
import com.sora4222.database.DatabaseWrapper;
import com.sora4222.database.FileCommand;
import com.sora4222.file.FileInformation;
import lombok.Setter;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseChangeLocator {
  private final DatabaseWrapper database;
  
  @Setter
  List<FileInformation> filesInDirectories;
  
  public DatabaseChangeLocator(final DatabaseWrapper database) {
    this.database = database;
    this.filesInDirectories = new LinkedList<>();
  }
  
  public List<FileCommand> findChangesToDirectory() {
    LinkedList<FileCommand> commandsToDatabase = new LinkedList<>();
    
    for (FileInformation fileInADirectory : filesInDirectories) {
      commandsToDatabase.addAll(getFileCommands(fileInADirectory));
    }
    return commandsToDatabase;
  }
  
  private LinkedList<FileCommand> getFileCommands(final FileInformation fileInADirectory) {
    List<FileInformation> receivedFileList = database.checkForFile(fileInADirectory);
    LinkedList<FileCommand> commandsToAdd = new LinkedList<>();
    
    // Deal with the event that it is not in there in any way.
    if (receivedFileList.isEmpty()) {
      commandsToAdd.add(new FileCommand(fileInADirectory, DatabaseCommand.Insert));
      return commandsToAdd;
    }
    
    // Exact file is already in the database
    if (receivedFileList.contains(fileInADirectory)) {
      return commandsToAdd;
    }
    
    if (isFileInDirectoryChangedFile(fileInADirectory, receivedFileList)) {
      commandsToAdd.add(new FileCommand(fileInADirectory, DatabaseCommand.Update));
      return commandsToAdd;
    }
    
    // The file has been moved so the hash is exactly the same the hostname is the same, the name can be different.
    // This satisfies file movements too.
    commandsToAdd.add(new FileCommand(fileInADirectory, DatabaseCommand.Insert));
    commandsToAdd.addAll(processDatabaseReturnedFiles(fileInADirectory, receivedFileList));
    
    return commandsToAdd;
  }
  
  private boolean isFileInDirectoryChangedFile(final FileInformation fileInADirectory,
                                               final List<FileInformation> receivedFileList) {
    return receivedFileList.parallelStream()
        .anyMatch(fileFromDatabase ->
                fileInADirectory.getFullLocation().equals(fileFromDatabase.getFullLocation()) &&
                !fileInADirectory.getFileHash().equals(fileFromDatabase.getFileHash()));
  }
  
  private LinkedList<FileCommand> processDatabaseReturnedFiles(final FileInformation fileInADirectory,
                                                               final List<FileInformation> receivedFileList) {
    List<FileInformation> movedFiles = receivedFileList.parallelStream()
        .filter(fileInformation -> fileInformation.getFileHash().equals(fileInADirectory.getFileHash()))
        .collect(Collectors.toList());
    
    LinkedList<FileCommand> commandsToAdd = new LinkedList<>();
    // Check if any of these files still exist
    for (FileInformation potentiallyExisting : movedFiles) {
      File potentiallyExistingFile = new File(potentiallyExisting.getFullLocation().toUri());
      if (!potentiallyExistingFile.exists())
        commandsToAdd.add(new FileCommand(potentiallyExisting, DatabaseCommand.Delete));
    }
    return commandsToAdd;
  }
}
