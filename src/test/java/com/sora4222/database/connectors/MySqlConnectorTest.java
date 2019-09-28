package com.sora4222.database.connectors;

import com.sora4222.file.FileInformation;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MySqlConnectorTest {
  
  MySqlConnector connector = null;
  private static String LOCATION_OF_TEST_CONFIG_FAKE_VALUES = "src/test/resources/filledConfigFile.json";
  
  String name;
  String location;
  String filehash;
  String computerName;
  
  @BeforeAll
  public static void systemProperties() {
    System.setProperty("config", LOCATION_OF_TEST_CONFIG_FAKE_VALUES);
  }
  
  @BeforeEach
  public void setup() {
    connector = new MySqlConnector();
    createNewFakeFile();
  }
  
  
  public void createNewFakeFile() {
    name = UUID.randomUUID().toString() + ".txt";
    location = Paths.get("").toAbsolutePath().toString() + name;
    filehash = UUID.randomUUID().toString();
    computerName = UUID.randomUUID().toString();
  }
  
  @AfterEach
  public void teardownConnector() {
    connector.close();
  }
  
  @Test
  public void InsertInformation() {
    Assertions.assertTrue(connector.insertFile(new FileInformation(name, location, filehash, computerName)));
  }
  
  @Test
  public void InsertAndSelect() {
    FileInformation file = new FileInformation(name, location, computerName, filehash);
    connector.insertFile(file);
    
    List<FileInformation> fileList = new ArrayList<>();
    fileList.add(file);
    Assertions.assertEquals(fileList, connector.checkForFile(file));
    
  }
  
  @Test
  public void InsertThenDelete() {
    FileInformation fileToInsertAndDelete = new FileInformation(name, location, computerName, filehash);
    connector.insertFile(fileToInsertAndDelete);
    
    Assertions.assertTrue(connector.deleteFileRow(fileToInsertAndDelete));
    
    Assertions.assertTrue(connector.checkForFile(fileToInsertAndDelete).isEmpty());
  }
  
  @Test
  public void UpdateFileHash() {
    FileInformation fileToInsertThenUpdate = new FileInformation(name, location, computerName, filehash);
    connector.insertFile(fileToInsertThenUpdate);
    
    FileInformation updatedFile = new FileInformation(name, location, computerName, UUID.randomUUID().toString());
    Assertions.assertTrue(connector.updateFileRow(updatedFile));
    List<FileInformation> retrievedFiles = connector.checkForFile(updatedFile);
    Assertions.assertTrue(retrievedFiles.contains(updatedFile), retrievedFiles.toString());
  }
  
  @Test
  public void InsertTwoFilesDifferentHashSameName() {
    FileInformation fileToInsertThenUpdate = new FileInformation(name, location, computerName, filehash);
    connector.insertFile(fileToInsertThenUpdate);
    
    FileInformation updatedFile = new FileInformation(name, location, computerName, UUID.randomUUID().toString());
    Assertions.assertTrue(connector.updateFileRow(updatedFile));
    
    List<FileInformation> retrievedFiles = connector.checkForFile(fileToInsertThenUpdate);
    Assertions.assertTrue(retrievedFiles.contains(updatedFile), retrievedFiles.toString());
  }
}
