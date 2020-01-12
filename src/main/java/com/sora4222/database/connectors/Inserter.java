package com.sora4222.database.connectors;

import com.sora4222.database.configuration.ComputerProperties;
import com.sora4222.file.FileInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class Inserter {
  private static Logger logger = LogManager.getLogger();
  @SuppressWarnings("SqlResolve")
  private static final String insertFileSql = "INSERT IGNORE INTO `file_paths` (AbsoluteFilePath) " +
    "SELECT ? " +  // This select defines the FilePath to be inserted
    "WHERE (SELECT COUNT(AbsoluteFilePath) FROM  `file_paths` WHERE AbsoluteFilePath = ?) = 0";
  private static final String insertionRecordSql =
    "INSERT IGNORE INTO `directory_records` (ComputerId, FileId, FileHash) " +
      "VALUES (?, (SELECT FileId FROM file_paths WHERE AbsoluteFilePath = ?), ?)";
  
  private static final String insertDirectory =
    "INSERT IGNORE INTO `directories_stored` (AbsoluteFilePath, ComputerId) VALUES (?, ?)";
  
  public static void insertRecordIntoDatabase(List<FileInformation> filesToInsert) {
    insertRecordIntoDatabase(ConnectionStorage.getConnection(), filesToInsert);
  }
  
  /**
   * Inserts a list of files into the directory database.
   * Will take the files passed and insert the path into the file path table,
   * and then insert the rest of the file information into the main table.
   */
  public static void insertRecordIntoDatabase(Connection databaseConnection, List<FileInformation> filesToInsert) {
    if (filesToInsert.size() == 0)
      return;
    
    insertFilesToFileTable(filesToInsert);
    try {
      PreparedStatement insertionSql = databaseConnection
        .prepareStatement(insertionRecordSql);
      
      logger.debug("Files to insert in database count: " + filesToInsert.size());
      
      for (FileInformation file : filesToInsert) {
        insertionSql.setInt(1, ComputerProperties.computerNameId.get());
        insertionSql.setString(2, file.getFullLocationAsLinuxBasedString());
        insertionSql.setString(3, file.getFileHash());
  
        insertionSql.addBatch();
      }
  
      logger.info("SQL statement for insertion: " + insertionSql.toString());
      insertionSql.executeBatch();
    } catch (SQLException e) {
      logger.error("During an insertion statement there has been an SQL exception: ", e);
      throw new RuntimeException(e);
    } catch (NullPointerException e) {
      logger.error("A null pointer exception occurred in the insertRecordIntoDatabase: " + e.toString());
      throw e;
    }
  }
  
  /**
   * Inserts the file name component into the database.
   * This is done separate from the insertRecordIntoDatabase which adds the computer and hash components.
   *
   * @param filesToInsert the files that will have their path inserted.
   */
  private static void insertFilesToFileTable(List<FileInformation> filesToInsert) {
    Connection databaseConnection = ConnectionStorage.getConnection();
    try {
      PreparedStatement insertionSql = databaseConnection
        .prepareStatement(insertFileSql);
      
      for (FileInformation file : filesToInsert) {
        insertionSql.setString(1, file.getFullLocationAsLinuxBasedString());
        insertionSql.setString(2, file.getFullLocationAsLinuxBasedString());
        insertionSql.addBatch();
      }
      logger.debug("SQL statement for insertion (files): " + insertionSql.toString());
      
      insertionSql.executeBatch();
    } catch (SQLException e) {
      logger.error("During an insertion statement there has been an SQL exception: ", e);
      throw new RuntimeException(e);
    } catch (NullPointerException e) {
      logger.error("A null pointer exception occurred in the insertFilesToFileTable: " + e.toString());
      throw e;
    }
  }
  
  public static void insertDirectoriesToDirectoryTable(List<Path> directoriesToInsert, Connection databaseConnection) {
    try {
      PreparedStatement insertionSql = databaseConnection.prepareStatement(insertDirectory);
      for (Path path : directoriesToInsert) {
        insertionSql.setString(1, path.toAbsolutePath().toString().replace("\\", "/"));
        insertionSql.setInt(2, ComputerProperties.computerNameId.get());
        insertionSql.addBatch();
      }
      logger.debug("SQL statement for insertion (directories): " + insertionSql.toString());
      insertionSql.executeBatch();
    } catch (SQLException e) {
      logger.error("Inserting directories");
    }
  }
}
