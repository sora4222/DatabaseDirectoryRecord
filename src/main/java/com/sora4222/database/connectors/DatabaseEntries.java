package com.sora4222.database.connectors;

import com.sora4222.database.configuration.ComputerProperties;
import com.sora4222.file.FileInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class DatabaseEntries {
  private static final Logger logger = LogManager.getLogger();
  private static final int hostnameId;
  
  static {
    hostnameId = ComputerProperties.computerNameId.get();
  }
  
  public List<FileInformation> getFiles() {
    String selectStatement =
      "Select  file_paths.AbsoluteFilePath as FilePath ,FileHash, " +
        "DatabaseRowCreationTime, computer_names.ComputerName as ComputerName " +
        "FROM `directory_records` " +
        "Inner Join computer_names ON directory_records.ComputerId = computer_names.ComputerId " +
        "Inner Join file_paths ON directory_records.FileId = file_paths.FileId " +
        "WHERE computer_names.ComputerId=? " +
        "ORDER BY DatabaseRowCreationTime DESC ";
    Connection conn = ConnectionStorage.getConnection();
    
    
    LinkedList<FileInformation> filesToOutput = new LinkedList<>();
    try {
      PreparedStatement stmt = conn.prepareStatement(selectStatement);
      stmt.setInt(1, hostnameId);
      
      
      ResultSet files = stmt.executeQuery();
      
      while (files.next()) {
        // This can only be done by recent JDBC
        LocalDateTime rowCreationDate =
          files.getObject("DatabaseRowCreationTime", LocalDateTime.class);
        filesToOutput.add(
          new FileInformation(
            files.getString("AbsoluteFilePath"),
            files.getString("FileHash"),
            rowCreationDate)
        );
      }
      
    } catch (SQLException e) {
      logger.error("Exception occurred trying to fill the queue for files from the database",
        e);
      throw new RuntimeException(e);
    }
    
    return filesToOutput;
  }
}