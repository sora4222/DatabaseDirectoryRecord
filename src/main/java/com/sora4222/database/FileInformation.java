package com.sora4222.database;

import lombok.Getter;

import java.time.LocalDateTime;

public class FileInformation {
    
    @Getter private final String fileName;
    @Getter private final String fullLocation;
    @Getter private final String computerName;
    @Getter private final LocalDateTime creationTime;
    
    public FileInformation(String fileName, String fullLocation, String computerName){
        this.fileName = fileName;
        this.fullLocation = fullLocation;
        this.computerName = computerName;
        
        this.creationTime = LocalDateTime.now();
    }
    
    public FileInformation(String fileName, String fullLocation, String computerName, LocalDateTime creationTime){
        this.fileName = fileName;
        this.fullLocation = fullLocation;
        this.computerName = computerName;
        
        this.creationTime = creationTime;
    }
}
