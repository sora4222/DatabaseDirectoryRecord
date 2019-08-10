package com.sora4222.database;

public class Configuration {
    private static Config heldConfig;

    public static Config getHeldConfig() {
        if (heldConfig == null){
            instantiateConfig();
        }
        return heldConfig;
    }

    private static void instantiateConfig() {

    }
}
