package com.sora4222.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class FileHasherTest {
  
  private FileHasher fileHasher = new FileHasher();
  
  @Test
  public void testTheHasherHashesValues() {
    Assertions.assertEquals("bfa128caebd14dfef2d9c18545e7031197a56601".toUpperCase(),
        fileHasher.hashFile(new File("src/test/resources/root1/sharedFile.txt")));
  }
}
