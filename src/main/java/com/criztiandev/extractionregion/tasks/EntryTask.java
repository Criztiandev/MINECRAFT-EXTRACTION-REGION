package com.criztiandev.extractionregion.tasks;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionType;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

public class EntryTask implements Runnable {

    private final ExtractionRegionPlugin plugin;

    public EntryTask(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.getRegionManager() == null || plugin.getRegionManager().getRegions() == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation();
            
            for (SavedRegion region : plugin.getRegionManager().getRegions()) {
                if (region.getType() != RegionType.ENTRY_REGION) continue;
                
                // If Drop target is not set, we do nothing to prevent errors.
                if (region.getDropWorld() == null || region.getDropWorld().isEmpty()) continue;

                if (isInsideEntryBounds(loc, region)) {
                    processEntrySequence(player, region);
                    // Only process one entry region per tick to prevent chaining if overlapping
                    break;
                }
            }
        }
    }

    // Extracted for unit testing
    public boolean isInsideEntryBounds(Location loc, SavedRegion region) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(region.getWorld())) {
            return false;
        }
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        
        return x >= Math.min(region.getMinX(), region.getMaxX()) && 
               x <= Math.max(region.getMinX(), region.getMaxX()) &&
               z >= Math.min(region.getMinZ(), region.getMaxZ()) && 
               z <= Math.max(region.getMinZ(), region.getMaxZ());
    }

    public void processEntrySequence(Player player, SavedRegion region) {
        World dropWorld = Bukkit.getWorld(region.getDropWorld());
        if (dropWorld == null) {
            plugin.getLogger().warning("Entry Region " + region.getId() + " points to a null/unloaded World: " + region.getDropWorld());
            return;
        }

        double[] coords = calculateDropCoordinates(region.getDropMinX(), region.getDropMaxX(), 
                                                   region.getDropMinY(), region.getDropMaxY(), 
                                                   region.getDropMinZ(), region.getDropMaxZ());

        Location dropLocation = new Location(dropWorld, coords[0], coords[1], coords[2], player.getLocation().getYaw(), player.getLocation().getPitch());

        // Perform Warp
        player.teleport(dropLocation);
        player.playSound(dropLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        
        // Potion Effects
        if (region.getSlowFallingSeconds() > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, region.getSlowFallingSeconds() * 20, 0, false, false, true));
        }
        if (region.getBlindnessSeconds() > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, region.getBlindnessSeconds() * 20, 0, false, false, true));
        }

        // Send Title so they feel immersed
        player.sendTitle("§c§lWARZONE", "§7Entering the Drop Zone...", 10, 40, 20);
    }

    // Pure math function for strict JUnit isolated testing without Bukkit environments
    public double[] calculateDropCoordinates(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        int lowX = Math.min(minX, maxX);
        int highX = Math.max(minX, maxX);
        int lowY = Math.min(minY, maxY);
        int highY = Math.max(minY, maxY);
        int lowZ = Math.min(minZ, maxZ);
        int highZ = Math.max(minZ, maxZ);

        double rX = lowX == highX ? lowX + 0.5 : random.nextInt(lowX, highX + 1) + 0.5;
        double rY = lowY == highY ? lowY : random.nextInt(lowY, highY + 1);
        double rZ = lowZ == highZ ? lowZ + 0.5 : random.nextInt(lowZ, highZ + 1) + 0.5;

        return new double[]{rX, rY, rZ};
    }
}
