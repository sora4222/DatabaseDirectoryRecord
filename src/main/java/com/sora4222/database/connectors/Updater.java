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

@SuppressWarnings("SqlResolve")
public class Updater {
  private static Logger logger = LogManager.getLogger();
  private static final Config config = ConfigurationManager.getConfiguration();
  private static String updateCommand =
    "UPDATE `" +
      config.getDataTable() +
      "` SET FileHash=? " +
      "WHERE FilePath=? AND ComputerName=?";
  
  public static void sendUpdatesToDatabase (final List<FileInformation> filesInDBToUpdate) {
    if(filesInDBToUpdate.size() == 0)
      return;
    
    Connection databaseConnection = ConnectionStorage.getConnection();
    try {
      PreparedStatement deleteStatement = databaseConnection.prepareCall(updateCommand);
      for(FileInformation file: filesInDBToUpdate) {
        deleteStatement.setString(1, file.getFileHash());
        deleteStatement.setString(2, file.getFullLocation().toString());
        deleteStatement.setString(3, ComputerProperties.computerName.get());
        
        deleteStatement.addBatch();
      }
      deleteStatement.executeUpdate();
    } catch (SQLException e) {
      logger.error("There has been an error trying to delete a group of rows.", e);
    } finally {
      ConnectionStorage.close();
    }
  }
}
