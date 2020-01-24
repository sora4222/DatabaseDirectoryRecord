package com.sora4222.database.setup.processors;

import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.connectors.DatabaseQuery;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ExistingFileFilterRunnable implements ProcessorThread, Runnable {
  private static final Logger logger = LogManager.getLogger();
  private final List<Path> batchHold = new LinkedList<>();
  private final StopWatch elapsedTime = new StopWatch();
  private boolean stopProcessor;
  private final UploadFileDataRunnable uploadFileDataRunnable = new UploadFileDataRunnable();
  private final Thread uploadFileThread;
  
  /**
   * Creates a {@link Runnable} that will query files in the queue filesToFilter
   * within {@link ConcurrentQueues}. All files that do not exist are passed to the
   * queue filesToUpload.
   */
  public ExistingFileFilterRunnable() {
    stopProcessor = false;
    uploadFileThread = new Thread(uploadFileDataRunnable,
      "UploadFileDataThread" + UUID.randomUUID().toString().subSequence(0,4));
  }
  
  @Override
  public void finishedProcessing() {
    stopProcessor = true;
  }
  
  @Override
  public void run() {
    uploadFileThread.start();
    while (!stopProcessor) {
      logger.debug("Configuration batch time: " + ConfigurationManager.getConfiguration().getBatchMaxTimeSeconds());
      logger.debug("Configuration batch size: " + ConfigurationManager.getConfiguration().getBatchMaxSize());
    
    
      elapsedTime.start();
    
      loopThroughFilesToAdd();
    
      if (batchHold.size() != 0)
        ConcurrentQueues.filesToUpload.addAll(DatabaseQuery.checkFilePathsExists(batchHold));
    
      elapsedTime.reset();
      logger.info("The setup processor has shutdown.");
    }
    uploadFileDataRunnable.finishedProcessing();
    try {
      uploadFileThread.join();
    } catch (InterruptedException e) {
      logger.error("Exception occurred waiting for the UploadFileDataRunnable thread to stop." + e.getMessage());
    }
  }
  
  private void loopThroughFilesToAdd() {
    do {
      loadInFiles();
      
      logger.debug("Time elapsed: " + elapsedTime.getTime(TimeUnit.SECONDS));
      elapsedTime.reset();
      
      if (batchHold.size() == 0) {
        sleepOneSecond();
        continue;
      }
      
      ConcurrentQueues.filesToUpload.addAll(DatabaseQuery.checkFilePathsExists(batchHold));
      
    } while (!stopProcessor || ConcurrentQueues.filesToQuery.size() != 0);
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
      && (result = ConcurrentQueues.filesToQuery.poll()) != null) {
      logger.debug("Adding a file to SetupProcessor batch.");
      batchHold.add(result);
    }
  }
  
}
