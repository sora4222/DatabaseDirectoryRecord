package com.sora4222.database.connectors;

import java.sql.Connection;

public class UtilityForConnector {
  public static Connection getOrInitializeConnection() {
    return ConnectionStorage.getConnection();
  }
}
