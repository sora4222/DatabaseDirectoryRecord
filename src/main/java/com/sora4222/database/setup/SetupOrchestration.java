package com.sora4222.database.setup;

import com.sora4222.database.DirectoryRecorder;
import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.setup.processors.ConcurrentQueues;
import com.sora4222.database.setup.processors.DirectoryVisitedSaverRunnable;
import com.sora4222.database.setup.processors.ExistingFileFilterRunnable;
import com.sora4222.database.setup.processors.ProcessorThread;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.UUID;

import static com.sora4222.database.setup.ScanningTools.subscribeToDirectory;

public class SetupOrchestration {
  private static final Logger logger = LogManager.getLogger();
  
  static public final FileVisitor<Path> visitor = new FileVisitor<Path>() {
    
    @Override
    public FileVisitResult preVisitDirectory(Path directoryPath, BasicFileAttributes basicFileAttributes) {
      if (!Files.isReadable(directoryPath)
        && !Files.isExecutable(directoryPath)
        && DirectoryRecorder.filterPathsBasedOnRegexExcludes(directoryPath)) {
        return FileVisitResult.SKIP_SUBTREE;
      }
      
      //To do check other directories in the directory whether they
      
      subscribeToDirectory(directoryPath);
      return FileVisitResult.CONTINUE;
    }
    
    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
      if (Files.isReadable(path) && DirectoryRecorder.filterPathsBasedOnRegexExcludes(path)
        && Files.isRegularFile(path)) {
        ConcurrentQueues.filesToQuery.add(path);
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
  
  /**
   * Sets up threads and begins the visitor FileVisitor for each root folder configured.
   * Stops each of the threads after
   */
  public static void walkThroughFolders() {
    // Start the thread for processing
  
    DirectoryVisitedSaverRunnable directoryVisitedSaverRunnable = new DirectoryVisitedSaverRunnable();
    Thread visitedSaverRunnableThread = new Thread(directoryVisitedSaverRunnable, "directoryVisitedSaverT" + UUID.randomUUID().toString().subSequence(0,4));
  
    ExistingFileFilterRunnable existingFileFilterRunnable = new ExistingFileFilterRunnable();
    Thread existingFileFilterThread = new Thread(existingFileFilterRunnable, "existingFileFilterThread" + UUID.randomUUID().toString().subSequence(0,4));
  
    existingFileFilterThread.start();
    visitedSaverRunnableThread.start();
  
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
    try {
      stopProcessorThreads(directoryVisitedSaverRunnable, visitedSaverRunnableThread);
      stopProcessorThreads(existingFileFilterRunnable, existingFileFilterThread);
    } catch (InterruptedException e) {
      logger.error("An interrupted exception ");
    }
  }
  
  private static void stopProcessorThreads(ProcessorThread processor, Thread correspondingThread) throws InterruptedException {
    processor.finishedProcessing();
    correspondingThread.join();
  }
}
