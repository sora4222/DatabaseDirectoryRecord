package com.sora4222.database.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class Configuration {
    private static Config heldConfig;
    private static String location = System.getProperty("config");
    private static Logger logger = LogManager.getLogger();
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
            heldConfig = testDatabaseDetailsAreSetOtherwiseSetThem(heldConfig);
        } catch (IOException e) {
            logger.error("An IO exception has occurred instantiating the config.", e);
            throw new RuntimeException(e);
        }
    }

    private static Config testDatabaseDetailsAreSetOtherwiseSetThem(Config heldConfig) {
        if (heldConfig.getDatabasePassword().isEmpty()) {
            logger.info("Database password is being set to: " + System.getProperty("databasePassword"));
           heldConfig.setDatabasePassword(System.getProperty("databasePassword"));
        }

        if (heldConfig.getDatabaseUsername().isEmpty()) {
            logger.info("Database username is being set to  " + System.getProperty("databaseUsername"));
            heldConfig.setDatabaseUsername(System.getProperty("databaseUsername"));
        }

        if (heldConfig.getJdbcConnectionUrl().isEmpty()) {
            logger.info("Database JDBC connection url is being set to  " + System.getProperty("jdbcConnectionUrl"));
            heldConfig.setJdbcConnectionUrl(System.getProperty("jdbcConnectionUrl"));
        }

        if (heldConfig.getDataTable().isEmpty()) {
            logger.info("Database table is being set to  " + System.getProperty("dataTable"));
            heldConfig.setDataTable(System.getProperty("dataTable"));
        }

        return heldConfig;
    }

    @SuppressWarnings("SameParameterValue")
    static void setLocation (final String location) {
        Configuration.heldConfig = null;
        Configuration.location = location;
    }
}
