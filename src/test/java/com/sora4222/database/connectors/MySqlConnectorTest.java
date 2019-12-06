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
import java.util.stream.Collectors;

public class MySqlConnectorTest {

    Connection connector;
    String name;
    Path location;
    String fileHash;
    String computerName;

    List<FileInformation> testFiles = new LinkedList<>();
    
    @BeforeAll
    public static void clearConfiguration() {
        UtilityForConfig.clearConfig();
        System.clearProperty("config");
    }
    
    @BeforeEach
    public void setup () throws SQLException {
        ConfigurationManager.getConfiguration().setRootLocations(
            Arrays.asList("/location/1", "/location/that/I/endWith/forwardSlash/"));
    
        connector = UtilityForConnector.getOrInitializeConnection();
        connector.prepareStatement("DELETE FROM directory_records_test").executeUpdate();
        
        testFiles = new LinkedList<>();
        createNewFakeFile();
    }


    public void createNewFakeFile () {
        name = UUID.randomUUID().toString() + ".txt";
        location = Paths.get(name);
        fileHash = UUID.randomUUID().toString();
        computerName = UUID.randomUUID().toString();
    }

    @AfterEach
    public void teardownConnector () {
        ConnectionStorage.close();
    }

    @Test
    public void InsertInformation () throws SQLException {
        testFiles.add(new FileInformation(location, fileHash));
        Inserter.insertRecordIntoDatabase(testFiles);
        
        Assertions.assertEquals(testFiles, assertNumberItemsEqual(1));
    }

    @Test
    public void InsertAndSelect () throws SQLException {
        FileInformation file = new FileInformation(location, fileHash);
        testFiles.add(file);
        Inserter.insertRecordIntoDatabase(testFiles);
        
        Assertions.assertEquals(testFiles, assertNumberItemsEqual(1));
    }

    @Test
    public void InsertThenDelete () throws SQLException {
        FileInformation fileToInsertAndDelete = new FileInformation(location, fileHash);
        testFiles.add(fileToInsertAndDelete);
        Inserter.insertRecordIntoDatabase(testFiles);
        
        Deleter.sendDeletesToDatabase(testFiles);
        int expected = 0;
        assertNumberItemsEqual(expected);
    }
    
    private List<FileInformation> assertNumberItemsEqual (final int expected) throws SQLException {
        connector = ConnectionStorage.getConnection();
        ResultSet found = connector.prepareStatement("SELECT * FROM directory_records_test").executeQuery();
        int i = 0;
        List<FileInformation> files = new LinkedList<>();
        while (found.next()) {
            files.add(new FileInformation(found.getString("FilePath"), ComputerProperties.computerName.get(), found.getString("FileHash")));
            i++;
        }
        Assertions.assertEquals(expected, i, found.toString());
        return files;
    }
    
    @Test
    public void TestSelectAllFilesQuery() throws SQLException {
        connector
          .prepareStatement("INSERT INTO directory_records_test (ComputerName, FilePath, FileHash) VALUES ('fakeComputer', '/dir/file.txt', '1234asdf')")
            .executeUpdate();
        Assertions.assertEquals(1,
          DatabaseQuery
              .allFilesAlreadyInBothComputerAndDatabase(Collections.singletonList(new FileInformation("/dir/file.txt", "fakeComputer", "1234asdf")))
            .size());
    
        connector = ConnectionStorage.getConnection();
        connector
            .prepareStatement("INSERT INTO directory_records_test (ComputerName, FilePath, FileHash) VALUES ('fakeComputer', '/dir/file2.txt', '123asdf')")
            .executeUpdate();
        
        Assertions.assertEquals(1,
            DatabaseQuery
                .allFilesAlreadyInBothComputerAndDatabase(Collections.singletonList(new FileInformation("/dir/file.txt", "fakeComputer", "1234asdf")))
                .size());
        
        connector = ConnectionStorage.getConnection();
        connector
            .prepareStatement("INSERT INTO directory_records_test (ComputerName, FilePath, FileHash) VALUES ('moreFakeComputer', '/dir/file2.txt', '123asdf')")
            .executeUpdate();
    
        Assertions.assertEquals(1,
            DatabaseQuery
                .allFilesAlreadyInBothComputerAndDatabase(Collections.singletonList(new FileInformation("/dir/file.txt", "fakeComputer", "1234asdf")))
                .size());
    }
    
    @Test
    void allQueriesAcceptEmptyList() {
        Assertions.assertEquals(Collections.EMPTY_LIST, DatabaseQuery.allFilesAlreadyInBothComputerAndDatabase(Collections.emptyList()));
        Inserter.insertRecordIntoDatabase(Collections.emptyList());
        Updater.sendUpdatesToDatabase(Collections.emptyList());
        Deleter.sendDeletesToDatabase(Collections.emptyList());
    }
    
    @Test
    void testDatabaseEntriesReturnsNothingWhenTheFilesAreNotInDatabase() {
        Path rootOneDirectory = Paths.get("src/test/resources/root1/");
        DatabaseEntries entries = new DatabaseEntries(rootOneDirectory);
        Assertions.assertEquals(0, entries.databaseRecordCount());
        
        List<FileInformation> stream = entries.getComputersFilesFromDatabase()
            .limit(entries.databaseRecordCount()).collect(Collectors.toList());
        Assertions.assertTrue(stream.isEmpty());
    }
    
    @Test
    void testDataBaseEntriesWillReturnARowWithTheSrcRootInItButNoOtherRow() throws SQLException {
        Path rootOneDirectory = Paths.get("src/test/resources/root1/");
        
        // Insert something that exists
        PreparedStatement statement = connector
            .prepareStatement("INSERT INTO directory_records_test (ComputerName, FilePath, FileHash) VALUES (?, ?, '123asdf')");
        statement.setString(1, ComputerProperties.computerName.get());
        statement.setString(2, rootOneDirectory.resolve("sharedFile1.txt").toAbsolutePath().toString().replace("\\", "/"));
        statement.executeUpdate();
        
        Path rootTwoDirectory = Paths.get("src/test/resources/root2/");
        // Insert something that is in a different directory
        statement = connector
            .prepareStatement("INSERT INTO directory_records_test (ComputerName, FilePath, FileHash) VALUES (?, ?, '125asdf')");
        statement.setString(1, ComputerProperties.computerName.get());
        statement.setString(2, rootTwoDirectory.resolve("sharedFile2.txt").toAbsolutePath().toString().replace("\\", "/"));
        statement.executeUpdate();
        
        DatabaseEntries entries = new DatabaseEntries(rootOneDirectory);
        Assertions.assertEquals(1, entries.databaseRecordCount());
        
        List<FileInformation> stream = entries.getComputersFilesFromDatabase()
            .limit(entries.databaseRecordCount()).collect(Collectors.toList());
        Assertions.assertEquals(1, stream.size());
    }
}
