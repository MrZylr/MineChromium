package com.zylr.pipbrowser;


import com.zylr.pipbrowser.properties.MainProperties;

import java.util.Properties;

public class Config {
    public static double getScale() {
        Properties p = MainProperties.getConfig();
        return Double.parseDouble(p.getProperty("scale", "0.5"));
    }
    public static void setScale(double value) {
        Properties p = MainProperties.getConfig();
        p.setProperty("scale", String.valueOf(value));
        MainProperties.saveConfig(p);
    }
}
