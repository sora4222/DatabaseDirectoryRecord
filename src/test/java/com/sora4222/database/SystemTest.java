package com.sora4222.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;

public class SystemTest {
    private static final String LOCATION_OF_ROOT_ONE_ROOT_TWO = "src/test/resources/root1AndRoot2.json";

    Connection connection;
    private static String dataTable = System.getProperty("dataTable");
    @BeforeEach
    public void setupDatabase() throws SQLException {
         connection = DriverManager.getConnection(
            System.getProperty("jdbcConnectionUrl") + "?serverTimezone=Australia/Melbourne",
            System.getProperty("databaseUsername"), System.getProperty("databasePassword"));

        connection.prepareStatement("DELETE FROM " + dataTable).executeUpdate();
    }

    @AfterEach
    public void Disconnect() throws SQLException {
        connection.close();
    }

    @Test
    public void RunScannerOnceOnRootOneAndTwoJson() throws SQLException {
        System.setProperty("config", LOCATION_OF_ROOT_ONE_ROOT_TWO);

        DirectoryRecorder.setupScanning();
        DirectoryRecorder.scan();

        ResultSet files = connection.prepareStatement("SELECT * FROM " + dataTable).executeQuery();
        int count = 0;
        while (files.next()) {
            count++;
        }
        Assertions.assertEquals(6, count);
    }
}
