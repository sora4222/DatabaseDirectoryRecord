package com.sora4222.database;


import java.util.List;

public class DatabaseChangeSender {
    private final DatabaseWrapper database;
    
    public DatabaseChangeSender (final DatabaseWrapper database) {
        this.database = database;
    }
    
    public void updateDatabase(final List<FileCommand> directoryChanges) {
        return;
    }
}
