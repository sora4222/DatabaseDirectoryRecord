package com.sora4222.database;

import com.sora4222.database.directory.ChangeLocator;
import com.sora4222.database.directory.Scanner;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

public class DirectoryRecorder {
  private static DatabaseWrapper database;
  private static Scanner scanner;
  private static ChangeLocator changeLocator;
  private static DatabaseChangeSender changeSender;
  
  public static void main(String[] args) {
    setupScanning();
    startScanning();
  }
  
  private static void setupScanning() {
    database = loadDatabase();
    scanner = new Scanner();
    changeLocator = new ChangeLocator(database);
    changeSender = new DatabaseChangeSender(database);
  }
  
  private static DatabaseWrapper loadDatabase() {
    throw new NotImplementedException("Not currently implemented");
  }
  
  @SuppressWarnings("InfiniteLoopStatement")
  private static void startScanning() {
    while (true) {
      List<FileInformation> filesInDirectories = scanner.scanAllDirectories();
      List<FileInformation> directoryChanges = changeLocator.findChangesToDirectory(filesInDirectories);
      changeSender.sendAllFiles(directoryChanges);
    }
  }
}
