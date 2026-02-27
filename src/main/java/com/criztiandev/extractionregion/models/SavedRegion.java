package com.criztiandev.extractionregion.models;

import java.util.HashMap;
import java.util.Map;

public class SavedRegion {
    private String id;
    private final String world;
    private final int minX, maxX, minZ, maxZ;

    private long nextResetTime;
    private int resetIntervalMinutes;
    private transient int lastResetMinuteOfDay = -1;

    public SavedRegion(String id, String world, int minX, int maxX, int minZ, int maxZ) {
        this.id = id;
        this.world = world;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.nextResetTime = 0;
        this.resetIntervalMinutes = 360; // Default 6 hours
    }

    public long getNextResetTime() {
        return nextResetTime;
    }

    public void setNextResetTime(long nextResetTime) {
        this.nextResetTime = nextResetTime;
    }

    public int getResetIntervalMinutes() {
        return resetIntervalMinutes;
    }

    public void setResetIntervalMinutes(int resetIntervalMinutes) {
        this.resetIntervalMinutes = resetIntervalMinutes;
    }

    public int getLastResetMinuteOfDay() {
        return lastResetMinuteOfDay;
    }

    public void setLastResetMinuteOfDay(int lastResetMinuteOfDay) {
        this.lastResetMinuteOfDay = lastResetMinuteOfDay;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

