package com.zylr.pipbrowser;

import com.zylr.pipbrowser.hud.MainHud;
import com.zylr.pipbrowser.properties.MainProperties;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.*;
import java.util.Properties;

public class PIPBrowser implements ModInitializer {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "pipbrowser";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    private static PIPBrowser instance;
    public MainHud mainHud;

    public PIPBrowser() {
        instance = this;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing PIP Browser");

        // Create config file
        setupConfigs();

        // Create Widgets and HUDs
        this.mainHud = new MainHud();
    }

    private void setupConfigs() {
        // Verify directories
        File propDirectory = new File(MainProperties.PROPDIR);
        File widgetDir = new File("browser/properties/widgets");
        // Primary config
        File mainConfigPath = new File(MainProperties.PATH);

        // Configs
        Properties mainConfig = new Properties();
        try {
            // Check if directories exist
            if (!propDirectory.exists())
                propDirectory.mkdirs();
            if (!widgetDir.exists())
                widgetDir.mkdirs();
            // Check if config files exist
            if (!mainConfigPath.exists()) {
                mainConfigPath.createNewFile();
                // Load defaults for config files
                InputStream mainConfigInput = this.getClass().getClassLoader().getResourceAsStream("configs/mainconfig.properties");
                mainConfig.load(mainConfigInput);
                // Save default config files
                OutputStream mainConfigOutput = new FileOutputStream(MainProperties.PATH);
                mainConfig.store(mainConfigOutput, null);
                LOGGER.info("Created default config file at " + MainProperties.PATH);
            }
        }catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    public static PIPBrowser getInstance() {
        return instance;
    }
}
