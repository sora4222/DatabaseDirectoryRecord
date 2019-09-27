package com.sora4222.database;


import com.sora4222.file.FileInformation;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class DatabaseChangeSender {
  private final DatabaseWrapper database;
  private static final Logger logger = LogManager.getLogger();
  
  @Getter
  private List<FileCommand> directoryChangesLeft = new LinkedList<>();
  
  public DatabaseChangeSender(final DatabaseWrapper database) {
    this.database = database;
  }
  
  public void updateDatabase(final List<FileCommand> directoryChanges) throws TimeoutException {
    directoryChangesLeft = new ArrayList<>(directoryChanges);
    
    for (FileCommand currentFileCommand : directoryChanges) {
      switch (currentFileCommand.command) {
        case Insert:
          Function<FileInformation, Boolean> sendFileAction = information -> database.insertFile(information);
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
      directoryChangesLeft.remove(currentFileCommand);
    }
  }
  
  private void tryToPerformDatabaseAction(
      final Function<FileInformation, Boolean> action,
      final FileCommand currentFileCommand) throws TimeoutException {
    int i = 0;
    while (!action.apply(currentFileCommand.information)) {
      if (i > 10) {
        String errorMessage =
            String.format("Retries have been exhausted updating file information: %s", currentFileCommand.toString());
        logger.error(errorMessage);
        throw new TimeoutException(errorMessage);
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
