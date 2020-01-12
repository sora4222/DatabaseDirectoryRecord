package com.sora4222.database.setup.processors;

import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.connectors.DatabaseConnectionInstanceThreaded;
import com.sora4222.database.connectors.Inserter;
import com.sora4222.file.FileInformation;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UploadFileDataRunnable implements Runnable, ProcessorThread {
  private final List<FileInformation> batchHold = new LinkedList<>();
  private final StopWatch elapsedTime = new StopWatch();
  private boolean stopProcessor;
  private static final Logger logger = LogManager.getLogger();
  
  private final DatabaseConnectionInstanceThreaded conn;
  
  public UploadFileDataRunnable() {
    stopProcessor = false;
    conn = new DatabaseConnectionInstanceThreaded();
  }
  
  public synchronized void finishedProcessing() {
    stopProcessor = true;
  }
  
  @Override
  public void run() {
    logger.debug("Configuration batch time: " + ConfigurationManager.getConfiguration().getBatchMaxTimeSeconds());
    logger.debug("Configuration batch size: " + ConfigurationManager.getConfiguration().getBatchMaxSize());
  
    elapsedTime.start();
  
    loopThroughFilesToAdd();
  
    if (batchHold.size() != 0)
      insertFilesIntoDatabase();
  
    elapsedTime.reset();
    logger.info("The setup processor has shutdown.");
  }
  
  private void loopThroughFilesToAdd() {
    while (!stopProcessor || ConcurrentQueues.filesToUpload.size() != 0) {
      loadInFiles();
      logger.debug("Time elapsed: " + elapsedTime.getTime(TimeUnit.SECONDS));
    
      if (batchHold.size() == 0) {
        sleepOneSecond();
        continue;
      }
    
      // Send the files in batches to the database.
      insertFilesIntoDatabase();
    
    }
  }
  
  private void sleepOneSecond() {
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      logger.error("InterruptedException");
    }
  }
  
  private void loadInFiles() {
    Path result;
    while (batchHold.size() < ConfigurationManager.getConfiguration().getBatchMaxSize()
      && elapsedTime.getTime(TimeUnit.SECONDS) < ConfigurationManager.getConfiguration().getBatchMaxTimeSeconds()
      && (result = ConcurrentQueues.filesToUpload.poll()) != null) {
    
      // Loads the file as a FileInformation
      logger.debug("Adding a file to SetupProcessor batch: " + result.toString());
      batchHold.add(FileInformation.fromPath(result));
    }
  }
  
  /**
   * Adds the files into the database.
   */
  private void insertFilesIntoDatabase() {
    logger.info("Sending batch for setup");
    Inserter.insertRecordIntoDatabase(conn.getConnection(), batchHold);
    logger.info("A batch of files for setup has been sent.");
    batchHold.clear();
  }
  
}