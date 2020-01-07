package com.sora4222.database.directory;

import com.sora4222.database.setup.SetupOrchestration;
import com.sora4222.database.setup.processors.ConcurrentQueues;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestSetupOrchestration {
  private static Logger logger = LogManager.getLogger();
  Path home = Paths.get("src/test/resources/tempTestScan/");
  Path add1 = home.resolve(Paths.get("create1/"));
  Path add2 = home.resolve(Paths.get("create2/create3/"));
  Path add3 = home.resolve(Paths.get("create2/create4/"));
  
  @AfterEach
  @BeforeEach
  public void removeCreatedDirectories() throws IOException {
    logger.info(Files.deleteIfExists(add1.toAbsolutePath()));
    logger.info(Files.deleteIfExists(add2.toAbsolutePath()));
    logger.info(Files.deleteIfExists(add3.toAbsolutePath()));
  }
  
  @BeforeEach
  public void emptyAllQueues() {
    ConcurrentQueues.visitedDirectoriesQueue.clear();
    ConcurrentQueues.filesToUpload.clear();
  }
  
  @Test
  public void testFilesAreAdded() throws IOException {
    Files.walkFileTree(Paths.get("src/test/resources/root1/"), SetupOrchestration.visitor);
    Assertions.assertEquals(4, ConcurrentQueues.filesToUpload.size(),
      "The expected size was 4, actual: " +
        ConcurrentQueues.filesToUpload.size() +
        "\n list:" + ConcurrentQueues.filesToUpload.toString());
  }
  
  @Test
  public void testFoldersAddToTheDirectoryQueue() throws IOException {
    Files.walkFileTree(Paths.get("src/test/resources/root1/"), SetupOrchestration.visitor);
    Assertions.assertEquals(3, ConcurrentQueues.visitedDirectoriesQueue.size(),
      "The number of directories expected to be seen is 3. The number detected is: " + ConcurrentQueues.visitedDirectoriesQueue.size());
  }
  
}
