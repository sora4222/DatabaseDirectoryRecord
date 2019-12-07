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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
public class DatabaseEntries {
  private final DatabaseLookup fileGenerator;
  
  public DatabaseEntries(Path rootDirectoryPath) {
    fileGenerator = new DatabaseLookup(rootDirectoryPath.toAbsolutePath().toString().replace("\\", "/"));
  }
  
  // TODO: Check this
  
  /**
   * I want this to give me small batches of the files stored in the database.
   * Each batch will follow on from the last one.
   * https://www.baeldung.com/java-inifinite-streams
   */
  
  public Stream<FileInformation> getComputersFilesFromDatabase() {
    Supplier<FileInformation> filesFromDatabase = () -> fileGenerator.getFiles();
    return Stream.generate(filesFromDatabase);
  }
  
  public Integer databaseRecordCount() {
    return fileGenerator.count();
  }
  
}

class DatabaseLookup {
  private static final Logger logger = LogManager.getLogger();
  private static final Config config = ConfigurationManager.getConfiguration();
  private static final int hostnameId;
  private static final int number_of_records_at_a_time = 100000;
  private static final Queue<FileInformation> filesToOutput = new LinkedList<>();
  
  static {
    hostnameId = ComputerProperties.computerNameId.get();
  }
  
  private String directory;
  private int iteration;
  
  DatabaseLookup(String directory) {
    this.directory = directory;
    iteration = 0;
  }
  
  FileInformation getFiles() {
    if (filesToOutput.isEmpty()) {
      fillQueue();
    }
    
    return filesToOutput.poll();
  }
  
  private void fillQueue() {
    logger.debug("Queue for database entries is filling.");
    String selectStatement =
        "Select  file_paths.FilePath as FilePath ,FileHash, " +
            "DatabaseRowCreationTime, computer_names.ComputerName as ComputerName " +
            "FROM `directory_records` " +
            "Inner Join computer_names ON directory_records.ComputerId = computer_names.ComputerId " +
            "Inner Join file_paths ON directory_records.FileId = file_paths.FileId " +
            "WHERE computer_names.ComputerId=? " +
            "AND (lower(FilePath) LIKE ?) " +
            "ORDER BY DatabaseRowCreationTime DESC " +
            "LIMIT "
            + number_of_records_at_a_time + " OFFSET ?";
    Connection conn = ConnectionStorage.getConnection();
    
    // For error logging
    String queryAfterFillingIn = "";
    try {
      PreparedStatement stmt = conn.prepareStatement(selectStatement);
      stmt.setInt(1, hostnameId);
      stmt.setString(2, "%" + directory + "%");
      stmt.setInt(3, iteration++ * number_of_records_at_a_time);
      
      queryAfterFillingIn = stmt.toString();
      logger.debug("Fill queue: " + queryAfterFillingIn);
      
      ResultSet files = stmt.executeQuery();
      
      while (files.next()) {
        // This can only be done by recent JDBC
        LocalDateTime rowCreationDate = files.getObject("DatabaseRowCreationTime", LocalDateTime.class);
        filesToOutput.add(
            new FileInformation(
                files.getString("FilePath"),
                files.getString("FileHash"),
                rowCreationDate)
        );
      }
      
    } catch (SQLException e) {
      logger.error("Exception occurred trying to fill the queue for files from the database", e);
      if (!queryAfterFillingIn.equals(""))
        logger.error("Filled query statement:" + queryAfterFillingIn);
    }
  }
  
  Integer count() {
    logger.debug("Obtaining number of database records for directory: " + directory);
    String countStatement = "Select COUNT(*) AS total FROM `" + config.getDataTable() +
        "` WHERE ComputerName=? AND (FilePath LIKE ?)";
    Connection conn = ConnectionStorage.getConnection();
    try {
      PreparedStatement stmt = conn.prepareStatement(countStatement);
      stmt.setInt(1, hostnameId);
      stmt.setString(2, "%" + directory + "%");
      
      logger.debug("Count query: " + stmt.toString());
      
      ResultSet countResult = stmt.executeQuery();
      
      if (!countResult.next()) throw new AssertionError("There is rows in the request for count.");
      return countResult.getInt("total");
      
    } catch (SQLException e) {
      logger.error("Exception occurred trying to count the queue for files from the database: " + e.toString());
      throw new RuntimeException(e);
    } catch (AssertionError e) {
      logger.error(e);
      throw e;
    }
  }
}
