package com.sora4222.database.directory;

import com.sora4222.database.DatabaseCommand;
import com.sora4222.database.DatabaseWrapper;
import com.sora4222.database.FileCommand;
import com.sora4222.file.FileInformation;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.LinkedList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@SuppressWarnings("WeakerAccess")
@ExtendWith(MockitoExtension.class)
public class DatabaseChangeLocatorTest {
  
  @Mock
  private DatabaseWrapper database;
  
  private LinkedList<FileInformation> testFiles;
  private LinkedList<FileCommand> expectedFileCommands;
  private DatabaseChangeLocator databaseChangeLocator;
  
  @BeforeEach
  public void createTheFakeDatabase() {
    MockitoAnnotations.initMocks(DatabaseChangeLocator.class);
    
    databaseChangeLocator = new DatabaseChangeLocator(database);
    
    testFiles = new LinkedList<>();
    expectedFileCommands = new LinkedList<>();
  }
  
  @Test
  public void notSetChangeLocatorReturnsEmpty() {
    Assertions.assertTrue(databaseChangeLocator.findChangesToDirectory().isEmpty());
  }
  
  @Test
  public void setChangeLocatorListTwoItemAndOneNotStoredInDatabaseReturnsTheFileCommand() {
    FileInformation notInDatabase = new FileInformation("/a/location/aFile.txt", "123AF");
    FileInformation inDatabase = new FileInformation("/a/location/anExistingFile.txt", "1B3C8");
  
    testFiles.add(notInDatabase);
    testFiles.add(inDatabase);
  
    expectedFileCommands.add(new FileCommand(notInDatabase, DatabaseCommand.Insert));
  
    when(database.checkForFile(notInDatabase)).thenReturn(new LinkedList<>());
    when(database.checkForFile(inDatabase)).thenReturn(Collections.singletonList(inDatabase));
    
    
    databaseChangeLocator.setFilesInDirectories(testFiles);
    Assertions.assertEquals(expectedFileCommands, databaseChangeLocator.findChangesToDirectory());
  }
  
  @Test
  public void aFileHasBeenMovedToADifferentLocationNoChanges() {
    FileInformation originalFile = new FileInformation("/a/location/aFile.txt", "123AF");
    FileInformation newLocation = new FileInformation("/a/new/aFile.txt", "123AF");
  
    when(database.checkForFile(newLocation)).thenReturn(Collections.singletonList(originalFile));
  
    testFiles.add(newLocation);
    databaseChangeLocator.setFilesInDirectories(testFiles);
  
    assertThat(databaseChangeLocator.findChangesToDirectory(),
        Matchers.containsInAnyOrder(new FileCommand(originalFile, DatabaseCommand.Delete), new FileCommand(newLocation, DatabaseCommand.Insert)));
  }
  
  @Test
  public void aFileHasHadItsContentsChanged() {
    FileInformation originalFile = new FileInformation("/a/location/aFile.txt", "123AF");
    FileInformation newContentsFile = new FileInformation("/a/location/aFile.txt", "546AF");
  
    when(database.checkForFile(newContentsFile)).thenReturn(Collections.singletonList(originalFile));
  
    testFiles.add(newContentsFile);
    databaseChangeLocator.setFilesInDirectories(testFiles);
  
    expectedFileCommands.add(new FileCommand(newContentsFile, DatabaseCommand.Update));
  
    Assertions.assertEquals(expectedFileCommands, databaseChangeLocator.findChangesToDirectory());
  }
}
