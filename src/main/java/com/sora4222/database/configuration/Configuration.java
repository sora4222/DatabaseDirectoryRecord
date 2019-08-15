package com.sora4222.database.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class Configuration {
    private static Config heldConfig;
    private static String location = System.getProperty("config");
    
    @SuppressWarnings("ReturnPrivateMutableField")
    public static Config getConfiguration() {
        if (heldConfig == null){
            instantiateConfig();
        }
        return heldConfig;
    }

    private static void instantiateConfig() {
        ObjectMapper jsonToObject = new ObjectMapper();
        try {
            heldConfig = jsonToObject.readValue(new File(location), Config.class);
        } catch (IOException e) {
            // Logging
            throw new RuntimeException(e);
        }
    }
    
    @SuppressWarnings("SameParameterValue")
    static void setLocation (final String location) {
        Configuration.heldConfig = null;
        Configuration.location = location;
    }
}
