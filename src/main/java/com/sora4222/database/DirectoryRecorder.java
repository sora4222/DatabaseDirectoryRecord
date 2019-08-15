package com.sora4222.database;

import com.sora4222.database.directory.DatabaseChangeSender;
import com.sora4222.database.directory.DirectoryChangeLocator;
import com.sora4222.database.directory.DirectoryScanner;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

public class DirectoryRecorder {
    private static DatabaseWrapper database;
    private static DirectoryScanner directoryScanner;
    private static DirectoryChangeLocator changeLocator;
    private static DatabaseChangeSender changeSender;
    
    public static void main (String[] args) {
        
        setupScanning();
        startScanning();
    }
    
    private static void setupScanning () {
        database = loadDatabase();
        directoryScanner = new DirectoryScanner();
        changeLocator = new DirectoryChangeLocator(database);
        changeSender = new DatabaseChangeSender(database);
    }
    
    private static DatabaseWrapper loadDatabase () {
        throw new NotImplementedException("Not currently implemented");
    }
    
    @SuppressWarnings("InfiniteLoopStatement")
    private static void startScanning(){
        while(true){
            List<FileInformation> filesInDirectories = directoryScanner.scanAllDirectories();
            List<FileInformation> directoryChanges = changeLocator.findChangesToDirectory(filesInDirectories);
            changeSender.sendAllFiles(directoryChanges);
        }
    }
}
