package com.sora4222.database.directory;

import com.sora4222.database.DirectoryRecorder;
import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.directory.processors.ConcurrentQueues;
import com.sora4222.database.directory.processors.SetupProcessor;
import com.sora4222.file.FileInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SetupDirectoryScan {
  private static final Logger logger = LogManager.getLogger();
  
  static final FileVisitor<Path> visitor = new FileVisitor<Path>() {
    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
      if (!Files.isReadable(path)
        && !Files.isExecutable(path)
        && DirectoryRecorder.filterPathsBasedOnRegexExcludes(path)) {
        return FileVisitResult.SKIP_SUBTREE;
      }
      
      subscribeToDirectory(path);
      return FileVisitResult.CONTINUE;
    }
    
    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
      if (Files.isReadable(path) && DirectoryRecorder.filterPathsBasedOnRegexExcludes(path)) {
        FileInformation file = FileInformation.fromPath(path);
        ConcurrentQueues.hardDriveSetupQueue.add(file);
      }
      
      return FileVisitResult.CONTINUE;
    }
    
    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
      if (e != null) {
        logger.info("File visit failed [" + path.toString() + "]", e);
      }
      return FileVisitResult.CONTINUE;
    }
  
    @Override
    public FileVisitResult postVisitDirectory(Path directoryPath, IOException e) {
      if (e != null) {
        logger.error(e);
      }
      ConcurrentQueues.visitedDirectoriesQueue.add(directoryPath);
      return FileVisitResult.CONTINUE;
    }
  };
  
  public static void walkThroughFolders() {
    // Start the thread for processing
    SetupProcessor processor = new SetupProcessor();
    Thread processorThread = new Thread(processor);
    processorThread.start();
    //Subscribe to directories for the first time and seed
    for (Path confDirPath : ConfigurationManager.getConfiguration().getRootLocationsAsPaths()) {
      try {
        Files.walkFileTree(confDirPath,
          Collections.singleton(FileVisitOption.FOLLOW_LINKS),
          ConfigurationManager.getConfiguration().getDepthOfTree(),
          visitor);
          
      } catch (IOException e) {
        logger.error("An IOException occurred whilst scanning the file tree.", e);
      }
    }
  
    while (ConcurrentQueues.hardDriveSetupQueue.size() != 0) {
      try {
        TimeUnit.SECONDS.sleep(3);
      } catch (InterruptedException e) {
        logger.error(e);
      }
    }
  
    stopProcessorThread(processor, processorThread);
  }
  
  private static void stopProcessorThread(SetupProcessor processor, Thread processorThread) {
    processor.stopProcessor();
    try {
      processorThread.join(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
  
  private static void subscribeToDirectory(Path directory) {
    try {
      DirectoryRecorder.directoriesWatching.put(directory,
        directory.register(DirectoryRecorder.subscribeService,
          StandardWatchEventKinds.ENTRY_CREATE,
          StandardWatchEventKinds.ENTRY_DELETE,
          StandardWatchEventKinds.ENTRY_MODIFY));
    } catch (IOException e) {
      logger.error("Subscribing to a directory has failed.", e);
      throw new RuntimeException(e);
    }
  }
}
