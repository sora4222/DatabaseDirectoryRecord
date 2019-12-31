package com.sora4222.thread;

import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.connectors.ConnectionStorage;
import com.sora4222.database.connectors.MySqlConnectorTest;
import com.sora4222.database.connectors.UtilityForConnector;
import com.sora4222.database.thread.SetupProcessor;
import com.sora4222.database.thread.Tools;
import com.sora4222.file.FileInformation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

public class TestSetupProcessor {
  @BeforeEach
  public void setup() throws SQLException {
    Connection connector = UtilityForConnector.getOrInitializeConnection();
    connector.prepareStatement("DELETE FROM `directory_records`").executeUpdate();
  }
  
  @AfterEach
  public void teardownConnector() {
    ConnectionStorage.close();
  }
  
  
  @Test
  public void willRunAndStop() {
    SetupProcessor processor = new SetupProcessor();
    processor.stopProcessor();
    processor.run();
    
    //TODO: Check nothing was input
  }
  
  @Test
  public void outputsFilesOverTime() throws InterruptedException, SQLException {
    FileInformation fakeFile = new FileInformation("aFile.txt", "AFAKEHASH");
    Tools.hardDriveSetupQueue.add(fakeFile);
    ConfigurationManager.getConfiguration().setBatchMaxTimeSeconds(1);
    ConfigurationManager.getConfiguration().setBatchMaxSize(100);
  
    SetupProcessor processor = new SetupProcessor();
    Thread processorThread = new Thread(processor);
    processorThread.start();
    
    Thread.sleep(1500);
    processor.stopProcessor();
    processorThread.join(5000);
    Assertions.assertFalse(processorThread.isAlive());
    MySqlConnectorTest.assertNumberItemsEqual(1);
  }
  
  @Test
  public void outputsFilesOverBatch() throws InterruptedException, SQLException {
    Tools.hardDriveSetupQueue.add(new FileInformation("aFile.txt", "AFAKEHASH"));
    Tools.hardDriveSetupQueue.add(new FileInformation("aFile2.txt", "AFAKEHASH2"));
    ConfigurationManager.getConfiguration().setBatchMaxTimeSeconds(100);
    ConfigurationManager.getConfiguration().setBatchMaxSize(2);
  
    SetupProcessor processor = new SetupProcessor();
    Thread processorThread = new Thread(processor);
    processorThread.start();
    
    processor.stopProcessor();
    processorThread.join(5000);
    Assertions.assertFalse(processorThread.isAlive());
    MySqlConnectorTest.assertNumberItemsEqual(2);
  
  }
  
  @Test
  public void outputsFilesWillFlush() throws InterruptedException, SQLException {
    FileInformation fakeFile = new FileInformation("aFile.txt", "AFAKEHASH");
    Tools.hardDriveSetupQueue.add(fakeFile);
    ConfigurationManager.getConfiguration().setBatchMaxTimeSeconds(100);
    ConfigurationManager.getConfiguration().setBatchMaxSize(1000);
    
    SetupProcessor processor = new SetupProcessor();
    Thread processorThread = new Thread(processor);
    processorThread.start();
    
    processor.stopProcessor();
    processorThread.join(5000);
    Assertions.assertFalse(processorThread.isAlive());
    MySqlConnectorTest.assertNumberItemsEqual(1);
  
  }
  
}
