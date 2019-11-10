package com.sora4222.database;

import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.configuration.UtilityForConfig;
import com.sora4222.database.connectors.UtilityForConnector;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

public class SystemTest {
  private static final String LOCATION_OF_ROOT_ONE_ROOT_TWO = "src/test/resources/root1AndRoot2.json";
  private static String dataTable = ConfigurationManager.getConfiguration().getDataTable();
  
  Connection connection;
  
  @BeforeAll
  public static void resetConfig() {
    UtilityForConfig.clearConfig();
  }
  
  @SuppressWarnings("SqlWithoutWhere")
  @BeforeEach
  public void setupDatabase() throws SQLException {
    connection = UtilityForConnector.getOrInitializeConnection();
    
    connection.prepareStatement("DELETE FROM " + dataTable).executeUpdate();
  }
  
  @AfterEach
  public void Disconnect() throws SQLException {
    connection.close();
  }
  
  @Test
  public void RunScannerSetupRootOneAndTwo() throws SQLException {
    System.setProperty("config", LOCATION_OF_ROOT_ONE_ROOT_TWO);
    UtilityForConfig.clearConfig();
    
    ConfigurationManager
        .getConfiguration()
        .setRootLocations(Arrays.asList("src/test/resources/root1", "src/test/resources/root2/"));
    
    DirectoryRecorder.setupScanning();
  
    connection = UtilityForConnector.getOrInitializeConnection();
    Statement stmt = connection.createStatement();
    ResultSet files = stmt.executeQuery("SELECT * FROM " + dataTable);
    
    int count = 0;
    while (files.next()) {
      count++;
    }
    Assertions.assertEquals(6, count);
  }
  
  @Test
  public void ChangesInScannedFolderWillBeRegisteredAndPutInDatabase() {
    DirectoryRecorder.setupScanning();
    DirectoryRecorder.scanOnce();
  }
}
