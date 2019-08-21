package com.sora4222.database.directory;

import com.sora4222.database.FileInformation;
import com.sora4222.database.configuration.utilityConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

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
    
    List<String> expectedNamesResult = Arrays.asList(
        "innerfile1.txt",
        "innerfile2.txt",
        "sharedFile.txt",
        "aSecondaryFile.txt");
    List<String> expectedLocationEndingsResult = Arrays.asList(
        "src/test/resources/root1/level1/innerfile1.txt",
        "src/test/resources/root1/level1/innerfile2.txt",
        "src/test/resources/root1/sharedFile.txt",
        "src/test/resources/root1/level1/level2/aSecondaryFile.txt");
    
    List<String> expectedHashes = new LinkedList<>(Arrays.asList(
        "bfa128caebd14dfef2d9c18545e7031197a56601".toUpperCase(),
        "467c9ceffdceaec8f055279d71bba127740c38a0".toUpperCase(),
        "c7745b7d45dce6791d2f034800ea2c61d5cfc51a".toUpperCase(),
        "60ac906cafee61392326a24bfd97c472d0e5ba71".toUpperCase()
    ));
    
    checkScanResultsAreExpected(scanResults, expectedNamesResult, expectedLocationEndingsResult);
    
    for (FileInformation information : scanResults) {
      Assertions.assertTrue(expectedHashes.remove(information.getFileHash()),
          String.format("The expected file hash list does not contain: %s", information.getFileHash()));
    }
    Assertions.assertTrue(expectedHashes.isEmpty(), expectedHashes.toString());
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
        "root2file1.txt",
        "aSecondaryFile.txt");
    
    List<String> expectedLocationEndingsResult = Arrays.asList(
        "src/test/resources/root1/level1/innerfile1.txt",
        "src/test/resources/root1/level1/innerfile2.txt",
        "src/test/resources/root1/level1/level2/aSecondaryFile.txt",
        "src/test/resources/root1/sharedFile.txt",
        "src/test/resources/root2/root2file1.txt",
        "src/test/resources/root2/sharedFile.txt");
    
    checkScanResultsAreExpected(scanResults, expectedNamesResult, expectedLocationEndingsResult);
  }
  
  private void checkScanResultsAreExpected(List<FileInformation> scanResults, List<String> filesToCheck, List<String> locationsToCheck) {
    for (FileInformation containedFile : scanResults) {
      Assertions.assertTrue(filesToCheck.contains(containedFile.getFileName()),
          String.format("The file: %s was not contained in the list of expected files", containedFile.getFileName()));
      
      Path relativePathFromProjectRoot = Paths.get("").toAbsolutePath().relativize(containedFile.getFullLocation());
      Assertions.assertTrue(
          locationsToCheck
              .contains(relativePathFromProjectRoot.toString()
                  .replace("\\", "/")),
          String.format("The path: '%s' was not found in the list of expected paths",
              relativePathFromProjectRoot.toString()));
    }
  }
}
