package com.sora4222.database;

import com.sora4222.database.configuration.ComputerProperties;
import com.sora4222.database.configuration.Config;
import com.sora4222.database.configuration.Configuration;
import com.sora4222.database.connectors.Inserter;
import com.sora4222.database.connectors.MySqlConnector;
import com.sora4222.database.directory.DatabaseChangeLocator;
import com.sora4222.database.directory.DatabaseQuery;
import com.sora4222.database.directory.Scanner;
import com.sora4222.file.FileInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectoryRecorder {
    private static DatabaseWrapper database;
    private static Scanner scanner;
    private static DatabaseChangeLocator databaseChangeLocator;
    private static DatabaseChangeSender changeSender;
    private static Logger logger = LogManager.getLogger();
    private static Config config = Configuration.getConfiguration();
    private static final HashMap<Path, WatchKey> directoriesWatching = new HashMap<>();
    private static final WatchService subscribeService;

    static {
        try {
            subscribeService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            logger.error("The subscribe service has failed to start", e);
            throw new RuntimeException(e);
        }
    }

    public static void main (String[] args) {
        setupScanning();
        startScanning();
    }

    static void setupScanning () {
        loadDatabase();
        scanner = new Scanner();
        databaseChangeLocator = new DatabaseChangeLocator(database);
        changeSender = new DatabaseChangeSender(database);
    }

    private static void loadDatabase () {
        database = new MySqlConnector();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private static void startScanning () {
        while (true) {
            //Subscribe to directories for the first time
            for (Path confDirPath : config.getRootLocationsAsPaths()) {
                try {
                    try (Stream<Path> objectsInConfigurationDirectories = Files.walk(confDirPath)) {
                        objectsInConfigurationDirectories.parallel().filter(path -> path.toFile().isDirectory()).forEach(path -> {
                            try {
                                directoriesWatching.put(path, path.register(subscribeService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY));
                            } catch (IOException e) {
                                logger.error(e);
                            }
                        });
                    }
                    //Seed all directories
                    try (Stream<Path> objectsInConfigurationDirectories = Files.walk(confDirPath)) {
                        // Obtain all files in database
                        List<FileInformation> allFiles = objectsInConfigurationDirectories
                            .map(path -> new FileInformation(
                                path.getFileName().toString(),
                                path.toString(),
                                ComputerProperties.getComputerName(),
                                ""))
                            .collect(Collectors.toList());

                        List<FileInformation> filesNotInTheDatabase =
                            DatabaseEntries
                                .getComputersFilesFromDatabase()
                                .limit(DatabaseEntries.databaseRecordCount())
                                .parallel()
                                .filter(fileInformation -> !allFiles.contains(fileInformation))
                                .collect(Collectors.toList());

                        // Upload those files.
                        Inserter.insertFilesIntoDatabase(filesNotInTheDatabase);
                    }
                    if (Boolean.getBoolean(System.getProperty("skipUpdateAndDelete", "False"))) {
                        try (Stream<Path> objectsInConfigurationDirectories = Files.walk(confDirPath)) {
                            List<Path> existingFiles = objectsInConfigurationDirectories
                                .parallel()
                                .filter(path -> path.toFile().isFile())
                                .map(path -> path.toAbsolutePath())
                                .collect(Collectors.toList());

                            List<FileCommand> updates = DatabaseQuery
                                .allExistingFilesForThisComputer(existingFiles)
                                .parallelStream()
                                .map(path -> {
                                    if (existingFiles.contains(path))
                                        new FileCommand(
                                            new FileInformation(
                                                path.getFileName().toString(),
                                                path.toString(),
                                                "ComputerName",
                                                "GetFileHash"),
                                            DatabaseCommand.Update);
                                    else
                                        new FileCommand(
                                            new FileInformation(
                                                path.getFileName().toString(),
                                                path.toString(),
                                                "ComputerName",
                                                "GetFileHash"),
                                            DatabaseCommand.Delete);
                                });
                        }
                    }
                } catch (IOException e) {
                    logger.error(String.format("The configured path has had an error: %s", confDirPath.toString()), e);
                }
            }
            //Delete non existent files Stream from the database to here
        }
    }

}
