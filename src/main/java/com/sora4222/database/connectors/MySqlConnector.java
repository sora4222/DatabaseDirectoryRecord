package com.sora4222.database.connectors;

import com.sora4222.database.DatabaseWrapper;
import com.sora4222.database.FileInformation;
import com.sora4222.database.configuration.Config;
import com.sora4222.database.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class MySqlConnector implements DatabaseWrapper {
  private static final Logger logger = LogManager.getLogger();
  private static final Config config = Configuration.getConfiguration();
  private Connection connect;
  
  public MySqlConnector() {
    establishConnection();
  }
  
  private void establishConnection() {
    while (true) {
      logger.info("Establishing connection to MySQL database.");
      // Establish a connection
      try {
        connect = DriverManager.getConnection(config.getJdbcConnectionUrl());
        break;
      } catch (SQLException e) {
        logger.error(String.format("The MySQL database cannot be connected to. URI: %s\nError: %s",
            config.getJdbcConnectionUrl(), e.getMessage()));
      }
    }
  }
  
  @Override
  public List<FileInformation> checkForFile(final FileInformation fileInformation) {
    List<FileInformation> fileInformations = new LinkedList<>();
    try {
      // FileName,
      PreparedStatement selectStatement =
          connect.prepareStatement("SELECT * FROM FilesDatabase WHERE FileName=? OR FileHash=?");
      
      selectStatement.setString(1, fileInformation.getFileName());
      selectStatement.setString(1, fileInformation.getFileHash());
  
      ResultSet resultSet = selectStatement.executeQuery();
  
      fileInformations = convertResultSetToFileInformationList(resultSet);
    } catch (SQLException e) {
      logger.error("An SQL error was thrown checking for file: " + fileInformation.getFullLocation(), e);
    }
    return fileInformations;
  }
  
  private List<FileInformation> convertResultSetToFileInformationList(final ResultSet resultSet) throws SQLException {
    final List<FileInformation> fileInformations = new LinkedList<>();
    while (resultSet.next()){
      fileInformations.add(new FileInformation(
          resultSet.getString("FileName"),
          resultSet.getString("FileLocation"),
          resultSet.getString("ComputerName"),
          resultSet.getString("FileHash")));
    }
    return fileInformations;
  }
  
  @Override
  public boolean insertFile(final FileInformation infoToSend) {
    return false;
  }
  
  @Override
  public boolean deleteFileRow(final FileInformation fileToDelete) {
    return false;
  }
  
  @Override
  public boolean updateFileRow(final FileInformation fileToUpdate) {
    return false;
  }
  
  @Override
  public boolean currentComputerName(final String computerName) {
    return false;
  }
}
