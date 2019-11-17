package com.sora4222.database.connectors;

import com.sora4222.database.configuration.ComputerProperties;
import com.sora4222.database.configuration.Config;
import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.file.FileInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class DatabaseQuery {
  private static final Config config = ConfigurationManager.getConfiguration();
  private static final Logger logger = LogManager.getLogger();
  private static final String singleFileCheckQuery =
    "SELECT * FROM `" + config.getDataTable() + "` WHERE ComputerName=? AND FilePath=?";
  private static final String allComputerFileCheck =
    "SELECT FilePath, FileHash FROM `" + config.getDataTable() + "` WHERE ComputerName=?";
  
  public static Collection<FileInformation> allFilesAlreadyInBothComputerAndDatabase(List<FileInformation> filesInHardDrive) {
    Connection connection = ConnectionStorage.getConnection();
    try {
      PreparedStatement checkEachFileIsThere = connection.prepareStatement(allComputerFileCheck);
      checkEachFileIsThere.setString(1, ComputerProperties.computerName.get());
  
      logger.info("Select statement: " + checkEachFileIsThere.toString());
      ResultSet resultSet = checkEachFileIsThere.executeQuery();
      
      final HashSet<FileInformation> hardDriveFiles = new HashSet<>(filesInHardDrive);
      while (resultSet.next()) {
        hardDriveFiles.remove(
          new FileInformation(
            resultSet.getString("FilePath"),
            ComputerProperties.computerName.get(),
            resultSet.getString("FileHash")));
      }
      logger.debug("Returning: " + hardDriveFiles.toString());
      return new LinkedList<>(hardDriveFiles);
    } catch (SQLException e) {
      logger.error("Checking for a batch of files in the database, query template: " + allComputerFileCheck, e);
    } finally {
      ConnectionStorage.close();
    }
    
    return new LinkedList<>();
  }
}
