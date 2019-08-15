package com.sora4222.database.configuration;

import com.sora4222.database.configuration.Config;
import com.sora4222.database.configuration.Configuration;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConfigurationTest {
    private static String LOCATION_OF_TEST_CONFIG_FAKE_VALUES = "src/test/resources/filledConfigFile.json";
    private static List<String> LOCATIONS_IN_TEST_CONFIG = Arrays.asList("/location/1",
        "/location/that/I/endWith/forwardSlash/");
    
    @AfterEach
    public void removeConfig(){
        System.clearProperty("config");
    }
    
    /**
     * The configurations will have:
     *  * a list of root locations to search in
     */
    @Test
    @Order(1)
    public void testConfigurationInstantiates(){
        System.setProperty("config", LOCATION_OF_TEST_CONFIG_FAKE_VALUES);
        Config configFile = Configuration.getConfiguration();
        Assertions.assertEquals(LOCATIONS_IN_TEST_CONFIG, configFile.getRootLocations(),
            "The locations listed are not as expected.");
    }
    
    @Test
    @Order(2)
    public void testRuntimeErrorThrownIfFileDoesntExist(){
        Configuration.setLocation("ANonExistentFile");
        Assertions.assertThrows(RuntimeException.class, Configuration::getConfiguration);
    }
}
