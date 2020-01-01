package com.sora4222.database.directory;

import com.sora4222.database.directory.processors.ConcurrentQueues;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestSetupDirectoryScan {
  
  @Test
  public void TestFilesAreAdded() throws IOException {
    Files.walkFileTree(Paths.get("src/test/resources/root1/"), SetupDirectoryScan.visitor);
    Assertions.assertEquals(4, ConcurrentQueues.hardDriveSetupQueue.size(),
        "The expected size was 4, actual: " +
            ConcurrentQueues.hardDriveSetupQueue.size() +
            "\n list:" + ConcurrentQueues.hardDriveSetupQueue.toString());
  }
}
