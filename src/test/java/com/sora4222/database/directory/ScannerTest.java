package com.sora4222.database.directory;

import com.sora4222.database.FileInformation;
import com.sora4222.database.configuration.utilityConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * I expect the scanner to be able to go through the root directories
 * find all the files as they are. The scanner needs to be able to
 * give a list of the file information, getting a fingerprint of the
 * file is important.
 */
@SuppressWarnings("WeakerAccess")
public class ScannerTest {
  
  @Test
  public void goesThroughSingleRootDirectory() {
    utilityConfig.setLocationConfig("src/test/resources/root1Only.json");
    
    Scanner testScanner = new Scanner();
    List<FileInformation> scanResults = testScanner.scanAllDirectories();
    
    Assertions.assertFalse(scanResults.isEmpty(), "Scanner returned an empty list.");
    
    List<String> expectedNamesResult = Arrays.asList("innerfile1.txt", "innerfile2.txt", "sharedFile.txt");
    List<String> expectedLocationEndingsResult = Arrays.asList("src/test/resources/level1/level2/innerfile1.txt",
      "src/test/resources/level1/level2/innerfile2.txt", "src/test/resources/sharedFile.txt");
    
    for (FileInformation containedFile : scanResults) {
      Assertions.assertTrue(expectedNamesResult.contains(containedFile.getFileName()));
      Path resultantPath = containedFile.getFullLocation();
      Assertions.assertTrue(expectedLocationEndingsResult.contains(resultantPath.toString()
        .replace("\\", "/")));
    }
  }
  
  @Test
  public void goesThroughMultipleRootDirectories() {
    utilityConfig.setLocationConfig("src/test/resources/root1AndRoot2.json");
    
    Scanner testScanner = new Scanner();
    List<FileInformation> scanResults = testScanner.scanAllDirectories();
    
    Assertions.assertFalse(scanResults.isEmpty(), "Scanner returned an empty list");
    
    List<String> expectedNamesResult = Arrays.asList(
      "innerfile1.txt",
      "innerfile2.txt",
      "sharedFile.txt",
      "root2file1.txt");
    List<String> expectedLocationEndingsResult = Arrays.asList(
      "src/test/resources/root1/level1/level2/innerfile1.txt",
      "src/test/resources/root1/level1/level2/innerfile2.txt",
      "src/test/resources/root1/sharedFile.txt",
      "src/test/resources/root2/root2file1.txt",
      "src/test/resources/root2/sharedFile.txt");
    
    for (FileInformation containedFile : scanResults) {
      Assertions.assertTrue(expectedNamesResult.contains(containedFile.getFileName()));
      Path resultantPath = containedFile.getFullLocation();
      Assertions.assertTrue(expectedLocationEndingsResult.contains(resultantPath.toString()
        .replace("\\", "/")));
    }
  }
}
