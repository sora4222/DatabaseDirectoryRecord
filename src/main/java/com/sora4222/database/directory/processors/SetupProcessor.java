package com.sora4222.database.directory.processors;

import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.connectors.DatabaseQuery;
import com.sora4222.database.connectors.Inserter;
import com.sora4222.file.FileInformation;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SetupProcessor implements Runnable {
  private final List<FileInformation> batchHold = new LinkedList<>();
  private static final StopWatch elapsedTime = new StopWatch();
  private boolean stopProcessor;
  private static final Logger logger = LogManager.getLogger();
  
  public SetupProcessor() {
    stopProcessor = false;
  }
  
  public synchronized void stopProcessor() {
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
    do {
      loadInFiles();
  
      logger.debug("Time elapsed: " + elapsedTime.getTime(TimeUnit.SECONDS));
      if (batchHold.size() >= ConfigurationManager.getConfiguration().getBatchMaxSize()
          || elapsedTime.getTime(TimeUnit.SECONDS) >= ConfigurationManager.getConfiguration().getBatchMaxTimeSeconds()) {
        restartTimer();
        if (batchHold.size() == 0)
          continue;
    
        // Send the ones that are not in the database via updates or insert probably with another thread
        insertFilesIntoDatabase();
      }
      sleepOneSecond();
  
    } while (!stopProcessor);
  }
  
  private void sleepOneSecond() {
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      logger.error("InterruptedException");
    }
  }
  
  private void loadInFiles() {
    FileInformation result;
    while (batchHold.size() < ConfigurationManager.getConfiguration().getBatchMaxSize()
        && elapsedTime.getTime(TimeUnit.SECONDS) < ConfigurationManager.getConfiguration().getBatchMaxTimeSeconds()
        && (result = ConcurrentQueues.hardDriveSetupQueue.poll()) != null) {
      logger.debug("Adding a file to SetupProcessor batch.");
      batchHold.add(result);
    }
  }
  
  private void restartTimer() {
    elapsedTime.reset();
    elapsedTime.start();
  }
  
  private void insertFilesIntoDatabase() {
    logger.info("Sending batch for setup");
    Inserter.insertRecordIntoDatabase(DatabaseQuery.queryTheDatabaseForFiles(batchHold));
    logger.info("A batch of files for setup has been sent.");
    batchHold.clear();
  }
  
}