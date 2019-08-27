package com.sora4222.database;


import java.util.List;

public class DatabaseChangeSender {
  private final DatabaseWrapper database;
  
  public DatabaseChangeSender (final DatabaseWrapper database) {
    this.database = database;
  }
  
  public void updateDatabase(final List<FileCommand> directoryChanges) {
    for(FileCommand currentFileCommand: directoryChanges) {
      switch (currentFileCommand.command) {
        case Insert:
          database.sendFile(currentFileCommand.information);
          break;
        case Delete:
          database.deleteFileRow(currentFileCommand.information);
          break;
        case Update:
          database.updateFileRow(currentFileCommand.information);
          break;
      }
    }
  }
}
