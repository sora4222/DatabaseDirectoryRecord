package com.sora4222.database.connectors;

import com.sora4222.database.configuration.ComputerProperties;
import com.sora4222.file.FileInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@SuppressWarnings("SqlResolve")
public class Updater {
  private static Logger logger = LogManager.getLogger();
  private static String updateCommand =
      "UPDATE `directory_records` SET FileHash=? " +
          "WHERE FileId IN (SELECT FileId FROM file_paths WHERE FilePath = ?) " +
          "AND ComputerId=?";
  
  public static void sendUpdatesToDatabase (final List<FileInformation> filesInDBToUpdate) {
    if(filesInDBToUpdate.size() == 0)
      return;
    
    Connection databaseConnection = ConnectionStorage.getConnection();
    try {
      PreparedStatement updateStmt = databaseConnection.prepareCall(updateCommand);
      for(FileInformation file: filesInDBToUpdate) {
        updateStmt.setString(1, file.getFileHash());
        updateStmt.setString(2, file.getFullLocation().toString());
        updateStmt.setInt(3, ComputerProperties.computerNameId.get());
  
        updateStmt.addBatch();
      }
      updateStmt.executeUpdate();
    } catch (SQLException e) {
      logger.error("There has been an error trying to delete a group of rows.", e);
    }
  }
}
