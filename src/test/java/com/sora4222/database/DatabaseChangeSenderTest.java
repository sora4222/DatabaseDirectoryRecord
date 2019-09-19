package com.sora4222.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.LinkedList;

import static org.mockito.Mockito.*;

class DatabaseChangeSenderTest {
  @Mock
  DatabaseWrapper database;
  DatabaseChangeSender changeSender;
  LinkedList<FileCommand> fileCommands;
  FileInformation testInfo1 = new FileInformation("aFile.txt", "/location/aFile.txt", "Hostname", "A412B43F");
  FileInformation testInfo2 = new FileInformation("aFile.txt", "/location/aFile.txt", "Hostname", "A412B43F");
  
  @BeforeEach
  void setupDatabase() {
    MockitoAnnotations.initMocks(this);
    fileCommands = new LinkedList<>();
  }
  
  @Test
  void initializeDatabaseChangeSender() {
    changeSender = new DatabaseChangeSender(database);
  }
  
  @Test
  void updateDatabaseIsCalledEmptyNothingIsSentTest() {
    changeSender = new DatabaseChangeSender(database);
    
    when(database.sendFile(any())).thenReturn(true);
    
    changeSender.updateDatabase(new LinkedList<>());
    verify(database, never()).sendFile(any());
  }
  
  @Test
  void whenUpdateIsCalledWithAnInsertCommandTheFileIsSent() {
    changeSender = new DatabaseChangeSender(database);
    
    fileCommands.add(new FileCommand(testInfo1, DatabaseCommand.Insert));
    
    when(database.sendFile(testInfo1)).thenReturn(true);
    changeSender.updateDatabase(fileCommands);
    
    verify(database, times(1)).sendFile(testInfo1);
    verify(database, never()).deleteFileRow(testInfo1);
    verify(database, never()).updateFileRow(testInfo1);
  }
  
  @Test
  void whenUpdateIsCalledWithADeleteCommandTheFileIsDeleted() {
    changeSender = new DatabaseChangeSender(database);
  
    fileCommands.add(new FileCommand(testInfo1, DatabaseCommand.Delete));
  
    when(database.deleteFileRow(testInfo1)).thenReturn(true);
    changeSender.updateDatabase(fileCommands);
  
    verify(database, times(1)).deleteFileRow(testInfo1);
    verify(database, never()).sendFile(testInfo1);
    verify(database, never()).updateFileRow(testInfo1);
  }
  
  @Test
  void whenUpdateIsCalledWithAnUpdateTheRowIsUpdated() {
    changeSender = new DatabaseChangeSender(database);
  
    fileCommands.add(new FileCommand(testInfo1, DatabaseCommand.Update));
  
    when(database.updateFileRow(testInfo1)).thenReturn(true);
    changeSender.updateDatabase(fileCommands);
  
    verify(database, times(1)).updateFileRow(testInfo1);
    verify(database, never()).deleteFileRow(testInfo1);
    verify(database, never()).sendFile(testInfo1);
  }
  
}
