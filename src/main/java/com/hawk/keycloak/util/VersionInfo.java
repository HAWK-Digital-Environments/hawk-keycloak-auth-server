package com.hawk.keycloak.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class VersionInfo {

    private static final String PACKAGE_VERSION;

    static {
        String version;
        Properties properties = new Properties();
        try (InputStream input = VersionInfo.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (input != null) {
                properties.load(input);
                version = properties.getProperty("version", "unknown");
            } else {
                version = "unknown";
            }
        } catch (IOException e) {
            log.error("Failed to read version.properties", e);
            version = "unknown";
        }
        PACKAGE_VERSION = version;
    }

    public static String getPackageVersion() {
        return PACKAGE_VERSION;
    }
}
