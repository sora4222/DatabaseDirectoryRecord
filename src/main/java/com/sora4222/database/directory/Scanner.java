package com.sora4222.database.directory;

import com.sora4222.database.FileInformation;
import com.sora4222.database.configuration.Config;
import com.sora4222.database.configuration.Configuration;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Scanner {
  private final Config config = Configuration.getConfiguration();
  private final List<FileInformation> filesFound;
  private final String computerName;
  private static final Logger logger = LogManager.getLogger();
  
  public Scanner() {
    filesFound = new LinkedList<>();
    this.computerName = getComputerName();
  }
  
  private String getComputerName() {
    if (!(System.getenv("HOSTNAME") == null)) {
      return System.getenv("HOSTNAME");
    } else if (!(System.getenv("COMPUTERNAME") == null)) {
      return System.getenv("COMPUTERNAME");
    } else {
      String message ="The computer does not have a name";
      logger.error(message);
      throw new RuntimeException(message);
    }
  }
  
  /**
   * Scan all the directories for any files in the root directories or
   * their sub-directories.
   *
   * @return A list of all the files contained in root or sub-directories
   */
  @SuppressWarnings("ReturnPrivateMutableField")
  public List<FileInformation> scanAllDirectories() {
    for (Path rootLocation : config.getRootLocationsAsPaths()) {
      File rootLocationAsFile = rootLocation.toFile();
      assertRootLocationIsDirectory(rootLocationAsFile);
      scanDirectory(rootLocationAsFile);
    }
    return filesFound;
  }
  
  private void assertRootLocationIsDirectory(final File rootLocation) {
    if(!rootLocation.isDirectory()) {
      String errorMessage = String.format("The root directory '%s' is not a directory.",
          rootLocation.getName());
      logger.error(errorMessage);
      throw new AssertionError(errorMessage);
    }
  }
  
  // The suppression is for the loop which is reported to be able to return null
  @SuppressWarnings("ConstantConditions")
  private void scanDirectory(final File locationDirectory) {
    for (File subFileOrLocation : locationDirectory.listFiles()) {
      processFileType(subFileOrLocation);
    }
  }
  
  private void processFileType(final File subFileOrLocation) {
    if (subFileOrLocation.isDirectory()) {
      scanDirectory(subFileOrLocation);
    } else {
      addFileToList(subFileOrLocation);
    }
  }
  
  private void addFileToList(final File fileFound) {
    FileInformation fileInformation = new FileInformation(
        fileFound.getName(),
        fileFound.getAbsolutePath(),
        computerName,
        "md5");
    filesFound.add(fileInformation);
  }
}
