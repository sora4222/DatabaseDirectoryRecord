package com.sora4222.database;

import com.sora4222.database.directory.DatabaseChangeLocator;
import com.sora4222.database.directory.Scanner;

import java.util.List;

public class DirectoryRecorder {
  private static DatabaseWrapper database;
  private static Scanner scanner;
  private static DatabaseChangeLocator databaseChangeLocator;
  private static DatabaseChangeSender changeSender;
  
  public static void main(String[] args) {
    setupScanning();
    startScanning();
  }
  
  private static void setupScanning() {
    database = loadDatabase();
    scanner = new Scanner();
    databaseChangeLocator = new DatabaseChangeLocator(database);
    changeSender = new DatabaseChangeSender(database);
  }
  
  private static DatabaseWrapper loadDatabase() {
    throw new UnsupportedOperationException("Not currently implemented");
  }
  
  @SuppressWarnings("InfiniteLoopStatement")
  private static void startScanning() {
    while (true) {
      List<FileInformation> filesInDirectories = scanner.scanAllDirectories();
      databaseChangeLocator.setFilesInDirectories(filesInDirectories);
      List<FileCommand> directoryChanges = databaseChangeLocator.findChangesToDirectory();
      changeSender.updateDatabase(directoryChanges);
    }
  }
}
