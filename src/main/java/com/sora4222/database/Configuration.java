package com.sora4222.database;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class Configuration {
    private static Config heldConfig;

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
            heldConfig = jsonToObject.readValue(new File(System.getProperty("config")), Config.class);
        } catch (IOException e) {
            // Logging
            throw new RuntimeException(e);
        }
    }
}
