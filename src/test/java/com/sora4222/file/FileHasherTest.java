package com.sora4222.file;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

public class FileHasherTest {
  
  private static Logger logger = LogManager.getLogger();
  private static File constantFile = new File("src/test/resources/tempConstant.txt");
  
  @BeforeAll
  public static void downloadConstantFile() throws IOException {
    try {
      final URL constantFileLocation = new URL("https", "sora4222.com", "/files/constantFile.txt");
      FileUtils.copyURLToFile(constantFileLocation, constantFile);
    } catch (IOException e) {
      logger.fatal(e);
      throw e;
    }
  }
  
  @Test
  public void testTheHasherHashesValuesSmall() {
    FileHasher fileHasher = new FileHasher(constantFile, 1);
    Assertions.assertEquals(1, fileHasher.getMultiplier());
    Assertions.assertEquals("BFA128CAEBD14DFEF2D9C18545E7031197A56601",
        fileHasher.hashFile());
  }
  
  @Test
  public void differentMultiplierResultsInDifferentValues() {
    File fileToHash = createFileWithByteSize(20000, "src/test/resources/tempFileHasher.txt");
    
    FileHasher threeHundredMultiplier = new FileHasher(fileToHash, 300);
    FileHasher fiveHundredMultiplier = new FileHasher(fileToHash, 500);
    
    logger.info("Entering hashing");
    Assertions.assertNotEquals(threeHundredMultiplier.hashFile(), fiveHundredMultiplier.hashFile());
    
    fileToHash.delete();
  }
  
  @Test()
  public void fileDoesntExist() {
    File fileToHash = new File("");
    Throwable exception = Assertions.assertThrows(Exception.class, () -> new FileHasher(fileToHash));

    Assertions.assertTrue(exception.getCause() instanceof FileNotFoundException);
  }
  
  public static File createFileWithByteSize(final int size, final String fileToCreatePath) {
    File fileToCreate = new File(fileToCreatePath);
    fileToCreate.deleteOnExit();
    
    try {
      logger.info("About to write file");
      writeRandomizedFile(fileToCreate, size);
      
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    return fileToCreate;
  }
  
  private static void writeRandomizedFile(final File fileToCreate, final int size) throws IOException {
    if (fileToCreate.exists())
      fileToCreate.delete();
    
    Assertions.assertTrue(fileToCreate.createNewFile());
    
    FileOutputStream outputStream = new FileOutputStream(fileToCreate);
    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
    
    Random random = new Random();
    byte[] bytes = new byte[size];
    logger.info("Entering randomization.");
    random.nextBytes(bytes);
    logger.info("Exiting randomization.");
    
    bufferedOutputStream.write(bytes);
    
    bufferedOutputStream.flush();
    bufferedOutputStream.close();
    outputStream.close();
  }
}
