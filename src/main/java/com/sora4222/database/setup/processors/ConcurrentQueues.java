package com.sora4222.database.setup.processors;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentQueues {
  /**
   * Used to take what files need to be uploaded
   */
  public static final ConcurrentLinkedQueue<Path> filesToUpload
    = new ConcurrentLinkedQueue<>();
  
  /**
   * Used with a thread to query whether a file exists in the database.
   * filesToQuery -> filesToUpload
   */
  @SuppressWarnings("WeakerAccess")
  public static final ConcurrentLinkedQueue<Path> filesToQuery = new ConcurrentLinkedQueue<>();
  
  /**
   * Used to list to the database a visited directory.
   */
  public static ConcurrentLinkedQueue<Path> visitedDirectoriesQueue = new ConcurrentLinkedQueue<>();
  
}
