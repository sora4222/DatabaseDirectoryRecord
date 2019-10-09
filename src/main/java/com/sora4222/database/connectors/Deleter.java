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

public class Deleter {
  private static Logger logger = LogManager.getLogger();
  private static final Config config = ConfigurationManager.getConfiguration();
  private static String deleteCommand =
    "DELETE FROM `" +
    config.getDataTable() +
    "` WHERE FilePath=? AND ComputerName=?";
  
  @SuppressWarnings("SqlResolve")
  public static void sendDeletesToDatabase (final List<FileInformation> filesInDBToDelete) {
    Connection databaseConnection = ConnectionStorage.getConnection();
    try {
      PreparedStatement deleteStatement = databaseConnection.prepareCall(deleteCommand);
      for(FileInformation fileToDelete: filesInDBToDelete) {
        deleteStatement.setString(1, fileToDelete.getFullLocationAsLinuxBasedString());
        deleteStatement.setString(2, ComputerProperties.computerName.get());

        System.out.println(deleteStatement.toString());
        logger.info("SQL statement to execute: " + deleteStatement.toString());
        deleteStatement.execute();
        System.out.println(deleteStatement.getUpdateCount());
      }
      
    } catch (SQLException e) {
      logger.error("There has been an error trying to delete a group of rows.", e);
    } finally {
//      ConnectionStorage.close();
    }
  }
}
