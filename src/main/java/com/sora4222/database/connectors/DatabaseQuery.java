package com.sora4222.database.connectors;

import com.sora4222.database.configuration.ComputerProperties;
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
  private static final Logger logger = LogManager.getLogger();
  private static final String allComputerFileCheck =
      "SELECT FilePath, FileHash FROM `directory_records` " +
          "INNER JOIN file_paths ON directory_records.FileId = file_paths.FileId " +
          "WHERE ComputerId IN (SELECT ComputerId FROM computer_names WHERE ComputerName=?)";
  
  /**
   * Connects to a database and queries the files from the device this is called on.
   * The query then removes from the list given all files already contained in the database.
   * @param filesInHardDrive The files that are in the hard drive in FileInformation format
   * @return A list with files not in the database
   */
  public static List<FileInformation> queryTheDatabaseForFiles(
      List<FileInformation> filesInHardDrive) {
    Connection connection = ConnectionStorage.getConnection();
    try {
      PreparedStatement checkEachFileIsThere = connection.prepareStatement(allComputerFileCheck);
      checkEachFileIsThere.setInt(1, ComputerProperties.computerNameId.get());
      
      logger.info("Select statement: " + checkEachFileIsThere.toString());
      ResultSet resultSet = checkEachFileIsThere.executeQuery();
      
      final HashSet<FileInformation> hardDriveFiles = new HashSet<>(filesInHardDrive);
      while (resultSet.next()) {
        hardDriveFiles.remove(
          new FileInformation(
            resultSet.getString("FilePath"),
            resultSet.getString("FileHash")));
      }
      logger.debug("Returning: " + hardDriveFiles.toString());
      return new LinkedList<>(hardDriveFiles);
    } catch (SQLException e) {
      logger.error(
          "Checking for a batch of files in the database, query template: " + allComputerFileCheck,
          e);
    } finally {
      ConnectionStorage.close();
    }
    
    return new LinkedList<>();
  }
}
