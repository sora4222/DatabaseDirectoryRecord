package com.sora4222.file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class FileHasherTest {
  
  private static Logger logger = LogManager.getLogger();
  
  @Test
  public void testTheHasherHashesValuesSmall() {
    FileHasher fileHasher = new FileHasher(new File("src/test/resources/root1/sharedFile1.txt"));
    Assertions.assertEquals("BFE2AF0EB5DD84445EDB0C57EAD3DA409223EAD2",
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
    if(fileToCreate.exists())
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
