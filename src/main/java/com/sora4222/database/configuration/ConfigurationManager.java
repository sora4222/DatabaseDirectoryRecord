package com.sora4222.database.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class ConfigurationManager {
  private static Config heldConfig;
  private static String location;
  private static Logger logger = LogManager.getLogger();
  
  @SuppressWarnings("ReturnPrivateMutableField")
  public static Config getConfiguration() {
    if (heldConfig == null) {
      location = System.getProperty("config", "");
      instantiateConfig();
    }
    return heldConfig;
  }
  
  private static void instantiateConfig() {
    logger.debug("Location of config: " + location);
    
    ObjectMapper jsonToObject = new ObjectMapper();
    File homeFileLocation = new File(String.format("%s%s.directoryRecorder%sConfig.json", System.getProperty("user.home"), File.separator, File.separator));
    try {
      if (!location.isEmpty()) {
        logger.debug("Reading from set location file.");
        heldConfig = jsonToObject.readValue(new File(location), Config.class);
      } else if (homeFileLocation.exists()) {
        logger.debug("Reading config from home directory location");
        heldConfig = jsonToObject.readValue(homeFileLocation, Config.class);
      }
      
      heldConfig = testDatabaseDetailsAreSetOtherwiseSetThemWithParameters(heldConfig);
    } catch (IOException e) {
      logger.error("An IO exception has occurred instantiating the config.", e);
      throw new RuntimeException(e);
    }
  }
  
  private static Config testDatabaseDetailsAreSetOtherwiseSetThemWithParameters(Config heldConfig) {
    if (heldConfig.getDatabasePassword().isEmpty()) {
      logger.debug("Database password is being set to: " + System.getProperty("databasePassword"));
      heldConfig.setDatabasePassword(System.getProperty("databasePassword"));
    }
    
    if (heldConfig.getDatabaseUsername().isEmpty()) {
      logger.debug("Database username is being set to  " + System.getProperty("databaseUsername"));
      heldConfig.setDatabaseUsername(System.getProperty("databaseUsername"));
    }
    
    if (heldConfig.getJdbcConnectionUrl().isEmpty()) {
      logger.debug("Database JDBC connection url is being set to  " + System.getProperty("jdbcConnectionUrl"));
      heldConfig.setJdbcConnectionUrl(System.getProperty("jdbcConnectionUrl"));
    }
    
    if (heldConfig.getDataTable().isEmpty()) {
      logger.debug("Database table is being set to  " + System.getProperty("dataTable"));
      heldConfig.setDataTable(System.getProperty("dataTable"));
    }
    
    return heldConfig;
  }
  
  /**
   * For testing only.
   * Removes the configuration
   */
  static void clearConfig() {
    heldConfig = null;
  }
}
