package com.sora4222.database.directory;

import com.sora4222.file.FileInformation;
import com.sora4222.database.configuration.Config;
import com.sora4222.database.configuration.Configuration;
import com.sora4222.file.FileHasher;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Scanner {
    private final Config config = Configuration.getConfiguration();
    private final List<FileInformation> filesFound;
    private final String computerName;
    private static final Logger logger = LogManager.getLogger();

    public Scanner() {
        filesFound = new LinkedList<>();
        this.computerName = getComputerName();
        assert computerName != null;
    }

    private String getComputerName() {
        String linuxHostname =  getLinuxHostname();
        if (!linuxHostname.isEmpty()) {
            logger.info("Linux hostname: " + linuxHostname);
            return linuxHostname;
        } else if (!(System.getenv("COMPUTERNAME") == null)) {
            logger.info("Windows hostname: " + System.getenv("COMPUTERNAME"));
            return System.getenv("COMPUTERNAME");
        } else {
            String message = "The computer does not have a name";
            logger.error(message);

            throw new RuntimeException(message);
        }
    }

    private String getLinuxHostname() {
        InetAddress ip;
        String hostname = "";
        try {
            ip = InetAddress.getLocalHost();
            hostname = ip.getHostName();

        } catch (UnknownHostException e) {

            logger.info("The attempt to obtain a hostname for linux devices has failed.");
        }
        return hostname;
    }

    /**
     * Scan all the directories for any files in the root directories or
     * their sub-directories.
     *
     * @return A list of all the files contained in root or sub-directories
     */
    @SuppressWarnings("ReturnPrivateMutableField")
    public List<FileInformation> scanAllDirectories() {
        for (Path rootLocation : config.getRootLocationsAsPaths()) {
            File rootLocationAsFile = rootLocation.toFile();
            assertRootLocationIsDirectory(rootLocationAsFile);
            scanDirectory(rootLocationAsFile);
        }
        return filesFound;
    }

    private void assertRootLocationIsDirectory(final File rootLocation) {
        if (!rootLocation.isDirectory()) {
            String errorMessage = String.format("The root directory '%s' is not a directory.",
                    rootLocation.getName());
            logger.error(errorMessage);
            throw new AssertionError(errorMessage);
        }
    }

    // The suppression is for the loop which is reported to be able to return null
    @SuppressWarnings("ConstantConditions")
    private void scanDirectory(final File locationDirectory) {
        for (File subFileOrLocation : locationDirectory.listFiles()) {
            processFileType(subFileOrLocation);
        }
    }

    private void processFileType(final File subFileOrLocation) {
        if (subFileOrLocation.isDirectory()) {
            scanDirectory(subFileOrLocation);
        } else {
            addFileToList(subFileOrLocation);
        }
    }

    private void addFileToList(final File fileFound) {
        FileHasher hasher = new FileHasher(fileFound);
        FileInformation fileInformation = new FileInformation(
                fileFound.getName(),
                fileFound.getAbsolutePath(),
                computerName,
                hasher.hashFile());
        filesFound.add(fileInformation);
    }
}
