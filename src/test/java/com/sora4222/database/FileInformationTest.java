package com.sora4222.database;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

public class FileInformationTest {
    
    @Test
    public void testTimeIsDeclaredAndEverythingReturns(){
        LocalDateTime beforeDateTime = LocalDateTime.now();
        
        FileInformation testInformation = new FileInformation("testName", "/full/location", "MyComputer", "md5");
        Assertions.assertEquals("testName", testInformation.getFileName());
        Assertions.assertTrue(testInformation.getFullLocation().toString().replace("\\", "/").contains("/full/location"));
        Assertions.assertEquals("MyComputer", testInformation.getComputerName());
    
        LocalDateTime afterDateTime = LocalDateTime.now().plusSeconds(1L);
        
        Assertions.assertTrue(testInformation.getCreationTime().plusNanos(2000L).isAfter(beforeDateTime),"Date and time were not after this test was made.");
        Assertions.assertTrue(testInformation.getCreationTime().minusNanos(2000L).isBefore(afterDateTime), "Date and time were after the fileInformation was made.");
    }
    
    @Test
    public void testTimeIsCopiedAndEverythingReturns(){
        LocalDateTime testingTime = LocalDateTime.now();
        FileInformation testInformation = new FileInformation("testName", "/full/location", "MyComputer", "md5", testingTime);
        Assertions.assertEquals("testName", testInformation.getFileName());
        Assertions.assertTrue(
            testInformation.getFullLocation().toString().replace("\\", "/").contains("/full/location"));
        Assertions.assertEquals("MyComputer", testInformation.getComputerName());
        
        
        Assertions.assertEquals(testingTime, testInformation.getCreationTime());
    }
}
