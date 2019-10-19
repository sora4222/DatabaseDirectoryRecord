package com.sora4222.database.connectors;

import com.sora4222.database.configuration.ComputerProperties;
import com.sora4222.file.FileInformation;
import org.junit.jupiter.api.*;

import javax.xml.crypto.Data;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class MySqlConnectorTest {

    Connection connector;
    private static String LOCATION_OF_TEST_CONFIG_FAKE_VALUES = "src/test/resources/filledConfigFile.json";

    String name;
    Path location;
    String filehash;
    String computerName;

    @BeforeAll
    public static void systemProperties () {
        System.setProperty("config", LOCATION_OF_TEST_CONFIG_FAKE_VALUES);
    }
    List<FileInformation> testFiles = new LinkedList<>();

    @BeforeEach
    public void setup () throws SQLException {
        connector = ConnectionStorage.getConnection();
        connector.prepareStatement("DELETE FROM directory_records_test").executeUpdate();
        
        testFiles = new LinkedList<>();
        createNewFakeFile();
    }


    public void createNewFakeFile () {
        name = UUID.randomUUID().toString() + ".txt";
        location = Paths.get(name);
        filehash = UUID.randomUUID().toString();
        computerName = UUID.randomUUID().toString();
    }

    @AfterEach
    public void teardownConnector () {
        ConnectionStorage.close();
    }

    @Test
    public void InsertInformation () throws SQLException {
        testFiles.add(new FileInformation(location, filehash));
        Inserter.insertFilesIntoDatabase(testFiles);
        
        Assertions.assertEquals(testFiles, assertNumberItemsEqual(1));
    }

    @Test
    public void InsertAndSelect () throws SQLException {
        FileInformation file = new FileInformation(location, filehash);
        testFiles.add(file);
        Inserter.insertFilesIntoDatabase(testFiles);
        
        Assertions.assertEquals(testFiles, assertNumberItemsEqual(1));
    }

    @Test
    public void InsertThenDelete () throws SQLException {
        FileInformation fileToInsertAndDelete = new FileInformation(location, filehash);
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

//    @Test
//    public void UpdateFileHash () {
//        FileInformation fileToInsertThenUpdate = new FileInformation(location, filehash);
//        connector.insertFile(fileToInsertThenUpdate);
//
//        FileInformation updatedFile = new FileInformation(location, UUID.randomUUID().toString());
//        Assertions.assertTrue(connector.updateFileRow(updatedFile));
//        List<FileInformation> retrievedFiles = connector.checkForFile(updatedFile);
//        Assertions.assertTrue(retrievedFiles.contains(updatedFile), retrievedFiles.toString());
//    }
//
//    @Test
//    public void InsertTwoFilesDifferentHashSameName () {
//        FileInformation fileToInsertThenUpdate = new FileInformation(location, filehash);
//        connector.insertFile(fileToInsertThenUpdate);
//
//        FileInformation updatedFile = new FileInformation(location, UUID.randomUUID().toString());
//        Assertions.assertTrue(connector.updateFileRow(updatedFile));
//
//        List<FileInformation> retrievedFiles = connector.checkForFile(fileToInsertThenUpdate);
//        Assertions.assertTrue(retrievedFiles.contains(updatedFile), retrievedFiles.toString());
//    }
}
