package com.sora4222.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class ConfigurationTest {
    private static String LOCATION_OF_TEST_CONFIG_FAKE_VALUES = "src/test/resources/filledConfigFile.json";
    private static List<String> LOCATIONS_IN_TEST_CONFIG = Arrays.asList("/location/1",
        "/location/that/I/endWith/forwardSlash/");
    
    @AfterEach
    public void removeConfig(){
        System.setProperty("config", "");
    }
    
    /**
     * The configurations will have:
     *  * a list of root locations to search in
     */
    @Test
    public void testConfigurationInstantiates(){
        System.setProperty("config", LOCATION_OF_TEST_CONFIG_FAKE_VALUES);
        Config configFile = Configuration.getConfiguration();
        Assertions.assertEquals(LOCATIONS_IN_TEST_CONFIG, configFile.rootLocations,
            "The locations listed are not as expected.");
    }
    
    @Test
    public void testRuntimeErrorThrownIfFileDoesntExist(){
        System.setProperty("config", "aBadConfig");
        System.out.println(Configuration.getConfiguration().rootLocations);
        Assertions.assertThrows(RuntimeException.class, Configuration::getConfiguration);
    }
}
