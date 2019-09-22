package com.sora4222.database.configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;


public class Config {
  @Getter
  @Setter
  List<String> rootLocations;
  
  @Getter
  @Setter
  String jdbcConnectionUrl;
  
  @Setter
  @Getter
  String databaseUsername;
  
  @Setter
  @Getter
  String databasePassword;
  /**
   * Gets the root locations as paths.
   * @return root locations as paths.
   */
  public List<Path> getRootLocationsAsPaths() {
    List<Path> rootLocationsAsPaths = new LinkedList<>();
    
    for (String rootPathAsString : rootLocations) {
      rootLocationsAsPaths.add(Paths.get(rootPathAsString));
    }
    
    return rootLocationsAsPaths;
  }
}
