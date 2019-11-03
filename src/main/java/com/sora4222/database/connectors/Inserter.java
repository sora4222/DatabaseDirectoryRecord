package com.sora4222.database.connectors;

import com.sora4222.database.configuration.ComputerProperties;
import com.sora4222.database.configuration.Config;
import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.file.FileInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class Inserter {
  private static Logger logger = LogManager.getLogger();
  private static Config config = ConfigurationManager.getConfiguration();
  @SuppressWarnings("SqlResolve")
  private static final String insertionString = "INSERT INTO `" +
    config.getDataTable() +"` (ComputerName, FilePath, FileHash) VALUES (?, ?, ?)";
  
  /**
   * Inserts a list of files into the directory database.
   */
  public static void insertFilesIntoDatabase (List<FileInformation> filesToInsert) {
    if(filesToInsert.size() == 0)
      return;
    
    Connection databaseConnection = ConnectionStorage.getConnection();
    try {
      databaseConnection.setAutoCommit(false);
      PreparedStatement insertionSql = databaseConnection
        .prepareStatement(insertionString);
      
      for (FileInformation file : filesToInsert) {
        insertionSql.setString(1, ComputerProperties.computerName.get());
        insertionSql.setString(2, file.getFullLocationAsLinuxBasedString());
        insertionSql.setString(3, file.getFileHash());
        
        insertionSql.addBatch();
      }
      
      logger.info("SQL statement for insertion: " + insertionSql.toString());
      insertionSql.executeBatch();
      databaseConnection.commit();
      databaseConnection.setAutoCommit(true);
    } catch (SQLException e) {
      rollbackDatabase(databaseConnection);
      logger.error("During an insertion statement there has been an SQL exception: ", e);
      throw new RuntimeException(e);
    } finally {
      ConnectionStorage.close();
    }
  }
  
  private static void rollbackDatabase (Connection databaseConnection) {
    try {
      databaseConnection.rollback();
      databaseConnection.setAutoCommit(true);
    } catch (SQLException e) {
      logger.error("A rollback for insert into database has failed", e);
    }
  }
}
