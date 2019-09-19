package com.sora4222.database;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.Function;

public class DatabaseChangeSender {
  private final DatabaseWrapper database;
  private static final Logger logger = LogManager.getLogger();
  
  public DatabaseChangeSender(final DatabaseWrapper database) {
    this.database = database;
  }
  
  public void updateDatabase(final List<FileCommand> directoryChanges) {
    for (FileCommand currentFileCommand : directoryChanges) {
      switch (currentFileCommand.command) {
        case Insert:
          Function<FileInformation, Boolean> sendFileAction = information -> database.sendFile(information);
          tryToPerformDatabaseAction(sendFileAction, currentFileCommand);
          break;
        case Delete:
          Function<FileInformation, Boolean> deleteFileAction = information -> database.deleteFileRow(information);
          tryToPerformDatabaseAction(deleteFileAction, currentFileCommand);
          break;
        case Update:
          Function<FileInformation, Boolean> updateFileAction = information -> database.updateFileRow(information);
          tryToPerformDatabaseAction(updateFileAction, currentFileCommand);
          break;
      }
    }
  }
  
  private void tryToPerformDatabaseAction(
      final Function<FileInformation, Boolean> action,
      final FileCommand currentFileCommand) {
    int i = 0;
    while (!action.apply(currentFileCommand.information)) {
      if (i > 10) {
        String errorMessage =
            String.format("Retries have been exhausted updating file information: %s", currentFileCommand.toString());
        logger.error(errorMessage);
        throw new RuntimeException(errorMessage);
      }
      try {
        Thread.sleep(i++ * 100);
      } catch (InterruptedException e) {
        logger.error("An interruption error has occurred whilst attempting to update a file.", e);
        throw new RuntimeException(e);
      }
    }
  }
}
