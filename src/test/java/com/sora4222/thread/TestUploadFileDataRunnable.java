package com.sora4222.thread;

import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.connectors.ConnectionStorage;
import com.sora4222.database.connectors.MySqlConnectorTest;
import com.sora4222.database.connectors.UtilityForConnector;
import com.sora4222.database.setup.processors.ConcurrentQueues;
import com.sora4222.database.setup.processors.UploadFileDataRunnable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

public class TestUploadFileDataRunnable {
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
    UploadFileDataRunnable processor = new UploadFileDataRunnable();
    processor.finishedProcessing();
    processor.run();
  
  }
  
  @Test
  public void outputsFilesOverTime() throws InterruptedException, SQLException {
    ConcurrentQueues.filesToUpload.add(Paths.get("src/test/resources/tempConstant.txt"));
    ConfigurationManager.getConfiguration().setBatchMaxTimeSeconds(1);
    ConfigurationManager.getConfiguration().setBatchMaxSize(100);
  
    UploadFileDataRunnable processor = new UploadFileDataRunnable();
    Thread processorThread = new Thread(processor, "UploadFileDataRunnable" + UUID.randomUUID().toString().subSequence(0,4));
    processorThread.start();
  
    Thread.sleep(1500);
    processor.finishedProcessing();
    processorThread.join();
    Assertions.assertFalse(processorThread.isAlive());
    MySqlConnectorTest.assertNumberItemsEqual(1);
  }
  
  @Test
  public void outputsFilesOverBatch() throws InterruptedException, SQLException {
    ConcurrentQueues.filesToUpload.add(Paths.get("src/test/resources/tempConstant.txt"));
    ConcurrentQueues.filesToUpload.add(Paths.get("src/test/resources/root1/sharedFile1.txt"));
    ConfigurationManager.getConfiguration().setBatchMaxTimeSeconds(100);
    ConfigurationManager.getConfiguration().setBatchMaxSize(2);
  
    UploadFileDataRunnable processor = new UploadFileDataRunnable();
    Thread processorThread = new Thread(processor);
    processorThread.start();
  
    processor.finishedProcessing();
    processorThread.join();
    Assertions.assertFalse(processorThread.isAlive());
    MySqlConnectorTest.assertNumberItemsEqual(2);
  
  }
  
  @Test
  public void outputsFilesWillFlush() throws InterruptedException, SQLException {
    ConcurrentQueues.filesToUpload.add(Paths.get("src/test/resources/root1/sharedFile1.txt"));
    ConfigurationManager.getConfiguration().setBatchMaxTimeSeconds(100);
    ConfigurationManager.getConfiguration().setBatchMaxSize(1000);
  
    UploadFileDataRunnable processor = new UploadFileDataRunnable();
    Thread processorThread = new Thread(processor);
    processorThread.start();
  
    processor.finishedProcessing();
    processorThread.join();
    Assertions.assertFalse(processorThread.isAlive());
    MySqlConnectorTest.assertNumberItemsEqual(1);
  
  }
  
}
