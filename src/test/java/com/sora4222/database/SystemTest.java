package com.sora4222.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SystemTest {
    private static final String LOCATION_OF_ROOT_ONE_ROOT_TWO = "src/test/resources/root1AndRoot2.json";

    @BeforeEach
    public void setupDatabase() throws SQLException {
        Connection connection = DriverManager.getConnection(
            System.getProperty("jdbcConnectionUrl") + "?serverTimezone=Australia/Melbourne",
            System.getProperty("databaseUsername"), System.getProperty("databasePassword"));

        connection.prepareStatement("DELETE FROM " + System.getProperty("dataTable")).executeUpdate();
        connection.close();
    }

    @Test
    public void RunScannerOnceOnRootOneAndTwoJson() {
        System.setProperty("config", LOCATION_OF_ROOT_ONE_ROOT_TWO);

        DirectoryRecorder.setupScanning();
        DirectoryRecorder.scan();
    }
}
