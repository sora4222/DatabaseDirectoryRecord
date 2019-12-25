package com.sora4222.database.configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Config {
  private static Logger logger = LogManager.getLogger();
  
  @Getter
  @Setter
  List<String> rootLocations;
  
  @Getter
  @Setter
  String jdbcConnectionUrl = "";
  
  @Setter
  @Getter
  String databaseUsername = "";
  
  @Setter
  @Getter
  String databasePassword = "";
  
  @Setter
  List<String> excludeRegex = new LinkedList<>();
  
  List<Predicate<String>> compiledRegex = new LinkedList<>();
  
  /**
   * Gets the root locations as paths.
   *
   * @return root locations as paths.
   */
  public List<Path> getRootLocationsAsPaths() {
    List<Path> rootLocationsAsPaths = new LinkedList<>();
    
    for (String rootPathAsString : rootLocations) {
      rootLocationsAsPaths.add(Paths.get(rootPathAsString));
    }
    
    return rootLocationsAsPaths;
  }
  
  public List<Predicate<String>> getExcludeRegex() {
    if(!excludeRegex.isEmpty() && compiledRegex.isEmpty()) {
      for (String regexDeclared: excludeRegex) {
        logger.info("Compiling regular expression: " + regexDeclared);
        compiledRegex.add(Pattern.compile(regexDeclared).asPredicate());
      }
    }
    return compiledRegex;
  }
  
  public boolean isJdbcConnectionUrlNotSet() {
    return jdbcConnectionUrl.isEmpty();
  }
  
  @Override
  public String toString(){
  
    String sb = "rootLocations:\n" +
        rootLocations.toString() +
        "\nexcludeRegex:\n" +
        excludeRegex.toString();
    return sb;
  }
}
