package com.sora4222.database;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.*;

class DatabaseChangeSenderTest {
  @Mock
  DatabaseWrapper database;
  DatabaseChangeSender changeSender;
  LinkedList<FileCommand> fileCommands;
  FileInformation testInfo1 = new FileInformation("aFile.txt", "/location/aFile.txt", "Hostname", "A412B43F");
  FileInformation testInfo2 = new FileInformation("aFile2.txt", "/location/aFile2.txt", "Hostname", "A412B43N");
  
  @BeforeEach
  void setupDatabase() {
    MockitoAnnotations.initMocks(this);
    fileCommands = new LinkedList<>();
    changeSender = new DatabaseChangeSender(database);
  }
  
  @Test
  void updateDatabaseIsCalledEmptyNothingIsSentTest() throws TimeoutException {
    when(database.insertFile(any())).thenReturn(true);
    
    changeSender.updateDatabase(new LinkedList<>());
    verify(database, never()).insertFile(any());
  }
  
  @Test
  void whenUpdateIsCalledWithAnInsertCommandTheFileIsSent() throws TimeoutException {
    fileCommands.add(new FileCommand(testInfo1, DatabaseCommand.Insert));
    
    when(database.insertFile(testInfo1)).thenReturn(true);
    changeSender.updateDatabase(fileCommands);
    
    verify(database, times(1)).insertFile(testInfo1);
    verify(database, never()).deleteFileRow(testInfo1);
    verify(database, never()).updateFileRow(testInfo1);
  }
  
  @Test
  void whenUpdateIsCalledWithADeleteCommandTheFileIsDeleted() throws TimeoutException {
    fileCommands.add(new FileCommand(testInfo1, DatabaseCommand.Delete));
    
    when(database.deleteFileRow(testInfo1)).thenReturn(true);
    changeSender.updateDatabase(fileCommands);
    
    verify(database, times(1)).deleteFileRow(testInfo1);
    verify(database, never()).insertFile(testInfo1);
    verify(database, never()).updateFileRow(testInfo1);
  }
  
  @Test
  void whenUpdateIsCalledWithAnUpdateTheRowIsUpdated() throws TimeoutException {
    fileCommands.add(new FileCommand(testInfo1, DatabaseCommand.Update));
    
    when(database.updateFileRow(testInfo1)).thenReturn(true);
    changeSender.updateDatabase(fileCommands);
    
    verify(database, times(1)).updateFileRow(testInfo1);
    verify(database, never()).deleteFileRow(testInfo1);
    verify(database, never()).insertFile(testInfo1);
    
    
  }
  
  @Test
  void whenAnActionIsCalledButDoesntReachTheDatabaseItWillRetry() throws TimeoutException {
    fileCommands.add(new FileCommand(testInfo1, DatabaseCommand.Update));
    when(database.updateFileRow(testInfo1)).thenReturn(false, false, true);
    
    changeSender.updateDatabase(fileCommands);
    
    verify(database, times(3)).updateFileRow(testInfo1);
    verify(database, never()).deleteFileRow(testInfo1);
    verify(database, never()).insertFile(testInfo1);
    
    
  }
  
  @Test
  void whenAnActionIsCalledAndCantConnectThrowAnError() throws TimeoutException {
    fileCommands.add(new FileCommand(testInfo1, DatabaseCommand.Delete));
    when(database.deleteFileRow(testInfo1)).thenReturn(false);
    
    changeSender.updateDatabase(fileCommands);
    
    Assertions.assertThrows(TimeoutException.class, () -> changeSender.updateDatabase(fileCommands));
  }
  
  @Test
  void whenAnActionFailsThroughAListCanResumeWithoutIssue() {
    fileCommands.add(new FileCommand(testInfo1, DatabaseCommand.Insert));
    fileCommands.add(new FileCommand(testInfo2, DatabaseCommand.Update));
    
    when(database.insertFile(testInfo1)).thenReturn(true);
    
    when(database.updateFileRow(testInfo2)).thenReturn(false);
    Assertions.assertThrows(TimeoutException.class, () -> changeSender.updateDatabase(fileCommands));
    verify(database, times(1)).insertFile(testInfo1);
  
    List<FileCommand> commandsLeft = changeSender.getDirectoryChangesLeft();
    Assertions.assertEquals(Collections.singletonList(new FileCommand(testInfo2, DatabaseCommand.Update)), commandsLeft);
    when(database.updateFileRow(testInfo2)).thenReturn(true);
    
    verify(database, times(1)).insertFile(testInfo1);
  }
}
