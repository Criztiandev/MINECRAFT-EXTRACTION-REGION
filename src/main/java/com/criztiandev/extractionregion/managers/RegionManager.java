package com.criztiandev.extractionregion.managers;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionSelection;
import com.criztiandev.extractionregion.models.SavedRegion;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RegionManager {

    private final ExtractionRegionPlugin plugin;
    private final Map<UUID, RegionSelection> selections = new HashMap<>();
    private final Map<String, SavedRegion> regions = new ConcurrentHashMap<>();

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

    public void saveRegion(SavedRegion region) {
        regions.put(region.getId().toLowerCase(), region);
        plugin.getStorageProvider().saveRegion(region);
    }
    
    public void deleteRegion(String id) {
        regions.remove(id.toLowerCase());
        plugin.getStorageProvider().deleteRegion(id);
    }
    
    public int runAutoSpawns(SavedRegion region) {
        // Destroy existing chests in this exact region
        for (com.criztiandev.extractionchest.models.ChestInstance inst : plugin.getExtractionChestApi().getChestInstanceManager().getAllInstances()) {
            if (inst.getWorld().equals(region.getWorld())) {
                org.bukkit.Location loc = inst.getLocation(org.bukkit.Bukkit.getWorld(inst.getWorld()));
                if (loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX() &&
                    loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ()) {
                    plugin.getExtractionChestApi().getChestInstanceManager().removeInstance(inst.getId());
                }
            }
        }

        // Spawn new chests
        int successCount = 0;
        RegionSelection selection = region.toRegionSelection();
        for (Map.Entry<String, Integer> entry : region.getAutoSpawns().entrySet()) {
            com.criztiandev.extractionchest.models.ParentChestDefinition def = plugin.getExtractionChestApi().getLootTableManager().getDefinition(entry.getKey());
            if (def == null) continue;
            
            for (int i = 0; i < entry.getValue(); i++) {
                org.bukkit.Location spawnLoc = com.criztiandev.extractionregion.utils.RegionLocationUtils.getRandomSafeLocation(selection, 20);
                if (spawnLoc != null) {
                    com.criztiandev.extractionchest.models.ChestInstance instance = new com.criztiandev.extractionchest.models.ChestInstance(UUID.randomUUID().toString(), def.getName(), spawnLoc.getWorld().getName(), spawnLoc.getBlockX(), spawnLoc.getBlockY(), spawnLoc.getBlockZ(), com.criztiandev.extractionchest.models.ChestState.READY, null);
                    instance.setActiveInventory(plugin.getExtractionChestApi().getLootTableManager().rollLoot(def));
                    plugin.getExtractionChestApi().getChestInstanceManager().addInstance(instance);
                    successCount++;
                }
            }
        }
        return successCount;
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
}

