package com.sora4222.database.configuration;

import com.google.common.base.Suppliers;
import com.sora4222.database.connectors.ConnectionStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

public class ComputerProperties {
  private static Logger logger = LogManager.getLogger();
  public static Supplier<Integer> computerNameId = Suppliers.memoize(ComputerProperties::getOrCreateComputerId);
  
  private static int getOrCreateComputerId() {
    String computerName = getComputerName();
    
    int computerId = getComputerId(computerName);
    if (computerId != -1)
      return computerId;
    
    createComputerId(computerName);
    return getComputerId(computerName);
  }
  
  /**
   * Inserts the computers name into the database.
   * This will generate an id (primary key) that will be used later.
   *
   * @param computerName The computers hostname that will be inserted
   *                     into the database.
   */
  private static void createComputerId(String computerName) {
    Connection conn = ConnectionStorage.getConnection();
    try {
      PreparedStatement stmt = conn.prepareStatement("INSERT INTO computer_names (ComputerName) VALUES ?");
      stmt.setString(1, computerName);
    } catch (SQLException e) {
      logger.error(e);
    }
  }
  
  /**
   * Gets the computername id in the database. Is used to
   * query against in the main database.
   *
   * @param computerName the name to query against
   * @return -1 if there is no result, otherwise the id number.
   */
  private static int getComputerId(String computerName) {
    Connection conn = ConnectionStorage.getConnection();
    try {
      PreparedStatement stmt = conn.prepareStatement("SELECT ComputerId FROM computer_names WHERE ComputerName=?");
      stmt.setString(1, computerName);
      ResultSet computerId = stmt.executeQuery();
      if (computerId.next()) {
        return computerId.getInt("ComputerIdNumber");
      }
      
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return -1;
  }
  
  private static String getComputerName() {
    while (true) {
      try {
        return getHostName();
      } catch (RuntimeException e) {
        logger.error(e);
      }
    }
  }
  
  private static String getHostName() {
    String linuxHostname = getLinuxHostname();
    if (!linuxHostname.isEmpty()) {
      logger.info("Linux hostname: " + linuxHostname);
      return linuxHostname;
    } else if (!(System.getenv("COMPUTERNAME") == null)) {
      logger.info("Windows hostname: " + System.getenv("COMPUTERNAME"));
      return System.getenv("COMPUTERNAME");
    } else {
      String message = "The computer does not have a name";
      logger.error(message);
      
      throw new RuntimeException(message);
    }
  }
  
  private static String getLinuxHostname() {
    InetAddress ip;
    String hostname = "";
    try {
      ip = InetAddress.getLocalHost();
      hostname = ip.getHostName();
      
    } catch (UnknownHostException e) {
      logger.info("The attempt to obtain a hostname for linux devices has failed.");
    }
    return hostname;
  }
}
