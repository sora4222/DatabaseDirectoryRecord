package com.sora4222.database;

import com.sora4222.database.connectors.MySqlConnector;
import com.sora4222.database.directory.DatabaseChangeLocator;
import com.sora4222.database.directory.Scanner;
import com.sora4222.file.FileInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class DirectoryRecorder {
  private static DatabaseWrapper database;
  private static Scanner scanner;
  private static DatabaseChangeLocator databaseChangeLocator;
  private static DatabaseChangeSender changeSender;
  private static Logger logger = LogManager.getLogger();
  
  public static void main(String[] args) {
    setupScanning();
    startScanning();
  }
  
  private static void setupScanning() {
    loadDatabase();
    scanner = new Scanner();
    databaseChangeLocator = new DatabaseChangeLocator(database);
    changeSender = new DatabaseChangeSender(database);
  }
  
  private static void loadDatabase() {
    database = new MySqlConnector();
  }
  
  @SuppressWarnings("InfiniteLoopStatement")
  private static void startScanning() {
    while (true) {
      List<FileInformation> filesInDirectories = scanner.scanAllDirectories();
      databaseChangeLocator.setFilesInDirectories(filesInDirectories);
      List<FileCommand> directoryChanges = databaseChangeLocator.findChangesToDirectory();
  
      updateDatabaseAndRetry(directoryChanges);
    }
  }
  
  private static void updateDatabaseAndRetry(final List<FileCommand> directoryChanges){
    List<FileCommand> directoryChangesRemaining = new ArrayList<>(directoryChanges);
    while (true) {
      try {
        changeSender.updateDatabase(directoryChangesRemaining);
        break;
      } catch (TimeoutException e) {
        directoryChangesRemaining = changeSender.getDirectoryChangesLeft();
        //Keep trying
        waitToRetry();
      }
    }
  }
  
  private static void waitToRetry(){
    try {
      Thread.currentThread().wait(60000);
    } catch (InterruptedException ex) {
      logger.error("There was an interruption whilst waiting to perform another database contact.");
    }
  }
}
