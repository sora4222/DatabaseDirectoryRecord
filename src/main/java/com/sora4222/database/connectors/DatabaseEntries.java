package com.sora4222.database.connectors;

import com.sora4222.database.configuration.ComputerProperties;
import com.sora4222.database.configuration.Config;
import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.file.FileInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
public class DatabaseEntries {
    // TODO: Check this
    /**
     * I want this to give me small batches of the files stored in the database.
     * Each batch will follow on from the last one.
     * https://www.baeldung.com/java-inifinite-streams
     * @param rootDirectoryPath
     */
    public static Stream<FileInformation> getComputersFilesFromDatabase(Path rootDirectoryPath) {
        Supplier<FileInformation> filesFromDatabase =
            () -> computerFileGenerator.getFiles(
                rootDirectoryPath.toAbsolutePath().toString().replace("\\", "/"));
        
        return Stream.generate(filesFromDatabase);
    }

    public static Integer databaseRecordCount() {
        return 0;
    }

}

class computerFileGenerator {
    private static final Logger logger = LogManager.getLogger();
    private static final Config config = ConfigurationManager.getConfiguration();
    private static final String hostname;

    private static final Queue<FileInformation> filesToOutput = new LinkedList<>();
    private String directory;
    private int iteration;
    
    static {
        hostname = ComputerProperties.computerName.get();
    }
    
    FileInformation getFiles(String pathForRootDirectory) {
        if(filesToOutput.isEmpty()){
            fillQueue();
        }

        return filesToOutput.poll();
    }
    
    private void fillQueue() {
        // For intention
        String selectStatement = "Select * From `" + config.getDataTable() + "` WHERE ComputerName=? AND (lower(FilePath) LIKE ?) ORDER BY DatabaseRowCreationTime DESC LIMIT 10000 OFFSET ?";
        Connection conn = ConnectionStorage.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(selectStatement);
            stmt.setInt(1, iteration * 10000);
            stmt.setString(2, "%" + directory + "%");
            stmt.setString(3, hostname);
            stmt.executeQuery();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
    }
}
