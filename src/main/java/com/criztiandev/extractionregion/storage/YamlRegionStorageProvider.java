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

                if (sec.contains("type")) {
                    try {
                        region.setType(com.criztiandev.extractionregion.models.RegionType.valueOf(sec.getString("type")));
                    } catch (Exception ignored) {}
                }
                
                if (sec.contains("conduit.world")) {
                    org.bukkit.World cWorld = org.bukkit.Bukkit.getWorld(sec.getString("conduit.world"));
                    if (cWorld != null) {
                        region.setConduitLocation(new org.bukkit.Location(cWorld, 
                            sec.getInt("conduit.x"), 
                            sec.getInt("conduit.y"), 
                            sec.getInt("conduit.z")));
                    }
                }
                
                if (sec.contains("cooldownSequence")) {
                    String seqStr = sec.getString("cooldownSequence");
                    java.util.List<Integer> seq = new java.util.ArrayList<>();
                    if (seqStr != null && !seqStr.isEmpty()) {
                        for (String part : seqStr.split(",")) {
                            try { seq.add(Integer.parseInt(part.trim())); } catch (NumberFormatException ignored) {}
                        }
                    }
                    if (!seq.isEmpty()) {
                        region.setCooldownSequence(seq);
                    }
                } else if (sec.contains("cooldownMinutes")) {
                    // Legacy migration
                    region.setCooldownSequence(new java.util.ArrayList<>(java.util.Arrays.asList(sec.getInt("cooldownMinutes"))));
                }
                
                if (sec.contains("cooldownIndex")) region.setCooldownIndex(sec.getInt("cooldownIndex"));
                if (sec.contains("maxCapacity")) region.setMaxCapacity(sec.getInt("maxCapacity"));
                if (sec.contains("minCapacity")) region.setMinCapacity(sec.getInt("minCapacity"));
                if (sec.contains("cooldownEndTime")) region.setCooldownEndTime(sec.getLong("cooldownEndTime"));
                if (sec.contains("mimicEnabled")) region.setMimicEnabled(sec.getBoolean("mimicEnabled"));
                if (sec.contains("mimicChance")) region.setMimicChance(sec.getInt("mimicChance"));
                if (sec.contains("announcementRadius")) region.setAnnouncementRadius(sec.getInt("announcementRadius"));
                if (sec.contains("beamColor")) region.setBeamColor(sec.getString("beamColor"));
                if (sec.contains("alarmSound")) region.setAlarmSound(sec.getString("alarmSound"));

                if (sec.contains("drop")) {
                    region.setDropWorld(sec.getString("drop.world"));
                    region.setDropMinX(sec.getInt("drop.minX"));
                    region.setDropMaxX(sec.getInt("drop.maxX"));
                    region.setDropMinY(sec.getInt("drop.minY"));
                    region.setDropMaxY(sec.getInt("drop.maxY"));
                    region.setDropMinZ(sec.getInt("drop.minZ"));
                    region.setDropMaxZ(sec.getInt("drop.maxZ"));
                }
                
                if (sec.contains("extractionSpawn")) {
                    region.setExtractionSpawnWorld(sec.getString("extractionSpawn.world"));
                    region.setExtractionSpawnX(sec.getDouble("extractionSpawn.x"));
                    region.setExtractionSpawnY(sec.getDouble("extractionSpawn.y"));
                    region.setExtractionSpawnZ(sec.getDouble("extractionSpawn.z"));
                    region.setExtractionSpawnYaw((float) sec.getDouble("extractionSpawn.yaw"));
                    region.setExtractionSpawnPitch((float) sec.getDouble("extractionSpawn.pitch"));
                }
                
                if (sec.contains("possibleDurations")) {
                    String seqStr = sec.getString("possibleDurations");
                    java.util.List<Integer> seq = new java.util.ArrayList<>();
                    if (seqStr != null && !seqStr.isEmpty()) {
                        for (String part : seqStr.split(",")) {
                            try { seq.add(Integer.parseInt(part.trim())); } catch (NumberFormatException ignored) {}
                        }
                    }
                    if (!seq.isEmpty()) {
                        region.setPossibleDurations(seq);
                    }
                } else if (sec.contains("extractionDurationSeconds")) {
                    // Legacy migration
                    region.setPossibleDurations(new java.util.ArrayList<>(java.util.Arrays.asList(sec.getInt("extractionDurationSeconds"))));
                }
                
                if (sec.contains("slowFallingSeconds")) region.setSlowFallingSeconds(sec.getInt("slowFallingSeconds"));
                if (sec.contains("blindnessSeconds")) region.setBlindnessSeconds(sec.getInt("blindnessSeconds"));

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
            
            config.set(path + ".type", region.getType().name());
            
            if (region.getConduitLocation() != null && region.getConduitLocation().getWorld() != null) {
                config.set(path + ".conduit.world", region.getConduitLocation().getWorld().getName());
                config.set(path + ".conduit.x", region.getConduitLocation().getBlockX());
                config.set(path + ".conduit.y", region.getConduitLocation().getBlockY());
                config.set(path + ".conduit.z", region.getConduitLocation().getBlockZ());
            } else {
                config.set(path + ".conduit", null);
            }
            
            java.util.List<Integer> seq = region.getCooldownSequence();
            StringBuilder seqBuilder = new StringBuilder();
            for (int i = 0; i < seq.size(); i++) {
                seqBuilder.append(seq.get(i));
                if (i < seq.size() - 1) seqBuilder.append(",");
            }
            
            config.set(path + ".cooldownSequence", seqBuilder.toString());
            config.set(path + ".cooldownIndex", region.getCooldownIndex());
            config.set(path + ".maxCapacity", region.getMaxCapacity());
            config.set(path + ".minCapacity", region.getMinCapacity());
            config.set(path + ".cooldownEndTime", region.getCooldownEndTime());
            config.set(path + ".mimicEnabled", region.isMimicEnabled());
            config.set(path + ".mimicChance", region.getMimicChance());
            config.set(path + ".announcementRadius", region.getAnnouncementRadius());
            config.set(path + ".beamColor", region.getBeamColor());
            config.set(path + ".alarmSound", region.getAlarmSound());

            if (region.getDropWorld() != null) {
                config.set(path + ".drop.world", region.getDropWorld());
                config.set(path + ".drop.minX", region.getDropMinX());
                config.set(path + ".drop.maxX", region.getDropMaxX());
                config.set(path + ".drop.minY", region.getDropMinY());
                config.set(path + ".drop.maxY", region.getDropMaxY());
                config.set(path + ".drop.minZ", region.getDropMinZ());
                config.set(path + ".drop.maxZ", region.getDropMaxZ());
            } else {
                config.set(path + ".drop", null);
            }
            
            if (region.getExtractionSpawnWorld() != null) {
                config.set(path + ".extractionSpawn.world", region.getExtractionSpawnWorld());
                config.set(path + ".extractionSpawn.x", region.getExtractionSpawnX());
                config.set(path + ".extractionSpawn.y", region.getExtractionSpawnY());
                config.set(path + ".extractionSpawn.z", region.getExtractionSpawnZ());
                config.set(path + ".extractionSpawn.yaw", region.getExtractionSpawnYaw());
                config.set(path + ".extractionSpawn.pitch", region.getExtractionSpawnPitch());
            } else {
                config.set(path + ".extractionSpawn", null);
            }
            
            java.util.List<Integer> dSeq = region.getPossibleDurations();
            StringBuilder dSeqBuilder = new StringBuilder();
            for (int i = 0; i < dSeq.size(); i++) {
                dSeqBuilder.append(dSeq.get(i));
                if (i < dSeq.size() - 1) dSeqBuilder.append(",");
            }
            config.set(path + ".possibleDurations", dSeqBuilder.toString());
            
            config.set(path + ".slowFallingSeconds", region.getSlowFallingSeconds());
            config.set(path + ".blindnessSeconds", region.getBlindnessSeconds());

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
