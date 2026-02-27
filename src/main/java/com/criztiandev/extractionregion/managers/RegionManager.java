package com.criztiandev.extractionregion.managers;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionSelection;
import com.criztiandev.extractionregion.models.SavedRegion;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import com.criztiandev.extractionregion.models.RegionType;

public class RegionManager {

    private final ExtractionRegionPlugin plugin;
    private final Map<UUID, RegionSelection> selections = new HashMap<>();
    private final Map<String, SavedRegion> regions = new ConcurrentHashMap<>();
    private final Map<UUID, RegionType> creatingPlayers = new ConcurrentHashMap<>();

    public RegionManager(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.getStorageProvider().loadAllRegions().thenAccept(list -> {
            regions.clear();
            for (SavedRegion r : list) {
                regions.put(r.getId().toLowerCase(), r);
            }
            plugin.getLogger().info("Loaded " + regions.size() + " saved regions.");
        });
    }

    public Collection<SavedRegion> getRegions() {
        return regions.values();
    }

    public SavedRegion getRegion(String id) {
        return regions.get(id.toLowerCase());
    }

    public SavedRegion getRegionAt(org.bukkit.Location location) {
        if (location.getWorld() == null) return null;
        for (SavedRegion region : regions.values()) {
            if (!region.getWorld().equals(location.getWorld().getName())) continue;
            int x = location.getBlockX();
            int z = location.getBlockZ();
            if (x >= region.getMinX() && x <= region.getMaxX() &&
                z >= region.getMinZ() && z <= region.getMaxZ()) {
                return region;
            }
        }
        return null;
    }

    public void saveRegion(SavedRegion region) {
        regions.put(region.getId().toLowerCase(), region);
        plugin.getStorageProvider().saveRegion(region);
    }
    
    public void deleteRegion(String id) {
        regions.remove(id.toLowerCase());
        plugin.getStorageProvider().deleteRegion(id);
    }
    
    public CompletableFuture<Boolean> renameRegion(String oldId, String newId) {
        if (regions.containsKey(newId.toLowerCase())) {
            return CompletableFuture.completedFuture(false);
        }
        
        SavedRegion region = regions.get(oldId.toLowerCase());
        if (region == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        return plugin.getStorageProvider().renameRegion(oldId, newId).thenApply(success -> {
            if (success) {
                regions.remove(oldId.toLowerCase());
                region.setId(newId);
                regions.put(newId.toLowerCase(), region);
            }
            return success;
        });
    }
    
    public int forceReplenish(SavedRegion region) {
        java.util.List<com.criztiandev.extractionchest.models.ChestInstance> chestsToReplenish = new java.util.ArrayList<>();
        for (com.criztiandev.extractionchest.models.ChestInstance inst : plugin.getExtractionChestApi().getChestInstanceManager().getAllInstances()) {
            if (inst.getWorld().equals(region.getWorld())) {
                org.bukkit.Location loc = inst.getLocation(org.bukkit.Bukkit.getWorld(inst.getWorld()));
                if (loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX() &&
                    loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ()) {
                    chestsToReplenish.add(inst);
                }
            }
        }
        
        if (chestsToReplenish.isEmpty()) return 0;

        new org.bukkit.scheduler.BukkitRunnable() {
            int index = 0;
            final int BATCH_SIZE = 5;

            @Override
            public void run() {
                for (int i = 0; i < BATCH_SIZE && index < chestsToReplenish.size(); i++, index++) {
                    com.criztiandev.extractionchest.models.ChestInstance inst = chestsToReplenish.get(index);
                    if (inst.getState() != com.criztiandev.extractionchest.models.ChestState.READY) {
                        plugin.getExtractionChestApi().getChestInstanceManager().changeState(inst, com.criztiandev.extractionchest.models.ChestState.RESPAWNING);
                    } else {
                        com.criztiandev.extractionchest.models.ParentChestDefinition def = plugin.getExtractionChestApi().getLootTableManager().getDefinition(inst.getParentName());
                        if (def != null && (inst.getActiveInventory() == null || inst.getActiveInventory().isEmpty())) {
                            inst.setActiveInventory(plugin.getExtractionChestApi().getLootTableManager().rollLoot(def));
                            plugin.getExtractionChestApi().getStorageProvider().saveInstance(inst);
                        }
                    }
                }
                if (index >= chestsToReplenish.size()) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        
        region.setNextResetTime(System.currentTimeMillis() + (region.getResetIntervalMinutes() * 60000L));
        saveRegion(region); // Save the updated timer
        return chestsToReplenish.size();
    }

    public RegionSelection getSelection(UUID uuid) {
        return selections.get(uuid);
    }

    public RegionSelection getOrCreateSelection(UUID uuid) {
        return selections.computeIfAbsent(uuid, k -> new RegionSelection());
    }

    public void removeSelection(UUID uuid) {
        selections.remove(uuid);
    }

    public void addCreatingPlayer(UUID uuid, RegionType type) {
        creatingPlayers.put(uuid, type);
    }

    public boolean isCreatingPlayer(UUID uuid) {
        return creatingPlayers.containsKey(uuid);
    }
    
    public RegionType getCreatingPlayerType(UUID uuid) {
        return creatingPlayers.get(uuid);
    }

    public void removeCreatingPlayer(UUID uuid) {
        creatingPlayers.remove(uuid);
    }
}

