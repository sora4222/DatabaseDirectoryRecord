package com.sora4222.file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.LinkedList;

@SuppressWarnings("WeakerAccess")
public class FileHasherSimplePerformanceTests {
  private static Logger logger = LogManager.getLogger();
  private static int NUMBER_OF_TIMES_TO_REPEAT = 3;
  
  static File verySmallFile = new File("");
  static File smallFile = new File("");
  static File megabyteFile = new File("");
  static File gigabyteFile = new File("");
  
  
  @BeforeAll
  public static void createAllTemporaryFiles() {
    verySmallFile = FileHasherTest.createFileWithByteSize(FileHasher.smallFileSize - 1, "src/test/resources/tempVerySmallFile.txt");
    smallFile = FileHasherTest.createFileWithByteSize(1000000 - 1, "src/test/resources/tempSmallFile.txt");
    megabyteFile = FileHasherTest.createFileWithByteSize(100000000 - 1, "src/test/resources/tempMegabyteFile.txt");
    gigabyteFile = FileHasherTest.createFileWithByteSize(1000000000 - 1, "src/test/resources/tempGigaFile.txt");
  }
  
  
  @AfterAll
  public static void deleteAllTemporaryFiles() {
    FileHasherSimplePerformanceTests.deleteAndLog(verySmallFile);
    FileHasherSimplePerformanceTests.deleteAndLog(smallFile);
    FileHasherSimplePerformanceTests.deleteAndLog(megabyteFile);
    FileHasherSimplePerformanceTests.deleteAndLog(gigabyteFile);
  }
  
  public static void deleteAndLog(File fileToDelete) {
    if(!fileToDelete.delete())
      logger.error("file did not delete");
  }
  
  @Test
  public void verySmallFilesReadWithinFiveSeconds() {
    LinkedList<Double> times = new LinkedList<>();
    for (int i = 0; i < NUMBER_OF_TIMES_TO_REPEAT; i++) {
      long timeTaken = callWithFile(verySmallFile);
      logger.info(String.format("Time taken: '%d'ms", timeTaken));
      times.add(timeTaken / 1000.0);
    }
    Double average = getListAverage(times);
  
    assertAverageBelowFiveSeconds(average);
  }
  
  @Test
  public void smallFilesReadWithinFiveSeconds() {
    LinkedList<Double> times = new LinkedList<>();
    for (int i = 0; i < NUMBER_OF_TIMES_TO_REPEAT; i++) {
      long timeTaken = callWithFile(smallFile);
      logger.info(String.format("Time taken: '%d'ms", timeTaken));
      times.add(timeTaken / 1000.0);
    }
    Double average = getListAverage(times);
  
    assertAverageBelowFiveSeconds(average);
  }
  
  @Test
  public void megabyteFilesReadWithinFiveSeconds() {
    LinkedList<Double> times = new LinkedList<>();
    for (int i = 0; i < NUMBER_OF_TIMES_TO_REPEAT; i++) {
      long timeTaken = callWithFile(megabyteFile);
      logger.info(String.format("Time taken: '%d'ms", timeTaken));
      times.add(timeTaken / 1000.0);
    }
    Double average = getListAverage(times);
    
    assertAverageBelowFiveSeconds(average);
  }
  
  @Test
  public void gigabyteFilesReadWithinFiveSeconds() {
    LinkedList<Double> times = new LinkedList<>();
    for (int i = 0; i < NUMBER_OF_TIMES_TO_REPEAT; i++) {
      long timeTaken = callWithFile(gigabyteFile);
      logger.info(String.format("Time taken: '%d'ms", timeTaken));
      times.add(timeTaken / 1000.0);
    }
    Double average = getListAverage(times);
    
    assertAverageBelowFiveSeconds(average);
  }
  
  private void assertAverageBelowFiveSeconds(final Double averageTime) {
    logger.info(String.format("Average time: %s", averageTime));
    Assertions.assertTrue(averageTime <= 5,
        String.format("Average amount of time: %s", averageTime));
  }
  
  private Double getListAverage(final LinkedList<Double> timeList) {
    @SuppressWarnings("Convert2MethodRef") Double sum = timeList.stream().reduce(0.0, (a, b) -> a + b);
    int count = timeList.size();
    return sum / count;
  }
  
  private long callWithFile(final File fileToHash) {
    long startTime = System.currentTimeMillis();
    
    FileHasher hasher = new FileHasher(fileToHash);
    hasher.hashFile();
    
    long stopTime = System.currentTimeMillis();
    return stopTime - startTime;
  }
}
