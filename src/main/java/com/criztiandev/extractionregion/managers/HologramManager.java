package com.criztiandev.extractionregion.managers;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionType;
import com.criztiandev.extractionregion.models.SavedRegion;
import com.criztiandev.extractionregion.utils.TimeUtil;
import com.criztiandev.extractionregion.tasks.ExtractionTask;
import com.criztiandev.extractionregion.tasks.ExtractionTask.ExtractionSession;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final ExtractionRegionPlugin plugin;
    private final Map<String, List<ArmorStand>> activeHolograms = new ConcurrentHashMap<>();

    public HologramManager(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateHolograms() {
        for (SavedRegion region : plugin.getRegionManager().getRegions()) {
            if (region.getType() != RegionType.EXTRACTION || region.getConduitLocation() == null) {
                removeHologram(region.getId());
                continue;
            }

            List<String> lines = getLinesForRegion(region);
            List<ArmorStand> stands = activeHolograms.get(region.getId());

            if (stands == null || stands.size() != lines.size()) {
                // Recreate from scratch if size mismatch or doesn't exist
                removeHologram(region.getId());
                spawnHologram(region, lines);
            } else {
                // Update existing text
                for (int i = 0; i < lines.size(); i++) {
                    ArmorStand stand = stands.get(i);
                    if (stand.isValid()) {
                        stand.setCustomName(lines.get(i));
                    } else {
                        // If a stand was destroyed by external forces, trigger a full recreate next tick
                        removeHologram(region.getId());
                        break;
                    }
                }
            }
        }
    }

    private List<String> getLinesForRegion(SavedRegion region) {
        String title = "§e§l" + region.getId().toUpperCase();
        String capacity = "§7Capacity: §f" + region.getCurrentCapacity() + " Players";
        
        if (region.isOnCooldown()) {
            long remaining = region.getCooldownEndTime() - System.currentTimeMillis();
            return Arrays.asList(
                title,
                "§c§lCooldown: §f" + TimeUtil.formatDuration(remaining),
                capacity
            );
        } else {
            // Check if anyone is currently extracting
            ExtractionTask task = plugin.getExtractionTask();
            boolean isExtracting = false;
            long extractRemaining = 0;
            
            if (task != null) {
                for (ExtractionSession session : task.getSessions().values()) {
                    if (session.getRegion().getId().equals(region.getId())) {
                        isExtracting = true;
                        long elapsed = System.currentTimeMillis() - session.getStartTime();
                        long requiredTime = session.getTargetDurationSeconds() * 1000L;
                        extractRemaining = Math.max(0, requiredTime - elapsed);
                        break;
                    }
                }
            }

            if (isExtracting) {
                return Arrays.asList(
                    title,
                    "§a§lExtracting: §f" + TimeUtil.formatDuration(extractRemaining),
                    capacity
                );
            } else {
                return Arrays.asList(
                    title,
                    "§a§lReady to Extract",
                    capacity
                );
            }
        }
    }

    private void spawnHologram(SavedRegion region, List<String> lines) {
        Location baseLoc = region.getConduitLocation().clone().add(0.5, 1.2 + (lines.size() * 0.25), 0.5);
        List<ArmorStand> spawned = new ArrayList<>();

        for (String line : lines) {
            if (baseLoc.getWorld() != null) {
                ArmorStand stand = baseLoc.getWorld().spawn(baseLoc, ArmorStand.class, as -> {
                    as.setVisible(false);
                    as.setMarker(true);
                    as.setGravity(false);
                    as.setPersistent(false);
                    as.setCustomName(line);
                    as.setCustomNameVisible(true);
                });
                spawned.add(stand);
            }
            baseLoc.subtract(0, 0.25, 0);
        }

        activeHolograms.put(region.getId(), spawned);
    }

    public void removeHologram(String regionId) {
        List<ArmorStand> stands = activeHolograms.remove(regionId);
        if (stands != null) {
            for (ArmorStand stand : stands) {
                if (stand.isValid()) {
                    stand.remove();
                }
            }
        }
    }

    public void removeAll() {
        for (List<ArmorStand> stands : activeHolograms.values()) {
            for (ArmorStand stand : stands) {
                if (stand.isValid()) {
                    stand.remove();
                }
            }
        }
        activeHolograms.clear();
    }
}
