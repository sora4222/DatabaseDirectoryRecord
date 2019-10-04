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
import java.util.concurrent.TimeUnit;

@Deprecated
public class MySqlConnector implements DatabaseWrapper {
    private static final Logger logger = LogManager.getLogger();
    private static final Config config = Configuration.getConfiguration();
    private static Connection connect;

    public MySqlConnector () {
        establishConnection();
    }

    private void establishConnection () {
        int numberOfConnectionAttempts = 0;
        // TODO: Backoff period
        while (true) {
            numberOfConnectionAttempts += 1;
            backOffConnectionAttempts(numberOfConnectionAttempts);
            logger.info("Establishing connection to MySQL database.");
            // Establish a connection
            try {
                connect = DriverManager
                    .getConnection(
                        config.getJdbcConnectionUrl() + "?serverTimezone=Australia/Melbourne",
                        config.getDatabaseUsername(),
                        config.getDatabasePassword());
                break;
            } catch (SQLException e) {
                logger.error(String.format("The MySQL database cannot be connected to URI: %s\nError: %s",
                    config.getJdbcConnectionUrl(), e.getMessage()));
            }
        }
    }

    private void backOffConnectionAttempts (int numberOfConnectionAttempts) {
        if (numberOfConnectionAttempts > 1) {
            try {
                if (numberOfConnectionAttempts <= 10)
                    TimeUnit.MILLISECONDS.sleep(100 * (long) Math.exp(numberOfConnectionAttempts));
                else
                    TimeUnit.HOURS.sleep(1);
            } catch (InterruptedException e) {
                logger.error("An interrupted exception occurred whilst backing off the number of connection attempts.", e);
            }
        }
    }

    @Override
    public List<FileInformation> checkForFile (final FileInformation fileInformation) {
        List<FileInformation> fileInformations = new LinkedList<>();
        try {
            assert fileInformation.getComputerName() != null;
            checkAndHandleDeadConnection();
            PreparedStatement selectStatement =
                connect.prepareStatement("SELECT * FROM `" + config.getDataTable() +
                    "` WHERE (FileName=? OR FileHash=? OR FilePath=?) AND ComputerName=?");

            selectStatement.setString(1, fileInformation.getFileName());
            selectStatement.setString(2, fileInformation.getFileHash());
            selectStatement.setString(3, fileInformation.getFullLocation().toString());
            selectStatement.setString(4, fileInformation.getComputerName());


            ResultSet resultSet = selectStatement.executeQuery();

            fileInformations = convertResultSetToFileInformationList(resultSet);
        } catch (SQLException e) {

            logger.error("An SQL error was thrown checking for the file: " + fileInformation.getFullLocation(), e);
        }
        return fileInformations;
    }

    private void checkAndHandleDeadConnection () throws SQLException {
        if (connect.isValid(35)) {
            try {
                connect = DriverManager.getConnection(config.getJdbcConnectionUrl() + "?serverTimezone=Australia/Melbourne",
                    config.getDatabaseUsername(),
                    config.getDatabasePassword());
            } catch (SQLException e) {
                logger.error("The database whilst attempting to reconnect the database has failed.");
                throw e;
            }
        }
    }

    private List<FileInformation> convertResultSetToFileInformationList (final ResultSet resultSet) throws SQLException {
        final List<FileInformation> fileInformations = new LinkedList<>();
        while (resultSet.next()) {
            fileInformations.add(new FileInformation(
                resultSet.getString("FileName"),
                resultSet.getString("FilePath"),
                resultSet.getString("ComputerName"),
                resultSet.getString("FileHash")));
        }
        return fileInformations;
    }

    @Override
    public boolean insertFile (final FileInformation infoToSend) {
        String query = "insert into `" + config.getDataTable() + "` (FileName, FilePath, FileHash, ComputerName) " +
            "VALUES (?, ?, ?, ?)";
        try {
            checkAndHandleDeadConnection();

            PreparedStatement insertQuery = connect.prepareStatement(query);
            insertQuery.setString(1, infoToSend.getFileName());
            insertQuery.setString(2, infoToSend.getFullLocation().toString());
            insertQuery.setString(3, infoToSend.getFileHash());
            insertQuery.setString(4, infoToSend.getComputerName());

            insertQuery.executeUpdate();
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

    public boolean close () {
        try {
            connect.close();
        } catch (SQLException e) {
            logger.info("The connection has failed to close", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteFileRow (final FileInformation fileToDelete) {
        try {
            checkAndHandleDeadConnection();
            PreparedStatement deleteStatement =
                connect.prepareStatement("DELETE FROM `" + config.getDataTable() + "` WHERE ComputerName=? AND FilePath=?");
            deleteStatement.setString(1, fileToDelete.getComputerName());
            deleteStatement.setString(2, fileToDelete.getFullLocation().toString());

            deleteStatement.executeUpdate();

        } catch (SQLException e) {
            logger.error("An error has occurred deleting a row", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean updateFileRow (final FileInformation fileToUpdate) {
        try {
            checkAndHandleDeadConnection();
            PreparedStatement updateStatement = connect.prepareStatement("UPDATE `" + config.getDataTable() +
                "` SET FileHash=? WHERE ComputerName=?  AND FileName=?");

            updateStatement.setString(1, fileToUpdate.getFileHash());
            updateStatement.setString(2, fileToUpdate.getComputerName());
            updateStatement.setString(3, fileToUpdate.getFileName());

            updateStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("An error has occurred updating a row.", e);
            return false;
        }
        return true;
    }
}
