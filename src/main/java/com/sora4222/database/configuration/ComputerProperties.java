package com.sora4222.database.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ComputerProperties {
    private static Logger logger = LogManager.getLogger();

    public static String getComputerName () {
        String linuxHostname =  getLinuxHostname();
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
