package com.sora4222.database.connectors;

import com.sora4222.database.DatabaseWrapper;
import com.sora4222.file.FileInformation;
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
    // TODO: Backoff period
    while (true) {
      logger.info("Establishing connection to MySQL database.");
      // Establish a connection
      try {
        connect = DriverManager.getConnection(config.getJdbcConnectionUrl(),
                config.getDatabaseUsername(),
                config.getDatabasePassword());
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
      PreparedStatement selectStatement =
          connect.prepareStatement("SELECT " +  config.getDataTable() + " FROM ? WHERE FileName=? OR FileHash=?");

      selectStatement.setString(1, config.getDataTable());
      selectStatement.setString(1, fileInformation.getFileName());
      selectStatement.setString(2, fileInformation.getFileHash());
  
      ResultSet resultSet = selectStatement.executeQuery();
  
      fileInformations = convertResultSetToFileInformationList(resultSet);
    } catch (SQLException e) {
      logger.error("An SQL error was thrown checking for the file: " + fileInformation.getFullLocation(), e);
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
    String query = "insert into " +  config.getDataTable() + "(FileName, FilePath, FileHash, ComputerName) " +
            "VALUES (?, ?, ?, ?)";
    try {
      PreparedStatement insertQuery = connect.prepareStatement(query);
      insertQuery.setString(1, infoToSend.getFileName());
      insertQuery.setString(2, infoToSend.getFullLocation().toAbsolutePath().toString());
      insertQuery.setString(3, infoToSend.getFileHash());
      insertQuery.setString(4, infoToSend.getComputerName());

      logger.info("Insert results: " + insertQuery.executeUpdate());
      connect.commit();
    } catch (SQLException e) {
      logger.error(String.format("An exception has occurred running the query '%s' with table: %s, Name: %s, Path: %s, FileHash: %s, ComputerName: %s",
              query,
              config.getDataTable(),
              infoToSend.getFileName(),
              infoToSend.getFullLocation(),
              infoToSend.getFileHash(),
              infoToSend.getComputerName()), e);
      return false;
    }

    return true;
  }

  public boolean close() {
    try {
      connect.close();
    } catch (SQLException e) {
      logger.info("The connection has failed to close", e);
      return false;
    }
    return true;
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
