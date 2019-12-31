package com.sora4222.database;

import com.sora4222.database.configuration.UtilityForConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DirectoryRecorderMethodsTests {
  private static final String LOCATION_OF_ROOT_ONE_ROOT_TWO = "src/test/resources/root1AndRoot2.json";
  
  @Test
  void allFilesGathered() {
    System.setProperty("config", LOCATION_OF_ROOT_ONE_ROOT_TWO);
    UtilityForConfig.clearConfig();
    List<String> files =
        DirectoryRecorder.gatherAllFilesUnderRootPath(Paths.get("src/test/resources/root1"))
            .stream()
            .map(fileInformation -> fileInformation.getFullLocation().getFileName().toString())
            .sorted(String::compareTo)
            .collect(Collectors.toList());
  
    List<String> filesExpected = Arrays.asList("aSecondaryFile.txt", "sharedFile1.txt",
        "innerfile1.txt", "innerfile2.txt");
    filesExpected.sort(String::compareTo);
  
    Assertions.assertEquals(filesExpected, files, filesExpected.toString());
  }
}
