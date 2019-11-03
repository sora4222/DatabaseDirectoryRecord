package com.sora4222.database.configuration;

public class UtilityForConfig {
    public static void setLocationConfig(String location){
        ConfigurationManager.setLocation(location);
    }
    public static void clearConfig() {
        ConfigurationManager.clearConfig();
    }
}
