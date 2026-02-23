package com.criztiandev.extractionregion;

import com.criztiandev.extractionchest.ExtractionChestPlugin;
import com.criztiandev.extractionregion.managers.RegionManager;
import com.criztiandev.extractionregion.storage.RegionStorageProvider;
import com.criztiandev.extractionregion.storage.YamlRegionStorageProvider;
import com.criztiandev.extractionregion.commands.RegionCommand;
import com.criztiandev.extractionregion.listeners.RegionWandListener;
import com.criztiandev.extractionregion.tasks.RegionTickManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ExtractionRegionPlugin extends JavaPlugin {

    private ExtractionChestPlugin extractionChestApi;
    private RegionStorageProvider storageProvider;
    private RegionManager regionManager;

    @Override
    public void onEnable() {
        getLogger().info("ExtractionRegionEditor has been enabled!");
        saveDefaultConfig();

        this.extractionChestApi = (ExtractionChestPlugin) getServer().getPluginManager().getPlugin("ExtractionChest");
        if (this.extractionChestApi == null) {
            getLogger().severe("ExtractionChest not found! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.storageProvider = new YamlRegionStorageProvider(this);
        this.storageProvider.initialize();

        this.regionManager = new RegionManager(this);
        this.regionManager.loadAll();

        getServer().getPluginManager().registerEvents(new RegionWandListener(this), this);
        getCommand("regioneditor").setExecutor(new RegionCommand(this));

        // Run the region tick manager every 60 seconds (1200 ticks)
        new RegionTickManager(this).runTaskTimer(this, 100L, 1200L);
    }

    @Override
    public void onDisable() {
        if (this.storageProvider != null) {
            this.storageProvider.shutdown();
        }
        getLogger().info("ExtractionRegionEditor has been disabled!");
    }

    public ExtractionChestPlugin getExtractionChestApi() {
        return extractionChestApi;
    }

    public RegionStorageProvider getStorageProvider() {
        return storageProvider;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }
}

