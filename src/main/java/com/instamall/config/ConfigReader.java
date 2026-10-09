package com.instamall.config;

import java.io.InputStream;
import java.util.Properties;

public final class ConfigReader {

    private static final Properties properties = new Properties();

    static {

        try (InputStream inputStream =
                     ConfigReader.class.getClassLoader()
                             .getResourceAsStream("config.properties")) {

            if (inputStream == null) {
                throw new RuntimeException("config.properties file not found.");
            }

            properties.load(inputStream);

        } catch (Exception e) {

            throw new RuntimeException("Unable to load config.properties", e);

        }

    }

    private ConfigReader() {

    }

    public static String get(String key) {

        return properties.getProperty(key);

    }

}