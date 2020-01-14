package com.sora4222.database.connectors;

import com.sora4222.database.configuration.ConfigurationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class ConnectionStorage {
    private static Connection connect;
    private static Logger logger = LogManager.getLogger();

    static {
      logger.info("Establishing connection to MySQL database.");
      int numberOfAttempts = 0;
      while (true) {
        try {
          if (numberOfAttempts++ != 0)
            backOffConnectionAttempts(numberOfAttempts);
          initialize();
          break;
        } catch (SQLException e) {
          logger.error(String.format("The MySQL database cannot be connected to URI: %s\nError: %s",
            ConfigurationManager.getConfiguration().getJdbcConnectionUrl(), e.getMessage()));
        } catch (NoSuchFieldException e) {
          logger.error(
                    String.format("A required field is missing for this application to start %s", e.getMessage()), e);
                throw new RuntimeException(e);
            }
        }
    }
  
  private static void initialize() throws SQLException, NoSuchFieldException {
    logger.trace("Initializing database connection.");
    logger.debug("config jdbcConnectionUrl: " + ConfigurationManager.getConfiguration().getJdbcConnectionUrl());
    if (ConfigurationManager.getConfiguration().isJdbcConnectionUrlNotSet())
      throw new NoSuchFieldException("The jdbcConnectionUrl is not set in the configuration settings.");
  
    connect = DriverManager
      .getConnection(
        ConfigurationManager.getConfiguration().getJdbcConnectionUrl() + "?serverTimezone=Australia/Melbourne&allowMultiQueries=true",
        ConfigurationManager.getConfiguration().getDatabaseUsername(),
        ConfigurationManager.getConfiguration().getDatabasePassword());
  }
  
  /**
   * @return If the connection is successfully made this will return a true
   */
  private static boolean checkAndHandleDeadConnection() {
    try {
      if (connect.isClosed() || !connect.isValid(1000)) {
        initialize();
      }
      return !connect.isValid(1000);
    } catch (SQLException e) {
      logger.error("The database whilst attempting to reconnect has failed.");
    } catch (NoSuchFieldException e) {
      // Shouldn't ever happen as initialization should catch this.
      logger.error(e);
    }
    return false;
  }
  
  public static void close() {
        try{
            connect.close();
        } catch (SQLException e) {
            logger.error("During connection close there was an error: ", e);
        }
    }
    
    /**
     * Obtains a connection to a database, also checking whether the connection is
     * dead or closed and reconnecting if it is.
     *
     * @return A connection to the database without any wrapper.
     */
    public static Connection getConnection() {
      int i = 0;
      while (checkAndHandleDeadConnection()) {
        backOffConnectionAttempts(i++);
      }
  
      return connect;
    }
  
  @SuppressWarnings("WeakerAccess")
  public static void backOffConnectionAttempts(int numberOfConnectionAttempts) {
    if (numberOfConnectionAttempts > 1) {
      try {
        if (numberOfConnectionAttempts <= 9)
          TimeUnit.MILLISECONDS.sleep(100 * (long) Math.exp(numberOfConnectionAttempts));
        else
          TimeUnit.MINUTES.sleep(1);
      } catch (InterruptedException e) {
        logger.error(
          "An interrupted exception occurred whilst backing off the number of connection attempts.",
          e);
            }
        }
    }
}
