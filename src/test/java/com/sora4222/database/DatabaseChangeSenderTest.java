package com.sora4222.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.LinkedList;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class DatabaseChangeSenderTest {
  @Mock
  DatabaseWrapper database;
  DatabaseChangeSender changeSender;
  
  
  @BeforeEach
  void setupDatabase() {
    MockitoAnnotations.initMocks(this);
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
  
  }
  @Test
  void whenUpdateIsCalledWithADeleteCommandTheFileIsDeleted() {
  
  }
  
  @Test
  void whenUpdateIsCalledWithAnUpdateTheRowIsUpdated() {
  
  }
}
