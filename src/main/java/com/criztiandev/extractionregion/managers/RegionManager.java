package com.criztiandev.extractionregion.managers;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionSelection;
import com.criztiandev.extractionregion.models.SavedRegion;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private final Map<String, List<SavedRegion>> regionsByWorld = new ConcurrentHashMap<>();
    private final Map<UUID, RegionType> creatingPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, String> timerConfiguringPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, String> conduitSelectingPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, String> promptStates = new ConcurrentHashMap<>();
    private final Set<UUID> activeDropZonePlayers = ConcurrentHashMap.newKeySet();

    public RegionManager(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.getStorageProvider().loadAllRegions().thenAccept(list -> {
            regions.clear();
            regionsByWorld.clear();
            
            for (SavedRegion r : list) {
                regions.put(r.getId().toLowerCase(), r);
                regionsByWorld.computeIfAbsent(r.getWorld().toLowerCase(), k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(r);
            }
            plugin.getLogger().info("Loaded " + regions.size() + " saved regions.");
        });
    }

    public java.util.List<SavedRegion> getRegionsInWorld(String worldName) {
        return regionsByWorld.getOrDefault(worldName.toLowerCase(), java.util.Collections.emptyList());
    }

    public Collection<SavedRegion> getRegions() {
        return regions.values();
    }

    public SavedRegion getRegion(String id) {
        return regions.get(id.toLowerCase());
    }

    public SavedRegion getRegionAt(org.bukkit.Location location) {
        if (location.getWorld() == null) return null;
        
        java.util.List<SavedRegion> worldRegions = getRegionsInWorld(location.getWorld().getName());
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        for (SavedRegion region : worldRegions) {
            if (x >= region.getMinX() && x <= region.getMaxX() &&
                y >= region.getMinY() && y <= region.getMaxY() &&
                z >= region.getMinZ() && z <= region.getMaxZ()) {
                return region;
            }
        }
        return null;
    }

    public void saveRegion(SavedRegion region) {
        SavedRegion old = regions.put(region.getId().toLowerCase(), region);
        if (old != null && !old.getWorld().equalsIgnoreCase(region.getWorld())) {
            // World changed, remove from old world list
            java.util.List<SavedRegion> oldList = regionsByWorld.get(old.getWorld().toLowerCase());
            if (oldList != null) oldList.remove(old);
        }
        
        java.util.List<SavedRegion> worldList = regionsByWorld.computeIfAbsent(region.getWorld().toLowerCase(), k -> new java.util.concurrent.CopyOnWriteArrayList<>());
        if (!worldList.contains(region)) {
             worldList.add(region);
        }
        
        com.criztiandev.extractionregion.tasks.SelectionVisualizerTask vt = plugin.getVisualizerTask();
        if (vt != null) {
            vt.invalidateCache(region.getId());
        }
        
        plugin.getStorageProvider().saveRegion(region);
    }
    
    public void deleteRegion(String id) {
        SavedRegion region = regions.remove(id.toLowerCase());
        if (region != null) {
            java.util.List<SavedRegion> worldList = regionsByWorld.get(region.getWorld().toLowerCase());
            if (worldList != null) {
                worldList.remove(region);
            }
        }
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
                // No need to update regionsByWorld as the instance reference doesn't change and ID is not the key there.
            }
            return success;
        });
    }
    
    public int forceReplenish(SavedRegion region) {
        java.util.List<com.criztiandev.extractionchest.models.ChestInstance> chestsToReplenish = new java.util.ArrayList<>();
        // Maps instance ID -> the parentName that should be used this cycle (after shuffle/fallback)
        java.util.Map<String, String> instanceToParent = new java.util.HashMap<>();
        // Maps instance ID -> original baseParentName (so we can restore after RESPAWNING)
        java.util.Map<String, String> originalBaseNames = new java.util.HashMap<>();
        java.util.Set<String> skipInstances = new java.util.HashSet<>();

        for (com.criztiandev.extractionchest.models.ChestInstance inst : plugin.getExtractionChestApi().getChestInstanceManager().getAllInstances()) {
            if (inst.getWorld().equals(region.getWorld())) {
                int cx = inst.getX();
                int cz = inst.getZ();
                if (cx >= region.getMinX() && cx <= region.getMaxX() &&
                    cz >= region.getMinZ() && cz <= region.getMaxZ()) {
                    chestsToReplenish.add(inst);
                    originalBaseNames.put(inst.getId(), inst.getBaseParentName());
                }
            }
        }

        if (chestsToReplenish.isEmpty()) return 0;

        if (region.isShuffleChests()) {
            // Step 1: Collect non-stationary chests and shuffle their parentName assignments
            java.util.List<com.criztiandev.extractionchest.models.ChestInstance> shuffleable = new java.util.ArrayList<>();
            java.util.List<String> parentNames = new java.util.ArrayList<>();

            for (com.criztiandev.extractionchest.models.ChestInstance inst : chestsToReplenish) {
                if (!inst.isStationary()) {
                    shuffleable.add(inst);
                    parentNames.add(inst.getBaseParentName()); // use base (the persistent name)
                }
            }

            java.util.Collections.shuffle(parentNames);
            for (int i = 0; i < shuffleable.size(); i++) {
                instanceToParent.put(shuffleable.get(i).getId(), parentNames.get(i));
            }
        }

        // Step 2: Apply spawn-chance / fallback rolling on ALL chests
        for (com.criztiandev.extractionchest.models.ChestInstance inst : chestsToReplenish) {
            String assignedParent;
            if (inst.isStationary() || !region.isShuffleChests()) {
                assignedParent = inst.getBaseParentName();
            } else {
                assignedParent = instanceToParent.getOrDefault(inst.getId(), inst.getBaseParentName());
            }

            int rng = java.util.concurrent.ThreadLocalRandom.current().nextInt(100) + 1;

            if (rng > inst.getSpawnChance()) {
                String fallback = inst.getFallbackParentName();
                if (fallback == null || fallback.isEmpty()) {
                    skipInstances.add(inst.getId());
                    inst.setState(com.criztiandev.extractionchest.models.ChestState.EMPTY);
                    plugin.getExtractionChestApi().getHologramManager().deleteHologram(inst);
                    org.bukkit.World w = org.bukkit.Bukkit.getWorld(inst.getWorld());
                    if (w != null) w.getBlockAt(inst.getX(), inst.getY(), inst.getZ()).setType(org.bukkit.Material.AIR);
                    continue;
                } else {
                    assignedParent = fallback;
                }
            }

            instanceToParent.put(inst.getId(), assignedParent);
            inst.setState(com.criztiandev.extractionchest.models.ChestState.EMPTY);
            plugin.getExtractionChestApi().getHologramManager().deleteHologram(inst);
        }

        new org.bukkit.scheduler.BukkitRunnable() {
            int index = 0;
            final int BATCH_SIZE = plugin.getConfig().getInt("region.replenish-batch-size", 5);

            @Override
            public void run() {
                for (int i = 0; i < BATCH_SIZE && index < chestsToReplenish.size(); i++, index++) {
                    com.criztiandev.extractionchest.models.ChestInstance inst = chestsToReplenish.get(index);

                    if (skipInstances.contains(inst.getId())) continue;

                    String shuffledParent = instanceToParent.getOrDefault(inst.getId(), inst.getBaseParentName());
                    String originalBase = originalBaseNames.getOrDefault(inst.getId(), inst.getBaseParentName()); // Re-added this line
                    inst.setBaseParentName(shuffledParent); // Moved this line up

                    plugin.getExtractionChestApi().getChestInstanceManager().changeState(inst, com.criztiandev.extractionchest.models.ChestState.RESPAWNING);

                    plugin.getExtractionChestApi().getChestInstanceManager().addInstance(inst);
                        
                    // Fix for TPS Lag: Save async
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        plugin.getExtractionChestApi().getStorageProvider().saveInstance(inst);
                    });

                    inst.setBaseParentName(originalBase); // Re-added this line
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

    public void addActiveDropZonePlayer(UUID uuid) {
        activeDropZonePlayers.add(uuid);
    }

    public void removeActiveDropZonePlayer(UUID uuid) {
        activeDropZonePlayers.remove(uuid);
    }

    public boolean isActiveDropZonePlayer(UUID uuid) {
        return activeDropZonePlayers.contains(uuid);
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

