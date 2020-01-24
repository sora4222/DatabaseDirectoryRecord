package com.sora4222.database;

import com.sora4222.database.configuration.ComputerProperties;
import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.configuration.UtilityForConfig;
import com.sora4222.database.connectors.ConnectionStorage;
import com.sora4222.database.connectors.UtilityForConnector;
import com.sora4222.database.setup.processors.ConcurrentQueues;
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
  private static final String LOCATION_OF_ROOT_ONE_ROOT_TWO_EXCLUDE = "src/test/resources/root1AndRoot2Exclude.json";
  
  Connection connection;
  File temporaryFile = new File("");
  
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
    connection.prepareStatement("DELETE FROM directory_records").executeUpdate();
    connection.prepareStatement("DELETE FROM file_paths").executeUpdate();
  }
  
  @AfterEach
  public void Disconnect() throws SQLException {
    ConnectionStorage.close();
  }
  
  @AfterEach
  public void deleteFile() {
    if (temporaryFile.exists() && temporaryFile.isFile())
      temporaryFile.delete();
  }
  
  @AfterEach
  @BeforeEach
  public void emptyQueues() {
    ConcurrentQueues.filesToUpload.clear();
    ConcurrentQueues.filesToQuery.clear();
    ConcurrentQueues.visitedDirectoriesQueue.clear();
  }
  
  private ResultSet getDatabaseContents() throws SQLException {
    connection = UtilityForConnector.getOrInitializeConnection();
    Statement stmt = connection.createStatement();
    return stmt.executeQuery("SELECT directory_records.FileId as FileId, FileHash, ComputerId, DatabaseRowCreationTime, file_paths.AbsoluteFilePath as FilePath " +
      "FROM `directory_records` " +
      "INNER JOIN file_paths ON directory_records.FileId = file_paths.FileId " +
      "WHERE ComputerId = " + ComputerProperties.computerNameId.get().toString());
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
  public void ChangesInScannedFolderWillBeRegisteredAndPutInDatabase() throws IOException, SQLException {
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
    } while (filePaths.contains(temporaryFile.getAbsolutePath().replace("\\", "/"))
        && i++ < maxRetries);
    
    Assertions.assertFalse(filePaths.contains(temporaryFile.getAbsolutePath().replace("\\", "/")),
        "No more retries to test whether the database has deleted the files");
  }
  
  @Test
  public void regularExpressionsAreFilteredOnSetup() throws IOException, SQLException {
    UtilityForConfig.clearConfig();
    System.setProperty("config", LOCATION_OF_ROOT_ONE_ROOT_TWO_EXCLUDE);
    
    // Make a file
    temporaryFile = new File("src/test/resources/root1/sharedFileb.txt");
    FileWriter writer = new FileWriter(temporaryFile);
    writer.write(UUID.randomUUID().toString());
    writer.close();
    
    DirectoryRecorder.setupScanning();
    
    
    // Shouldn't contain the shared files except the one I just put in.
    boolean containsSharedFileb = false;
    boolean innerFile1Contained = false;
    ResultSet databaseContents = getDatabaseContents();
    
    while (databaseContents.next()) {
      String file = databaseContents.getString("FilePath");
      Assertions.assertFalse(file.contains("sharedFile1.txt"));
      Assertions.assertFalse(file.contains("sharedFile2.txt"));
      Assertions.assertFalse(file.contains("aSecondaryFile.txt"));
      if (file.contains("sharedFileb.txt"))
        containsSharedFileb = true;
      if (file.contains("innerfile1.txt"))
        innerFile1Contained = true;
    }
    Assertions.assertTrue(containsSharedFileb,
        "The sharedFileb.txt was excluded from the database entries. This shouldn't have been excluded.");
    Assertions.assertTrue(innerFile1Contained, "The inner file being contained shouldn't have changed.");
    
    Assertions.assertTrue(temporaryFile.delete());
    
  }
  
  @Test
  public void regularExpressionsAreFilteredOnActivity() throws IOException, SQLException, InterruptedException {
    UtilityForConfig.clearConfig();
    System.setProperty("config", LOCATION_OF_ROOT_ONE_ROOT_TWO_EXCLUDE);
    System.out.println("Connection status: " + ConnectionStorage.getConnection().isClosed());
    DirectoryRecorder.setupScanning();
    // Make a file matching a regex
    temporaryFile = new File("src/test/resources/root1/sharedFile3AMassOfString.txt");
    FileWriter writer = new FileWriter(temporaryFile);
    writer.write(UUID.randomUUID().toString());
    writer.close();
    
    Thread.sleep(3000);
    DirectoryRecorder.scanOnce();
    
    // Shouldn't contain the shared files except the one I just put in.
    ResultSet databaseContents = getDatabaseContents();
    
    while (databaseContents.next()) {
      String file = databaseContents.getString("FilePath");
      Assertions.assertFalse(file.contains("sharedFile3.txt"));
    }
    Assertions.assertTrue(temporaryFile.delete());
  }
}
