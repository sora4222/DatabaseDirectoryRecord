package com.sora4222.database.directory;

import com.sora4222.database.FileInformation;
import com.sora4222.database.configuration.utilityConfig;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
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
  private static Logger logger = LogManager.getLogger();
  @BeforeAll
  public static void downloadAllFiles() {
    downloadFile("/files/aSecondaryFile.txt", "src/test/resources/root1/level1/level2/aSecondaryFile.txt");
    downloadFile("/files/innerfile1.txt","src/test/resources/root1/level1/innerfile1.txt");
    downloadFile("/files/innerfile2.txt","src/test/resources/root1/level1/innerfile2.txt");
    downloadFile("/files/sharedFile1.txt","src/test/resources/root1/sharedFile1.txt");
  }
  
  private static void downloadFile(String filePathToDownloadFrom, String locationToDownload) {
    try {
      URL conversionToUrl = new URL("https", "sora4222.com", filePathToDownloadFrom);
      FileUtils.copyURLToFile(conversionToUrl, new File(locationToDownload));
    } catch (IOException e) {
      logger.error(e);
      throw new RuntimeException(e);
    }
  }
  
  @Test
  public void goesThroughSingleRootDirectory() {
    utilityConfig.setLocationConfig("src/test/resources/root1Only.json");
    
    Scanner testScanner = new Scanner();
    List<FileInformation> scanResults = testScanner.scanAllDirectories();
    
    Assertions.assertFalse(scanResults.isEmpty(), "Scanner returned an empty list.");
    
    List<String> expectedNamesResult = Arrays.asList(
        "innerfile1.txt",
        "innerfile2.txt",
        "sharedFile1.txt",
        "aSecondaryFile.txt");
    List<String> expectedLocationEndingsResult = Arrays.asList(
        "src/test/resources/root1/level1/innerfile1.txt",
        "src/test/resources/root1/level1/innerfile2.txt",
        "src/test/resources/root1/sharedFile1.txt",
        "src/test/resources/root1/level1/level2/aSecondaryFile.txt");
    
    List<String> expectedHashes = new LinkedList<>(Arrays.asList(
        "BFE2AF0EB5DD84445EDB0C57EAD3DA409223EAD2",
        "E9CF8A147CDC5C6ACF59554854414F5C9EAAB866",
        "B817962D80592D2DAAE997CB63F848FAD3483BDC",
        "3C6F57DBA43DF0035A480CD9BA38CEC0289A6879"
    ));
    
    checkScanResultsAreExpected(scanResults, expectedNamesResult, expectedLocationEndingsResult);
    
    for (FileInformation information : scanResults) {
      Assertions.assertTrue(expectedHashes.remove(information.getFileHash()),
          String.format("The expected file hash list does not contain: %s, Hash list: %s", information.getFileHash(),
              expectedHashes.toString()));
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
        "sharedFile1.txt",
        "sharedFile2.txt",
        "root2file1.txt",
        "aSecondaryFile.txt");
    
    List<String> expectedLocationEndingsResult = Arrays.asList(
        "src/test/resources/root1/level1/innerfile1.txt",
        "src/test/resources/root1/level1/innerfile2.txt",
        "src/test/resources/root1/level1/level2/aSecondaryFile.txt",
        "src/test/resources/root1/sharedFile1.txt",
        "src/test/resources/root2/root2file1.txt",
        "src/test/resources/root2/sharedFile2.txt");
    
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
