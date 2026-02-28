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
    private final Map<UUID, String> timerConfiguringPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, String> conduitSelectingPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, String> promptStates = new ConcurrentHashMap<>();

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
                int cx = inst.getX();
                int cz = inst.getZ();
                if (cx >= region.getMinX() && cx <= region.getMaxX() &&
                    cz >= region.getMinZ() && cz <= region.getMaxZ()) {
                    chestsToReplenish.add(inst);
                }
            }
        }
        
        if (chestsToReplenish.isEmpty()) return 0;

        new org.bukkit.scheduler.BukkitRunnable() {
            int index = 0;
            final int BATCH_SIZE = plugin.getConfig().getInt("region.replenish-batch-size", 5);

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
        return chestsToReplenish.size();
    }

    public void addTimerConfiguringPlayer(UUID uuid, String regionId) {
        timerConfiguringPlayers.put(uuid, regionId);
    }

    public void removeTimerConfiguringPlayer(UUID uuid) {
        timerConfiguringPlayers.remove(uuid);
    }

    public boolean isTimerConfiguringPlayer(UUID uuid) {
        return timerConfiguringPlayers.containsKey(uuid);
    }

    public String getTimerConfiguringRegionId(UUID uuid) {
        return timerConfiguringPlayers.get(uuid);
    }
    
    public void addConduitSelectingPlayer(UUID uuid, String regionId) {
        conduitSelectingPlayers.put(uuid, regionId);
    }

    public void removeConduitSelectingPlayer(UUID uuid) {
        conduitSelectingPlayers.remove(uuid);
    }

    public boolean isConduitSelectingPlayer(UUID uuid) {
        return conduitSelectingPlayers.containsKey(uuid);
    }

    public String getConduitSelectingRegionId(UUID uuid) {
        return conduitSelectingPlayers.get(uuid);
    }
    
    // Generic Prompt States (e.g., "extrc_cooldown_myregion")
    public void addPromptState(UUID uuid, String state) {
        promptStates.put(uuid, state);
    }

    public void removePromptState(UUID uuid) {
        promptStates.remove(uuid);
    }

    public boolean hasPromptState(UUID uuid) {
        return promptStates.containsKey(uuid);
    }

    public String getPromptState(UUID uuid) {
        return promptStates.get(uuid);
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

