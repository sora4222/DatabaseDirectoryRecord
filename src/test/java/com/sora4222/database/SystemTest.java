package com.sora4222.database;

import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.configuration.UtilityForConfig;
import com.sora4222.database.connectors.UtilityForConnector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class SystemTest {
  private static final String LOCATION_OF_ROOT_ONE_ROOT_TWO = "src/test/resources/root1AndRoot2.json";
  private static String dataTable = ConfigurationManager.getConfiguration().getDataTable();
  
  Connection connection;
  File temporaryFile = new File("");
  Logger logger = LogManager.getLogger();
  
  @BeforeAll
  public static void resetConfig() {
    System.clearProperty("config");
    System.setProperty("config", LOCATION_OF_ROOT_ONE_ROOT_TWO);
    ConfigurationManager.getConfiguration();
    UtilityForConfig.clearConfig();
  }
  
  @SuppressWarnings("SqlWithoutWhere")
  @BeforeEach
  public void setupDatabase() throws SQLException {
    System.clearProperty("config");
    UtilityForConfig.clearConfig();
    
    connection = UtilityForConnector.getOrInitializeConnection();
    connection.prepareStatement("DELETE FROM " + dataTable).executeUpdate();
  }
  
  @AfterEach
  public void Disconnect() throws SQLException {
    connection.close();
  }
  
  @AfterEach
  public void deleteFile() {
    if (temporaryFile.exists() && temporaryFile.isFile())
      temporaryFile.delete();
  }
  
  private ResultSet getDatabaseContents() throws SQLException {
    connection = UtilityForConnector.getOrInitializeConnection();
    Statement stmt = connection.createStatement();
    return stmt.executeQuery("SELECT * FROM " + dataTable);
  }
  
  @Test
  public void RunScannerSetupRootOneAndTwo() throws SQLException {
    System.setProperty("config", LOCATION_OF_ROOT_ONE_ROOT_TWO);
    UtilityForConfig.clearConfig();
    
    ConfigurationManager
        .getConfiguration()
        .setRootLocations(Arrays.asList("src/test/resources/root1", "src/test/resources/root2/"));
    
    DirectoryRecorder.setupScanning();
  
    ResultSet files = getDatabaseContents();
    int count = 0;
    while (files.next()) {
      count++;
    }
    Assertions.assertEquals(6, count);
  }
  
  @Test
  public void ChangesInScannedFolderWillBeRegisteredAndPutInDatabase() throws IOException, SQLException, InterruptedException {
    System.setProperty("config", LOCATION_OF_ROOT_ONE_ROOT_TWO);
    UtilityForConfig.clearConfig();
    
    DirectoryRecorder.setupScanning();
  
    String randomName = UUID.randomUUID().toString() + ".txt";
  
    // Make a file
    temporaryFile = new File("src/test/resources/root1/" + randomName);
    FileWriter writer = new FileWriter(temporaryFile);
    writer.write(UUID.randomUUID().toString());
    writer.close();
  
    Assertions.assertTrue(temporaryFile.exists());
    
    DirectoryRecorder.scanOnce();
  
    // Check the file is in the database
    ResultSet databaseContents = getDatabaseContents();
    List<String> filePaths = new LinkedList<>();
    while (databaseContents.next()) {
      filePaths.add(databaseContents.getString("FilePath"));
    }
    Assertions.assertTrue(filePaths.contains(temporaryFile.getAbsolutePath().replace("\\", "/")));
  
    // Delete the file
    Assertions.assertTrue(temporaryFile.delete());
  
    int i = 0;
    int maxRetries = 10;
    do {
      DirectoryRecorder.scanOnce();
      databaseContents = getDatabaseContents();
    
      filePaths = new LinkedList<>();
      while (databaseContents.next()) {
        filePaths.add(databaseContents.getString("FilePath"));
      }
    } while (filePaths.contains(temporaryFile.getAbsolutePath().replace("\\", "/")) && i++ < maxRetries);
  
    Assertions.assertFalse(filePaths.contains(temporaryFile.getAbsolutePath().replace("\\", "/")),
        "No more retries to test whether the database has deleted the files");
  }
}
