package com.sora4222.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

public class FileInformationTest {
    
    @Test
    public void testTimeIsDeclaredAndEverythingReturns(){
        LocalDateTime beforeDateTime = LocalDateTime.now();
        
        FileInformation testInformation = new FileInformation("/full/location/testname", "md5");
        Assertions.assertTrue(testInformation.getFullLocation().toString().replace("\\", "/").contains("/full/location"));
    
        LocalDateTime afterDateTime = LocalDateTime.now().plusSeconds(1L);
        
        Assertions.assertTrue(testInformation.getCreationTime().plusNanos(2000L).isAfter(beforeDateTime),"Date and time were not after this test was made.");
        Assertions.assertTrue(testInformation.getCreationTime().minusNanos(2000L).isBefore(afterDateTime), "Date and time were after the fileInformation was made.");
    }
    
    @Test
    public void testTimeIsCopiedAndEverythingReturns(){
      LocalDateTime testingTime = LocalDateTime.now();
      FileInformation testInformation = new FileInformation("/full/location/testName", "md5", testingTime);
        Assertions.assertTrue(
            testInformation.getFullLocation().toString().replace("\\", "/").contains("/full/location"));
        
        Assertions.assertEquals(testingTime, testInformation.getCreationTime());
    }
    
    @Test
    public void testEquals() {
      LocalDateTime testingTime = LocalDateTime.now();
      FileInformation testInformation1 = new FileInformation("/full/location/testName", "md5", testingTime);
      FileInformation testInformation2 = new FileInformation("/full/location/testName", "md5", testingTime);
  
      Assertions.assertEquals(testInformation1, testInformation2);
      Assertions.assertEquals(testInformation1.hashCode(), testInformation2.hashCode());
  
      FileInformation testInformation3 = new FileInformation("/full/location/testName1", "md5", testingTime);
      Assertions.assertNotEquals(testInformation3, testInformation1);
      Assertions.assertNotEquals(testInformation1.hashCode(), testInformation3.hashCode());
  
      FileInformation testInformation4 = new FileInformation("full/hello/testName", "md5", testingTime);
      Assertions.assertNotEquals(testInformation4, testInformation1);
        Assertions.assertNotEquals(testInformation4.hashCode(), testInformation1.hashCode());
  
      FileInformation testInformation5 = new FileInformation("/full/location/testName", "md4", testingTime);
        Assertions.assertNotEquals(testInformation5, testInformation1);
        Assertions.assertNotEquals(testInformation5.hashCode(), testInformation1.hashCode());
  
      FileInformation testInformation6 = new FileInformation("/full/location/testName", "md5", testingTime);
        Assertions.assertEquals(testInformation6, testInformation1);
        Assertions.assertEquals(testInformation6.hashCode(), testInformation1.hashCode());
        
        Assertions.assertNotEquals(testInformation1, "S");
    }
}
