package com.sora4222.file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Random;

class FileHasherTest {
  
  private static Logger logger = LogManager.getLogger();
  private static File constantFile = new File("src/test/resources/tempConstant.txt");
  
  @Test
  void testTheHasherHashesValuesSmall() {
    FileHasher fileHasher = new FileHasher(constantFile, 1);
    FileHasher secondHasher = new FileHasher(constantFile, 1);
    Assertions.assertEquals(1, fileHasher.getMultiplier());
    String hash = fileHasher.hashFile();
    Assertions.assertEquals(secondHasher.hashFile(),
        hash);
    Assertions.assertEquals("ed6f9f68fc072d601e4f3ebb70403d10a9a7249e",
        hash);
  }
  
  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  void differentMultiplierResultsInDifferentValues() {
    File fileToHash = createFileWithByteSize(20000, "src/test/resources/tempFileHasher.txt");
    
    FileHasher threeHundredMultiplier = new FileHasher(fileToHash, 300);
    FileHasher fiveHundredMultiplier = new FileHasher(fileToHash, 500);
    
    logger.info("Entering hashing");
    Assertions.assertNotEquals(threeHundredMultiplier.hashFile(), fiveHundredMultiplier.hashFile());
    
    fileToHash.delete();
  }
  
  @Test()
  void fileDoesntExist() {
    File fileToHash = new File("");
    Throwable exception = Assertions.assertThrows(Exception.class, () -> new FileHasher(fileToHash));

    Assertions.assertTrue(exception.getCause() instanceof FileNotFoundException);
  }
  
  static File createFileWithByteSize(final int size, final String fileToCreatePath) {
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
