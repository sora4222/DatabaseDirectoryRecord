package com.sora4222.database.directory.processors;

import com.sora4222.database.connectors.Inserter;
import org.apache.commons.lang3.time.StopWatch;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import static com.sora4222.database.directory.processors.ConcurrentQueues.visitedDirectoriesQueue;

public class DirectoryVisitedSaver implements Runnable {
  private static final int MAX_BATCH_SIZE = 10000;
  private static final long MAX_WAIT_TIME = 120;
  private static final StopWatch timer = new StopWatch();
  public boolean finishedVisitingDirectories;
  
  public DirectoryVisitedSaver() {
    this.finishedVisitingDirectories = false;
  }
  
  public void isFinishedVisitingDirectories() {
    this.finishedVisitingDirectories = true;
  }
  
  @Override
  public void run() {
    startTimer();
    LinkedList<Path> directoriesBatch = new LinkedList<>();
    do {
      fillDirectoryBatch(directoriesBatch);
      storeDirectories(directoriesBatch);
    } while (!finishedVisitingDirectories || visitedDirectoriesQueue.size() != 0);
  }
  
  private void fillDirectoryBatch(LinkedList<Path> directoriesBatch) {
    for (int i = 0; i < MAX_BATCH_SIZE || timer.getTime(TimeUnit.SECONDS) >= MAX_WAIT_TIME; i++) {
      directoriesBatch.add(directoriesBatch.poll());
    }
  }
  
  private void startTimer() {
    timer.start();
  }
  
  private void storeDirectories(LinkedList<Path> directoriesBatch) {
    Inserter.insertDirectoriesToDirectoryTable(directoriesBatch);
  }
}

