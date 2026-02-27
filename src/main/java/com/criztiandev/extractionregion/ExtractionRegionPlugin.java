package com.criztiandev.extractionregion;

import com.criztiandev.extractionchest.ExtractionChestPlugin;
import com.criztiandev.extractionregion.managers.RegionManager;
import com.criztiandev.extractionregion.storage.RegionStorageProvider;
import com.criztiandev.extractionregion.storage.SQLiteRegionStorageProvider;
import com.criztiandev.extractionregion.commands.RegionCommand;
import com.criztiandev.extractionregion.listeners.RegionChatPromptListener;
import com.criztiandev.extractionregion.listeners.RegionInventoryListener;
import com.criztiandev.extractionregion.listeners.RegionWandListener;
import com.criztiandev.extractionregion.tasks.RegionTickManager;
import com.criztiandev.extractionregion.tasks.SelectionVisualizerTask;
import org.bukkit.plugin.java.JavaPlugin;

public class ExtractionRegionPlugin extends JavaPlugin {

    private ExtractionChestPlugin extractionChestApi;
    private RegionStorageProvider storageProvider;
    private RegionManager regionManager;
    private SelectionVisualizerTask visualizerTask;
    private com.criztiandev.extractionregion.tasks.ExtractionTask extractionTask;
    private com.criztiandev.extractionregion.tasks.EntryTask entryTask;

    @Override
    public void onEnable() {
        getLogger().info("ExtractionRegionEditor has been enabled!");
        saveDefaultConfig();

        // Initialize ExtractionChest API
        this.extractionChestApi = (ExtractionChestPlugin) getServer().getPluginManager().getPlugin("ExtractionChest");
        if (this.extractionChestApi == null) {
            getLogger().severe("ExtractionChest not found! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.storageProvider = new SQLiteRegionStorageProvider(this);
        this.storageProvider.initialize();

        this.regionManager = new RegionManager(this);
        this.regionManager.loadAll();

        // Inject Region Timer Override into ExtractionChest
        this.extractionChestApi.setRegionTimerCallback(chest -> {
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(chest.getWorld());
            if (world != null) {
                com.criztiandev.extractionregion.models.SavedRegion region = this.regionManager.getRegionAt(chest.getLocation(world));
                if (region != null && region.getType() == com.criztiandev.extractionregion.models.RegionType.CHEST_REPLENISH && region.getResetIntervalMinutes() > 0) {
                    return region.getNextResetTime();
                }
            }
            return null;
        });

        getServer().getPluginManager().registerEvents(new RegionWandListener(this), this);
        getServer().getPluginManager().registerEvents(new RegionInventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new RegionChatPromptListener(this), this);
        getServer().getPluginManager().registerEvents(new com.criztiandev.extractionregion.listeners.ExtractionMechanicsListener(this), this);
        RegionCommand regionCommand = new RegionCommand(this);
        if (getCommand("regioneditor") != null) getCommand("regioneditor").setExecutor(regionCommand);
        if (getCommand("regionchest") != null) getCommand("regionchest").setExecutor(regionCommand);
        if (getCommand("regionentry") != null) getCommand("regionentry").setExecutor(regionCommand);
        if (getCommand("regionexit") != null) getCommand("regionexit").setExecutor(regionCommand);

        // Run the region tick manager every 10 seconds (200 ticks)
        new RegionTickManager(this).runTaskTimer(this, 100L, 200L);

        // Run the selection visualizer every 10 ticks (0.5 seconds)
        this.visualizerTask = new SelectionVisualizerTask(this);
        this.visualizerTask.runTaskTimer(this, 10L, 10L);
        
        // Run the extraction checking task every 5 ticks (0.25 seconds)
        this.extractionTask = new com.criztiandev.extractionregion.tasks.ExtractionTask(this);
        this.extractionTask.runTaskTimer(this, 5L, 5L);

        // Run the entry portal task every 10 ticks (0.5 seconds)
        this.entryTask = new com.criztiandev.extractionregion.tasks.EntryTask(this);
        getServer().getScheduler().runTaskTimer(this, this.entryTask, 10L, 10L);
    }

    @Override
    public void onDisable() {
        if (this.visualizerTask != null) {
            this.visualizerTask.clearAll();
        }
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
    
    public com.criztiandev.extractionregion.tasks.ExtractionTask getExtractionTask() {
        return extractionTask;
    }
}

