package com.sora4222.database.configuration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.engine.script.ScriptAccessor;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConfigurationManagerTest {
  private static String LOCATION_OF_TEST_CONFIG_FAKE_VALUES = "src/test/resources/filledConfigFile.json";
  private static List<String> LOCATIONS_IN_TEST_CONFIG = Arrays.asList("/location/1",
    "/location/that/I/endWith/forwardSlash/");
  
  @AfterEach
  public void removeConfig() {
    System.clearProperty("config");
  }
  
  @AfterAll
  public static void removeAllSetConfiguration() {
    System.setProperty("config", "");
    UtilityForConfig.clearConfig();
  }
  
  /**
   * The configurations will have:
   *  * a list of root locations to search in
   */
  @Test
  @Order(1)
  public void testConfigurationInstantiates(){
    System.setProperty("config", LOCATION_OF_TEST_CONFIG_FAKE_VALUES);
    Config configFile = ConfigurationManager.getConfiguration();
    Assertions.assertEquals(LOCATIONS_IN_TEST_CONFIG, configFile.getRootLocations(),
      "The locations listed are not as expected.");
  }
  
  @Test
  @Order(2)
  public void testRuntimeErrorThrownIfFileDoesntExist(){
    System.setProperty("config", "ANonExistentFile");
    Assertions.assertThrows(RuntimeException.class, ConfigurationManager::getConfiguration);
  }
  
  @Test
  @Order(3)
  public void testLocationsCanBeReturnedAsPaths() {
    System.setProperty("config", LOCATION_OF_TEST_CONFIG_FAKE_VALUES);
    List<Path> allPathsReturned = ConfigurationManager.getConfiguration().getRootLocationsAsPaths();
    
    List<String> allPathsExpected = Arrays.asList("/location/1", "/location/that/I/endWith/forwardSlash");
    for(Path pathReturned: allPathsReturned){
      Assertions.assertTrue(allPathsExpected.contains(pathReturned.toString().replace("\\", "/")),
        String.format("The path: %s is not in the list of expected paths: %s",
          pathReturned.toString().replace("\\", "/"),
          allPathsExpected.toString())
        );
    }
  }
}
