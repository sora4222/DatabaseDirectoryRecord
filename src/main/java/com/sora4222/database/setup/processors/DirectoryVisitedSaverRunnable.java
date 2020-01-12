package com.sora4222.database.setup.processors;

import com.sora4222.database.connectors.DatabaseConnectionInstanceThreaded;
import com.sora4222.database.connectors.Inserter;
import org.apache.commons.lang3.time.StopWatch;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class DirectoryVisitedSaverRunnable implements Runnable, ProcessorThread {
  private static final int MAX_BATCH_SIZE = 10000;
  private static final long MAX_WAIT_TIME = 120;
  private final StopWatch timer = new StopWatch();
  private final LinkedList<Path> directoriesBatch = new LinkedList<>();
  private boolean finishedVisitingDirectories;
  private DatabaseConnectionInstanceThreaded conn;
  
  public DirectoryVisitedSaverRunnable() {
    this.finishedVisitingDirectories = false;
  
    conn = new DatabaseConnectionInstanceThreaded();
  }
  
  @Override
  public void run() {
    startTimer();
    while (!finishedVisitingDirectories || ConcurrentQueues.visitedDirectoriesQueue.size() != 0) {
      fillDirectoryBatch();
      
      if (directoriesBatch.size() != 0) {
        storeDirectories();
      }
      emptyBatch();
      timer.reset();
    }
  }
  
  private void emptyBatch() {
    directoriesBatch.clear();
  }
  
  private void fillDirectoryBatch() {
    int i = 0;
    Path directoryToAdd;
    while (i++ < MAX_BATCH_SIZE &&
      timer.getTime(TimeUnit.SECONDS) <= MAX_WAIT_TIME &&
      (directoryToAdd = ConcurrentQueues.visitedDirectoriesQueue.poll()) != null) {
      directoriesBatch.add(directoryToAdd);
    }
  }
  
  private void startTimer() {
    timer.start();
  }
  
  private void storeDirectories() {
    Inserter.insertDirectoriesToDirectoryTable(directoriesBatch, conn.getConnection());
  }
  
  @Override
  public void finishedProcessing() {
    this.finishedVisitingDirectories = true;
  }
}

