package com.sora4222.database;

import com.sora4222.database.configuration.UtilityForConfig;
import com.sora4222.database.connectors.ConnectionStorage;
import com.sora4222.database.connectors.UtilityForConnector;
import org.junit.jupiter.api.*;

import java.sql.*;

public class SystemTest {
  private static final String LOCATION_OF_ROOT_ONE_ROOT_TWO = "src/test/resources/root1AndRoot2.json";
  
  Connection connection;
  private static String dataTable = System.getProperty("dataTable");
  
  @BeforeAll
  public static void resetConfig() {
    UtilityForConfig.clearConfig();
  }
  
  @BeforeEach
  public void setupDatabase() throws SQLException {
    connection = UtilityForConnector.getConnection();
    
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
    DirectoryRecorder.startScanning();
    
    ResultSet files = connection.prepareStatement("SELECT * FROM " + dataTable).executeQuery();
    int count = 0;
    while (files.next()) {
      count++;
    }
    Assertions.assertEquals(6, count);
  }
}
