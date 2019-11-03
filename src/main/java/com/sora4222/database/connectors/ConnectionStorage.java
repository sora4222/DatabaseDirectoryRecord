package com.sora4222.database.connectors;

import com.google.common.annotations.VisibleForTesting;
import com.sora4222.database.configuration.Config;
import com.sora4222.database.configuration.ConfigurationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class ConnectionStorage {
    private static Connection connect;
    private static Config config = ConfigurationManager.getConfiguration();
    private static Logger logger = LogManager.getLogger();

    static {
        logger.info("Establishing connection to MySQL database.");
        while (true) {
            int i = 0;
            try {
                if(i++ != 0)
                    Thread.sleep((int) (100 * Math.exp(Math.min(i, 10))));
                initialize();
                break;
            } catch (SQLException e) {
                logger.error(String.format("The MySQL database cannot be connected to URI: %s\nError: %s",
                    config.getJdbcConnectionUrl(), e.getMessage()));
            } catch (NoSuchFieldException e) {
                logger.error(
                    String.format("A required field is missing for this application to start %s", e.getMessage()),
                    e);
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                logger.error(e);
            }
        }
    }

    private static void initialize () throws SQLException, NoSuchFieldException {
        logger.trace("Initializing database connection.");
        logger.debug("config jdbcConnectionUrl: " + config.getJdbcConnectionUrl());
        if(config.isJdbcConnectionUrlNotSet())
            throw new NoSuchFieldException("The jdbcConnectionUrl is not set in the configuration settings.");
        connect = DriverManager
            .getConnection(
                config.getJdbcConnectionUrl() + "?serverTimezone=Australia/Melbourne&allowMultiQueries=true",
                config.getDatabaseUsername(),
                config.getDatabasePassword());
    }

    private static void checkAndHandleDeadConnection() {
        try {
            if (connect.isClosed() || !connect.isValid(35)) {
                initialize();
            }
        } catch (SQLException e) {
            logger.error("The database whilst attempting to reconnect has failed.");
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            // Shouldn't ever happen as initialization should catch this.
            logger.error(e);
            throw new RuntimeException(e);
        }
    }
    
    public static void close() {
        try{
            connect.close();
        } catch (SQLException e) {
            logger.error("During connection close there was an error: ", e);
        }
    }
    
    /**
     * Intended for testing purposes only, it will return a connection to the database
     * in the same way that the main program does it.
     * @return A connection to the database without any wrapper.
     */
    @VisibleForTesting
    static Connection getConnection () {
        checkAndHandleDeadConnection();
        return connect;
    }

    private void backOffConnectionAttempts (int numberOfConnectionAttempts) {
        if (numberOfConnectionAttempts > 1) {
            try {
                if (numberOfConnectionAttempts <= 10)
                    TimeUnit.MILLISECONDS.sleep(100 * (long) Math.exp(numberOfConnectionAttempts));
                else
                    TimeUnit.HOURS.sleep(1);
            } catch (InterruptedException e) {
                logger.error(
                    "An interrupted exception occurred whilst backing off the number of connection attempts.",
                    e);
            }
        }
    }
}
