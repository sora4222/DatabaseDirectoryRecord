package com.sora4222.database.directory;

import com.sora4222.database.thread.Tools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestSetupDirectoryScan {
  
  @Test
  public void TestFilesAreAdded() throws IOException {
    Files.walkFileTree(Paths.get("src/test/resources/root1/"), SetupDirectoryScan.visitor);
    Assertions.assertEquals(4, Tools.hardDriveSetupQueue.size(),
      "The expected size was 4, actual: " +
        Tools.hardDriveSetupQueue.size() +
        "\n list:" + Tools.hardDriveSetupQueue.toString());
  }
}
