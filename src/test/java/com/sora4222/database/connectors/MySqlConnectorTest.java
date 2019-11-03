package com.sora4222.database.connectors;

import com.sora4222.database.configuration.ComputerProperties;
import com.sora4222.database.configuration.ConfigurationManager;
import com.sora4222.database.configuration.UtilityForConfig;
import com.sora4222.file.FileInformation;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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
    }
    
    @BeforeEach
    public void setup () throws SQLException {
        ConfigurationManager.getConfiguration().setRootLocations(
            Arrays.asList("/location/1", "/location/that/I/endWith/forwardSlash/"));
        
        connector = ConnectionStorage.getConnection();
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
        Inserter.insertFilesIntoDatabase(testFiles);
        
        Assertions.assertEquals(testFiles, assertNumberItemsEqual(1));
    }

    @Test
    public void InsertAndSelect () throws SQLException {
        FileInformation file = new FileInformation(location, fileHash);
        testFiles.add(file);
        Inserter.insertFilesIntoDatabase(testFiles);
        
        Assertions.assertEquals(testFiles, assertNumberItemsEqual(1));
    }

    @Test
    public void InsertThenDelete () throws SQLException {
        FileInformation fileToInsertAndDelete = new FileInformation(location, fileHash);
        testFiles.add(fileToInsertAndDelete);
        Inserter.insertFilesIntoDatabase(testFiles);
        
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
          .execute();
        Assertions.assertEquals(1,
          DatabaseQuery
            .allFilesInBothComputerAndDatabase(Collections.singletonList(new FileInformation("/dir/file.txt", "fakeComputer", "1234asdf")))
            .size());
    
        connector = ConnectionStorage.getConnection();
        connector
            .prepareStatement("INSERT INTO directory_records_test (ComputerName, FilePath, FileHash) VALUES ('fakeComputer', '/dir/file2.txt', '123asdf')")
            .execute();
        
        Assertions.assertEquals(1,
            DatabaseQuery
                .allFilesInBothComputerAndDatabase(Collections.singletonList(new FileInformation("/dir/file.txt", "fakeComputer", "1234asdf")))
                .size());
        
        connector = ConnectionStorage.getConnection();
        connector
            .prepareStatement("INSERT INTO directory_records_test (ComputerName, FilePath, FileHash) VALUES ('moreFakeComputer', '/dir/file2.txt', '123asdf')")
            .execute();
    
        Assertions.assertEquals(1,
            DatabaseQuery
                .allFilesInBothComputerAndDatabase(Collections.singletonList(new FileInformation("/dir/file.txt", "fakeComputer", "1234asdf")))
                .size());
    }
    
    @Test
    void allQueriesAcceptEmptyList() {
        Assertions.assertEquals(Collections.EMPTY_LIST, DatabaseQuery.allFilesInBothComputerAndDatabase(Collections.emptyList()));
        Inserter.insertFilesIntoDatabase(Collections.emptyList());
        Updater.sendUpdatesToDatabase(Collections.emptyList());
        Deleter.sendDeletesToDatabase(Collections.emptyList());
    
    }
}
