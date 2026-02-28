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
    private java.util.List<Integer> cooldownSequence = new java.util.ArrayList<>(java.util.Arrays.asList(10));
    private int cooldownIndex = 0;
    private int maxCapacity = 5;
    private int minCapacity = 1;
    private transient int currentCapacity = -1;
    private long cooldownEndTime = 0;
    private boolean mimicEnabled = true;
    private int mimicChance = 5; // Default 5%
    private int announcementRadius = 100; // Radius in blocks for extraction announcements
    private String beamColor = "#FF0000"; // Default beam color
    private String alarmSound = "ENTITY_ENDER_DRAGON_GROWL"; // Default alarm sound

    // REGION SPECIFIC EXTRACTION SPAWN
    private String extractionSpawnWorld;
    private double extractionSpawnX, extractionSpawnY, extractionSpawnZ;
    private float extractionSpawnYaw, extractionSpawnPitch;

    private java.util.List<Integer> possibleDurations = new java.util.ArrayList<>(java.util.Arrays.asList(5));

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
        if (resetIntervalMinutes <= 0) return 0;
        
        java.time.ZoneId phZoneId = java.time.ZoneId.of("Asia/Manila");
        java.time.ZonedDateTime manilaTime = java.time.ZonedDateTime.now(phZoneId);
        int currentMinuteOfDay = manilaTime.getHour() * 60 + manilaTime.getMinute();
        
        if (currentMinuteOfDay % resetIntervalMinutes == 0 && lastResetMinuteOfDay != currentMinuteOfDay) {
            // It's due right now! Return a past timestamp to trigger the UI "Pending/Processing..." state
            return System.currentTimeMillis() - 1000; 
        }
        
        int minutesToNext = resetIntervalMinutes - (currentMinuteOfDay % resetIntervalMinutes);
        
        // Accurately calculate seconds until the actual clock minute flips over
        long secondsUntilNextMinute = 60 - manilaTime.getSecond();
        long totalSecondsToNext = ((minutesToNext - 1) * 60L) + secondsUntilNextMinute;
        
        return System.currentTimeMillis() + (totalSecondsToNext * 1000L);
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

    public java.util.List<Integer> getCooldownSequence() {
        if (cooldownSequence == null || cooldownSequence.isEmpty()) {
            return new java.util.ArrayList<>(java.util.Arrays.asList(10));
        }
        return cooldownSequence;
    }

    public void setCooldownSequence(java.util.List<Integer> cooldownSequence) {
        if (cooldownSequence == null || cooldownSequence.isEmpty()) {
            this.cooldownSequence = new java.util.ArrayList<>(java.util.Arrays.asList(10));
        } else {
            this.cooldownSequence = cooldownSequence;
        }
        // Normalize index
        if (cooldownIndex >= this.cooldownSequence.size()) {
            cooldownIndex = 0;
        }
    }

    public int getCooldownIndex() {
        return cooldownIndex;
    }

    public void setCooldownIndex(int cooldownIndex) {
        this.cooldownIndex = cooldownIndex;
    }

    /**
     * Retrieves the current cooldown in minutes and increments the index so the next call uses the next cooldown.
     * Use this when actually applying a cooldown after a successful extraction.
     */
    public int getAndCycleNextCooldownMinutes() {
        java.util.List<Integer> seq = getCooldownSequence();
        if (cooldownIndex >= seq.size()) {
            cooldownIndex = 0;
        }
        int currentCooldown = seq.get(cooldownIndex);
        
        cooldownIndex++;
        if (cooldownIndex >= seq.size()) {
            cooldownIndex = 0;
        }
        
        return currentCooldown;
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

    public int getAnnouncementRadius() {
        return announcementRadius;
    }

    public void setAnnouncementRadius(int announcementRadius) {
        this.announcementRadius = announcementRadius;
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
    
    public int getMimicChance() {
        return mimicChance;
    }

    public void setMimicChance(int mimicChance) {
        if (mimicChance < 0) mimicChance = 0;
        if (mimicChance > 100) mimicChance = 100;
        this.mimicChance = mimicChance;
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

    // EXTRACTION SPAWN GETTERS & SETTERS
    public org.bukkit.Location getExtractionSpawnLocation() {
        if (extractionSpawnWorld == null) return null;
        org.bukkit.World w = org.bukkit.Bukkit.getWorld(extractionSpawnWorld);
        if (w == null) return null;
        return new org.bukkit.Location(w, extractionSpawnX, extractionSpawnY, extractionSpawnZ, extractionSpawnYaw, extractionSpawnPitch);
    }
    
    public void setExtractionSpawnLocation(org.bukkit.Location loc) {
        if (loc == null) {
            this.extractionSpawnWorld = null;
            return;
        }
        this.extractionSpawnWorld = loc.getWorld().getName();
        this.extractionSpawnX = loc.getX();
        this.extractionSpawnY = loc.getY();
        this.extractionSpawnZ = loc.getZ();
        this.extractionSpawnYaw = loc.getYaw();
        this.extractionSpawnPitch = loc.getPitch();
    }
    
    public String getExtractionSpawnWorld() { return extractionSpawnWorld; }
    public double getExtractionSpawnX() { return extractionSpawnX; }
    public double getExtractionSpawnY() { return extractionSpawnY; }
    public double getExtractionSpawnZ() { return extractionSpawnZ; }
    public float getExtractionSpawnYaw() { return extractionSpawnYaw; }
    public float getExtractionSpawnPitch() { return extractionSpawnPitch; }
    
    public void setExtractionSpawnWorld(String world) { this.extractionSpawnWorld = world; }
    public void setExtractionSpawnX(double x) { this.extractionSpawnX = x; }
    public void setExtractionSpawnY(double y) { this.extractionSpawnY = y; }
    public void setExtractionSpawnZ(double z) { this.extractionSpawnZ = z; }
    public void setExtractionSpawnYaw(float yaw) { this.extractionSpawnYaw = yaw; }
    public void setExtractionSpawnPitch(float pitch) { this.extractionSpawnPitch = pitch; }

    public java.util.List<Integer> getPossibleDurations() {
        if (possibleDurations == null || possibleDurations.isEmpty()) {
            return new java.util.ArrayList<>(java.util.Arrays.asList(5));
        }
        return possibleDurations;
    }

    public void setPossibleDurations(java.util.List<Integer> possibleDurations) {
        if (possibleDurations == null || possibleDurations.isEmpty()) {
            this.possibleDurations = new java.util.ArrayList<>(java.util.Arrays.asList(5));
        } else {
            this.possibleDurations = possibleDurations;
        }
    }

    public int getRandomDurationSeconds() {
        java.util.List<Integer> durations = getPossibleDurations();
        if (durations.size() == 1) return durations.get(0);
        int randomIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(durations.size());
        return durations.get(randomIndex);
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

    public String getBeamColor() {
        return beamColor;
    }

    public void setBeamColor(String beamColor) {
        this.beamColor = beamColor;
    }

    public String getAlarmSound() {
        return alarmSound;
    }

    public void setAlarmSound(String alarmSound) {
        this.alarmSound = alarmSound;
    }
}

