package com.sora4222.database.directory;

import com.sora4222.database.configuration.Config;
import com.sora4222.database.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.HashMap;

public class SeedAndSubscriber {
    private static Logger logger = LogManager.getLogger();
    private static Config config = Configuration.getConfiguration();
    private final HashMap<Path, WatchKey> pathsWatched;
    public SeedAndSubscriber() {
        pathsWatched = new HashMap<>();
    }


}
