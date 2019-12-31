package com.sora4222.database.connectors;

import com.sora4222.database.configuration.ComputerProperties;
import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.configuration.UtilityForConfig;
import com.sora4222.file.FileInformation;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MySqlConnectorTest {
  
  Connection connector;
  Path location = Paths.get("");
  String fileHash;
  
  List<FileInformation> testFiles = new LinkedList<>();
  
  @BeforeAll
  public static void clearConfiguration() {
    UtilityForConfig.clearConfig();
    System.clearProperty("config");
  }
  
  @BeforeEach
  public void setup() throws SQLException {
    ConfigurationManager.getConfiguration().setRootLocations(
        Arrays.asList("/location/1", "/location/that/I/endWith/forwardSlash/"));
    
    connector = UtilityForConnector.getOrInitializeConnection();
    connector.prepareStatement("DELETE FROM `directory_records`").executeUpdate();
    
    fileHash = UUID.randomUUID().toString();
    testFiles = new LinkedList<>();
  }
  
  @AfterEach
  public void teardownConnector() {
    ConnectionStorage.close();
  }
  
  public static List<FileInformation> assertNumberItemsEqual(final int expected) throws SQLException {
    Connection connectorStatic = ConnectionStorage.getConnection();
    ResultSet found = connectorStatic.prepareStatement(
        "SELECT file_paths.FilePath as filePath, directory_records.FileHash as fileHash " +
            "FROM `directory_records` " +
            "INNER JOIN file_paths ON directory_records.FileId = file_paths.FileId").executeQuery();
    int i = 0;
    List<FileInformation> files = new LinkedList<>();
    while (found.next()) {
      files.add(new FileInformation(found.getString("filePath"), found.getString("fileHash")));
      i++;
    }
    Assertions.assertEquals(expected, i, found.toString());
    return files;
  }
  
  @Test
  public void InsertInformation() throws SQLException {
    testFiles.add(new FileInformation(Paths.get("fakeFile." + UUID.randomUUID().toString()), fileHash));
    Inserter.insertRecordIntoDatabase(testFiles);
    
    Assertions.assertEquals(testFiles, assertNumberItemsEqual(1));
  }
  
  @Test
  public void InsertAndSelect() throws SQLException {
    FileInformation file = new FileInformation(location, fileHash);
    testFiles.add(file);
    Inserter.insertRecordIntoDatabase(testFiles);
    
    Assertions.assertEquals(testFiles, assertNumberItemsEqual(1));
  }
  
  @Test
  public void InsertThenDelete() throws SQLException {
    FileInformation fileToInsertAndDelete = new FileInformation(location, fileHash);
    testFiles.add(fileToInsertAndDelete);
    Inserter.insertRecordIntoDatabase(testFiles);
    
    Deleter.sendDeletesToDatabase(testFiles);
    int expected = 0;
    assertNumberItemsEqual(expected);
  }
  
  private int InsertFileReturnId(String fileName) throws SQLException {
    connector = ConnectionStorage.getConnection();
    PreparedStatement stmt = connector.prepareStatement("INSERT INTO file_paths (FilePath) VALUES (?)");
    stmt.setString(1, fileName);
    stmt.execute();
    
    stmt = connector.prepareStatement("SELECT FileId FROM file_paths WHERE FilePath = ?");
    stmt.setString(1, fileName);
    ResultSet set = stmt.executeQuery();
    
    set.next();
    return set.getInt("FileId");
  }
  
  @Test
  public void TestSelectAllFilesQuery() throws SQLException {
    // Insert a single file, give it a list of files as though they were on this computer. Expect just a single out.
    String fileName1 = "/dir/test_file." + UUID.randomUUID().toString();
    insertFileWithComputerAndPath(fileName1, "1234asdf");
    Assertions.assertEquals(1,
        DatabaseQuery
            .queryTheDatabaseForFiles(Collections.singletonList(new FileInformation(fileName1, "1234asdf")))
            .size());
    
    String fileName2 = "/dir/test_file_2." + UUID.randomUUID().toString();
    insertFileWithComputerAndPath(fileName2, "5678asdf");
    
    Assertions.assertEquals(1,
        DatabaseQuery
            .queryTheDatabaseForFiles(
                Collections.singletonList(new FileInformation(fileName2, "5678asdf"))).size());
    
    List<FileInformation> bothFiles = new ArrayList<>();
    bothFiles.add(new FileInformation(fileName2, "5678asdf"));
    bothFiles.add(new FileInformation(fileName1, "1234asdf"));
    Assertions.assertEquals(2,
        DatabaseQuery
            .queryTheDatabaseForFiles(bothFiles).size());
  }
  
  private void insertFileWithComputerAndPath(String fileName, String hash) throws SQLException {
    int fileId1 = InsertFileReturnId(fileName);
    
    // Insert a single file with random name
    PreparedStatement stmt1;
    stmt1 = connector
        .prepareStatement(
            "INSERT INTO `directory_records` (ComputerId, FileId, FileHash) " +
                "VALUES (?, ?, ?)");
    stmt1.setInt(1, ComputerProperties.computerNameId.get());
    stmt1.setInt(2, fileId1);
    stmt1.setString(3, hash);
    stmt1.executeUpdate();
  }
  
  @Test
  void allQueriesAcceptEmptyList() {
    Assertions.assertEquals(Collections.EMPTY_LIST, DatabaseQuery.queryTheDatabaseForFiles(Collections.emptyList()));
    Inserter.insertRecordIntoDatabase(Collections.emptyList());
    Updater.sendUpdatesToDatabase(Collections.emptyList());
    Deleter.sendDeletesToDatabase(Collections.emptyList());
  }
  
  @Test
  void testDatabaseEntriesReturnsNothingWhenTheFilesAreNotInDatabase() {
    Path rootOneDirectory = Paths.get("src/test/resources/root1/");
    DatabaseEntries entries = new DatabaseEntries();
    
    List<FileInformation> stream = entries.getFiles();
    Assertions.assertTrue(stream.isEmpty());
  }
  
  //TODO: Create a  test for databaseEntries.
}
