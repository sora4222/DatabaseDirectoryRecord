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
        Assertions.assertEquals("/full/location", testInformation.getFullLocation().toString().replace("\\", "/"));
        Assertions.assertEquals("MyComputer", testInformation.getComputerName());
    
        LocalDateTime afterDateTime = LocalDateTime.now().plusSeconds(1L);
        
        Assertions.assertTrue(testInformation.getCreationTime().isAfter(beforeDateTime),"Date and time were not after this test was made.");
        Assertions.assertTrue(testInformation.getCreationTime().isBefore(afterDateTime), "Date and time were after the fileInformation was made.");
    }
    
    @Test
    public void testTimeIsCopiedAndEverythingReturns(){
        LocalDateTime testingTime = LocalDateTime.now();
        FileInformation testInformation = new FileInformation("testName", "/full/location", "MyComputer", "md5", testingTime);
        Assertions.assertEquals("testName", testInformation.getFileName());
        Assertions.assertEquals("/full/location", testInformation.getFullLocation().toString().replace("\\", "/"));
        Assertions.assertEquals("MyComputer", testInformation.getComputerName());
        
        
        Assertions.assertEquals(testingTime, testInformation.getCreationTime());
    }
}
