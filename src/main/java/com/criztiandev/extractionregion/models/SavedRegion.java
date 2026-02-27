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

    // EXTRACTION REGION FIELDS
    private RegionType type = RegionType.CHEST_REPLENISH;
    private org.bukkit.Location conduitLocation;
    private int cooldownMinutes = 10;
    private int maxCapacity = 5;
    private int minCapacity = 1;
    private transient int currentCapacity = -1;
    private long cooldownEndTime = 0;
    private boolean mimicEnabled = true;

    // ENTRY REGION / DROP ZONE FIELDS
    private String dropWorld;
    private int dropMinX, dropMaxX, dropMinY, dropMaxY, dropMinZ, dropMaxZ;
    private int slowFallingSeconds = 10;
    private int blindnessSeconds = 3;

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
    
    // NEW GETTERS & SETTERS
    
    public RegionType getType() {
        return type;
    }

    public void setType(RegionType type) {
        this.type = type;
    }

    public org.bukkit.Location getConduitLocation() {
        return conduitLocation;
    }

    public void setConduitLocation(org.bukkit.Location conduitLocation) {
        this.conduitLocation = conduitLocation;
    }

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public int getMinCapacity() {
        return minCapacity;
    }

    public void setMinCapacity(int minCapacity) {
        this.minCapacity = minCapacity;
    }

    public int getCurrentCapacity() {
        if (currentCapacity == -1) {
            rollNewCapacity();
        }
        return currentCapacity;
    }

    public void setCurrentCapacity(int currentCapacity) {
        this.currentCapacity = currentCapacity;
    }

    public void rollNewCapacity() {
        if (maxCapacity <= minCapacity) {
            this.currentCapacity = minCapacity;
        } else {
            this.currentCapacity = java.util.concurrent.ThreadLocalRandom.current().nextInt(minCapacity, maxCapacity + 1);
        }
    }

    public long getCooldownEndTime() {
        return cooldownEndTime;
    }

    public void setCooldownEndTime(long cooldownEndTime) {
        this.cooldownEndTime = cooldownEndTime;
    }

    public boolean isMimicEnabled() {
        return mimicEnabled;
    }

    public void setMimicEnabled(boolean mimicEnabled) {
        this.mimicEnabled = mimicEnabled;
    }

    public boolean isOnCooldown() {
        return System.currentTimeMillis() < cooldownEndTime;
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

    // DROP ZONE GETTERS & SETTERS

    public String getDropWorld() {
        return dropWorld;
    }

    public void setDropWorld(String dropWorld) {
        this.dropWorld = dropWorld;
    }

    public int getDropMinX() {
        return dropMinX;
    }

    public void setDropMinX(int dropMinX) {
        this.dropMinX = dropMinX;
    }

    public int getDropMaxX() {
        return dropMaxX;
    }

    public void setDropMaxX(int dropMaxX) {
        this.dropMaxX = dropMaxX;
    }

    public int getDropMinY() {
        return dropMinY;
    }

    public void setDropMinY(int dropMinY) {
        this.dropMinY = dropMinY;
    }

    public int getDropMaxY() {
        return dropMaxY;
    }

    public void setDropMaxY(int dropMaxY) {
        this.dropMaxY = dropMaxY;
    }

    public int getDropMinZ() {
        return dropMinZ;
    }

    public void setDropMinZ(int dropMinZ) {
        this.dropMinZ = dropMinZ;
    }

    public int getDropMaxZ() {
        return dropMaxZ;
    }

    public void setDropMaxZ(int dropMaxZ) {
        this.dropMaxZ = dropMaxZ;
    }

    public int getSlowFallingSeconds() {
        return slowFallingSeconds;
    }

    public void setSlowFallingSeconds(int slowFallingSeconds) {
        this.slowFallingSeconds = slowFallingSeconds;
    }

    public int getBlindnessSeconds() {
        return blindnessSeconds;
    }

    public void setBlindnessSeconds(int blindnessSeconds) {
        this.blindnessSeconds = blindnessSeconds;
    }
}

