package com.sora4222.database.setup;

import com.sora4222.database.DirectoryRecorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;

public class ScanningTools {
  private static Logger logger = LogManager.getLogger();
  
  public static void subscribeToDirectory(Path directory) {
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
