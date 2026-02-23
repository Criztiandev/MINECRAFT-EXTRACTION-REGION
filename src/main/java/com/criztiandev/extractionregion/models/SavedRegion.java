package com.criztiandev.extractionregion.models;

import java.util.HashMap;
import java.util.Map;

public class SavedRegion {
    private final String id;
    private final String world;
    private final int minX, maxX, minZ, maxZ;
    private final Map<String, Integer> autoSpawns;

    public SavedRegion(String id, String world, int minX, int maxX, int minZ, int maxZ) {
        this.id = id;
        this.world = world;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.autoSpawns = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getWorld() {
        return world;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public Map<String, Integer> getAutoSpawns() {
        return autoSpawns;
    }

    public void setAutoSpawn(String definitionName, int amount) {
        if (amount <= 0) {
            autoSpawns.remove(definitionName);
        } else {
            autoSpawns.put(definitionName, amount);
        }
    }
    
    public int getAutoSpawnAmount(String definitionName) {
        return autoSpawns.getOrDefault(definitionName, 0);
    }
    
    // Convert to RegionSelection to reuse LocationUtils algorithms
    public RegionSelection toRegionSelection() {
        RegionSelection selection = new RegionSelection();
        org.bukkit.World bukkitWorld = org.bukkit.Bukkit.getWorld(world);
        if (bukkitWorld != null) {
            selection.setPos1(new org.bukkit.Location(bukkitWorld, minX, 0, minZ));
            selection.setPos2(new org.bukkit.Location(bukkitWorld, maxX, 0, maxZ));
        }
        return selection;
    }
}

