package com.zylr.pipbrowser.properties;

import java.io.*;
import java.util.Properties;

public class MainProperties {

    public static final String PROPDIR = "browser/properties";
    public static final String PATH = "browser/properties/config.properties";

    public static Properties getConfig() {
        try (InputStream input = new FileInputStream(PATH)) {

            Properties config = new Properties();

            // Load config
            config.load(input);
            return config;
        }catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void saveConfig(Properties config) {
        try (OutputStream output = new FileOutputStream(PATH)) {
            // Save the config
            config.store(output, null);
        }catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
