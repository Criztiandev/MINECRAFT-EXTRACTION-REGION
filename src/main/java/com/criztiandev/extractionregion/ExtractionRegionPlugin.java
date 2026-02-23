package com.criztiandev.extractionregion;

import org.bukkit.plugin.java.JavaPlugin;

public class ExtractionRegionPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("ExtractionRegionEditor has been enabled!");
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        getLogger().info("ExtractionRegionEditor has been disabled!");
    }
}
