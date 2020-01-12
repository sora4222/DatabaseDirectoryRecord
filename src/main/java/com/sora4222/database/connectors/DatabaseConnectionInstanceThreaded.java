package com.sora4222.database.connectors;

import com.sora4222.database.configuration.ConfigurationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static com.sora4222.database.connectors.ConnectionStorage.backOffConnectionAttempts;

//TODO: Refactor with ConnectionStorage
public class DatabaseConnectionInstanceThreaded {
  
  private static Logger logger = LogManager.getLogger();
  private Connection conn;
  
  /**
   * Creates a database connection that can be used with
   * multiple threads.
   */
  public DatabaseConnectionInstanceThreaded() {
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
  
  private void initialize() throws SQLException, NoSuchFieldException {
    logger.trace("Initializing database connection.");
    logger.debug("Config jdbcConnectionUrl: " + ConfigurationManager.getConfiguration().getJdbcConnectionUrl());
    if (ConfigurationManager.getConfiguration().isJdbcConnectionUrlNotSet())
      throw new NoSuchFieldException("The jdbcConnectionUrl is not set in the configuration settings.");
    conn = DriverManager
      .getConnection(
        ConfigurationManager.getConfiguration().getJdbcConnectionUrl() + "?serverTimezone=Australia/Melbourne&allowMultiQueries=true",
        ConfigurationManager.getConfiguration().getDatabaseUsername(),
        ConfigurationManager.getConfiguration().getDatabasePassword());
  }
  
  public void close() {
    try {
      conn.close();
    } catch (SQLException e) {
      logger.error("During connection close for an DatabaseConnectionInstanceThreaded an error occurred.", e);
    }
  }
  
  /**
   * Obtains a connection to a database,
   * also checking whether the connection is
   * dead or closed and reconnecting if it is.
   *
   * @return A connection to the database without any wrapper.
   */
  public Connection getConnection() {
    int i = 0;
    while (checkAndHandleDeadConnection()) {
      backOffConnectionAttempts(i++);
    }
    return conn;
  }
  
  /**
   * @return If the connection is successfully made this will return a true
   */
  private boolean checkAndHandleDeadConnection() {
    try {
      if (conn.isClosed() || !conn.isValid(1000)) {
        initialize();
      }
      return !conn.isValid(1000);
    } catch (SQLException e) {
      logger.error("The database whilst attempting to reconnect has failed.");
    } catch (NoSuchFieldException e) {
      // Shouldn't ever happen as initialization should catch this.
      logger.error(e);
    }
    return false;
  }
}
