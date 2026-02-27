package com.criztiandev.extractionregion.storage;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class YamlRegionStorageProvider implements RegionStorageProvider {

    private final ExtractionRegionPlugin plugin;
    private final File regionsFile;

    public YamlRegionStorageProvider(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
        this.regionsFile = new File(plugin.getDataFolder(), "regions.yml");
    }

    @Override
    public void initialize() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        plugin.getLogger().info("YamlRegionStorageProvider initialized.");
    }

    @Override
    public void shutdown() {
    }

    @Override
    public CompletableFuture<List<SavedRegion>> loadAllRegions() {
        return CompletableFuture.supplyAsync(() -> {
            List<SavedRegion> regions = new ArrayList<>();
            if (!regionsFile.exists()) return regions;

            YamlConfiguration config = YamlConfiguration.loadConfiguration(regionsFile);
            ConfigurationSection root = config.getConfigurationSection("regions");
            if (root == null) return regions;

            for (String key : root.getKeys(false)) {
                ConfigurationSection sec = root.getConfigurationSection(key);
                if (sec == null) continue;

                String id = sec.getString("id");
                String world = sec.getString("world");
                int minX = sec.getInt("minX");
                int maxX = sec.getInt("maxX");
                int minZ = sec.getInt("minZ");
                int maxZ = sec.getInt("maxZ");

                SavedRegion region = new SavedRegion(id, world, minX, maxX, minZ, maxZ);

                if (sec.contains("nextResetTime")) {
                    region.setNextResetTime(sec.getLong("nextResetTime"));
                }
                if (sec.contains("resetIntervalMinutes")) {
                    region.setResetIntervalMinutes(sec.getInt("resetIntervalMinutes"));
                }

                regions.add(region);
            }
            return regions;
        });
    }

    @Override
    public CompletableFuture<Void> saveRegion(SavedRegion region) {
        return CompletableFuture.runAsync(() -> {
            YamlConfiguration config = regionsFile.exists() ? YamlConfiguration.loadConfiguration(regionsFile) : new YamlConfiguration();
            
            String path = "regions." + region.getId();
            config.set(path + ".id", region.getId());
            config.set(path + ".world", region.getWorld());
            config.set(path + ".minX", region.getMinX());
            config.set(path + ".maxX", region.getMaxX());
            config.set(path + ".minZ", region.getMinZ());
            config.set(path + ".maxZ", region.getMaxZ());
            
            config.set(path + ".nextResetTime", region.getNextResetTime());
            config.set(path + ".resetIntervalMinutes", region.getResetIntervalMinutes());

            try {
                config.save(regionsFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save region " + region.getId(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteRegion(String id) {
        return CompletableFuture.runAsync(() -> {
            if (!regionsFile.exists()) return;
            YamlConfiguration config = YamlConfiguration.loadConfiguration(regionsFile);
            config.set("regions." + id, null);
            try {
                config.save(regionsFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete region " + id, e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> renameRegion(String oldId, String newId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!regionsFile.exists()) return false;
            YamlConfiguration config = YamlConfiguration.loadConfiguration(regionsFile);
            
            if (!config.contains("regions." + oldId)) {
                return false;
            }
            
            // Move section
            ConfigurationSection oldSection = config.getConfigurationSection("regions." + oldId);
            config.set("regions." + newId, oldSection);
            config.set("regions." + newId + ".id", newId); // Update internal ID field if stored
            config.set("regions." + oldId, null);
            
            try {
                config.save(regionsFile);
                return true;
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to rename region from " + oldId + " to " + newId + " in YAML", e);
                return false;
            }
        });
    }
}
