package com.sora4222.database.directory.processors;

import com.sora4222.file.FileInformation;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentQueues {
  public static final ConcurrentLinkedQueue<FileInformation> hardDriveSetupQueue
      = new ConcurrentLinkedQueue<>();
  public static ConcurrentLinkedQueue<Path> visitedDirectoriesQueue = new ConcurrentLinkedQueue<>();
  
}
