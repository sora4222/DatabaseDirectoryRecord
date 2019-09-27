package com.sora4222.database.connectors;

import com.sora4222.file.FileInformation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.UUID;

public class MySqlConnectorTest {

    MySqlConnector connector = null;
    private static String LOCATION_OF_TEST_CONFIG_FAKE_VALUES = "src/test/resources/filledConfigFile.json";

    @BeforeAll
    public static void systemProperties() {
        System.setProperty("config", LOCATION_OF_TEST_CONFIG_FAKE_VALUES);
    }

    @BeforeEach
    public void setupConnector() {
        connector = new MySqlConnector();
    }

    @AfterEach
    public void teardownConnector() {
        connector.close();
    }

    @Test
    public void InsertInformation() {
        String name  = UUID.randomUUID().toString() + ".txt";
        String location = Paths.get("").toAbsolutePath().toString() + name;
        String filehash = UUID.randomUUID().toString();
        String computerName = UUID.randomUUID().toString();

        connector.insertFile(new FileInformation(name,  location, filehash, computerName));
    }
}
