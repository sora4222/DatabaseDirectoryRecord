package com.sora4222.database;

import com.sora4222.database.configuration.ComputerProperties;
import com.sora4222.file.FileInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
public class DatabaseEntries {
    // TODO: Check this
    /**
     * I want this to give me small batches of the files stored in the database.
     * Each batch will follow on from the last one.
     * https://www.baeldung.com/java-inifinite-streams
     */
    public static Stream<FileInformation> getComputersFilesFromDatabase() {
        Supplier<FileInformation> filesFromDatabase = () -> computerFileGenerator.getFiles();
        return Stream.generate(filesFromDatabase);
    }

    public static Integer databaseRecordCount() {
        return 0;
    }

}

class computerFileGenerator {
    private static final Logger logger = LogManager.getLogger();
    static final String hostname;

    private static final Queue<FileInformation> filesToOutput = new LinkedList<>();

    static {
        hostname = ComputerProperties.computerName.get();
    }

    static FileInformation getFiles() {
        if(filesToOutput.isEmpty()){
            fillQueue();
        }

        return filesToOutput.poll();
    }

    private static void fillQueue () {
        // For intention
        String selectStatement = "Select * From directory_recorder SORT DESC LIMIT 100 SKIP 0";
    }
}
