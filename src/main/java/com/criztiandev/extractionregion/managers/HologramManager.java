package com.criztiandev.extractionregion.managers;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionType;
import com.criztiandev.extractionregion.models.SavedRegion;
import com.criztiandev.extractionregion.utils.TimeUtil;
import com.criztiandev.extractionregion.tasks.ExtractionTask;
import com.criztiandev.extractionregion.tasks.ExtractionTask.ExtractionSession;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final ExtractionRegionPlugin plugin;
    private final Map<String, TextDisplay> activeHolograms = new ConcurrentHashMap<>();

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
            TextDisplay display = activeHolograms.get(region.getId());
            String fullText = String.join("\n", lines);

            if (display == null || !display.isValid()) {
                removeHologram(region.getId());
                
                // Prevent duplicate ghost holograms by ensuring the chunk is actually loaded
                Location cLoc = region.getConduitLocation();
                if (cLoc.getWorld() != null && cLoc.getWorld().isChunkLoaded(cLoc.getBlockX() >> 4, cLoc.getBlockZ() >> 4)) {
                    // One final global sweep in a tiny radius before spawning to eradicate old valid ghosts in unloaded chunks
                    org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "extraction_hologram");
                    for (org.bukkit.entity.Entity e : cLoc.getWorld().getNearbyEntities(cLoc, 2.0, 5.0, 2.0)) {
                        if (e instanceof TextDisplay && e.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BYTE)) {
                            e.remove();
                        }
                    }
                    spawnHologram(region, fullText);
                }
            } else {
                display.setText(fullText);
                
                // Allow dynamic updating of position and scale through the GUI
                Location baseLoc = region.getConduitLocation().clone().add(region.getHologramOffsetX(), region.getHologramOffsetY(), region.getHologramOffsetZ());
                if (display.getLocation().distanceSquared(baseLoc) > 0.01) {
                    display.teleport(baseLoc);
                }
                
                double scale = region.getHologramScale();
                Transformation t = display.getTransformation();
                if (Math.abs(t.getScale().x() - scale) > 0.01) {
                    t.getScale().set((float) scale, (float) scale, (float) scale);
                    display.setTransformation(t);
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

    private void spawnHologram(SavedRegion region, String text) {
        Location baseLoc = region.getConduitLocation().clone().add(region.getHologramOffsetX(), region.getHologramOffsetY(), region.getHologramOffsetZ());
        
        if (baseLoc.getWorld() != null) {
            TextDisplay display = baseLoc.getWorld().spawn(baseLoc, TextDisplay.class, td -> {
                td.setPersistent(false);
                td.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "extraction_hologram"), org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                td.setText(text);
                td.setAlignment(TextDisplay.TextAlignment.CENTER);
                td.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                td.setDefaultBackground(false);
                td.setBackgroundColor(org.bukkit.Color.fromARGB(100, 0, 0, 0));
                
                double scale = region.getHologramScale();
                Transformation t = td.getTransformation();
                t.getScale().set((float) scale, (float) scale, (float) scale);
                td.setTransformation(t);
            });
            activeHolograms.put(region.getId(), display);
        }
    }

    public void removeHologram(String regionId) {
        TextDisplay display = activeHolograms.remove(regionId);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    public void cleanupOldHolograms() {
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "extraction_hologram");
        
        // 1. Clean nearby rogue marker ArmorStands near conduits (legacy holograms without PDC)
        for (SavedRegion region : plugin.getRegionManager().getRegions()) {
            Location loc = region.getConduitLocation();
            if (loc != null && loc.getWorld() != null) {
                for (org.bukkit.entity.Entity entity : loc.getWorld().getNearbyEntities(loc, 10, 20, 10)) {
                    if (entity instanceof org.bukkit.entity.ArmorStand) {
                        org.bukkit.entity.ArmorStand as = (org.bukkit.entity.ArmorStand) entity;
                        if (as.isMarker() && !as.isVisible() && !as.hasGravity() && as.isCustomNameVisible()) {
                            as.remove();
                        }
                    } else if (entity instanceof TextDisplay) {
                        // Also remove any rogue text displays near the conduit just in case
                        if (entity.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BYTE)) {
                            entity.remove();
                        }
                    }
                }
            }
        }
        
        // 2. Global sweep for anything with the exact PDC key
        for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntitiesByClass(org.bukkit.entity.ArmorStand.class)) {
                if (entity.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BYTE)) {
                    entity.remove();
                }
            }
            for (org.bukkit.entity.Entity entity : world.getEntitiesByClass(TextDisplay.class)) {
                if (entity.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BYTE)) {
                    entity.remove();
                }
            }
        }
    }

    public void removeAll() {
        for (TextDisplay display : activeHolograms.values()) {
            if (display.isValid()) {
                display.remove();
            }
        }
        activeHolograms.clear();
    }
}
