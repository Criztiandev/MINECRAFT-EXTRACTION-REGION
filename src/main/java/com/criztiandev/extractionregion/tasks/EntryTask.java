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
            if (loc.getWorld() == null) continue;
            
            java.util.List<SavedRegion> worldRegions = plugin.getRegionManager().getRegionsInWorld(loc.getWorld().getName());
            
            for (SavedRegion region : worldRegions) {
                if (region.getType() != RegionType.ENTRY_REGION) continue;
                
                // If Drop target is not set, we do nothing to prevent errors.
                if (region.getDropWorld() == null || region.getDropWorld().isEmpty()) continue;

                if (isInsideEntryBounds(loc, region)) {
                    // Check if region is enabled
                    if (!region.isEntryEnabled()) {
                        sendActionBar(player, "§cThis Drop Zone is currently under maintenance!");
                        String cmd = region.getEntryFallbackCommand().replace("%player%", player.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                        break;
                    }
                    
                    // Check if player is on cooldown
                    long now = System.currentTimeMillis();
                    java.util.UUID uuid = player.getUniqueId();
                    if (region.getPlayerEntryCooldowns().containsKey(uuid)) {
                        long expire = region.getPlayerEntryCooldowns().get(uuid);
                        if (now < expire) {
                            long remainingSeconds = (expire - now) / 1000;
                            sendActionBar(player, "§cYou cannot enter the Drop Zone for another " + remainingSeconds + "s");
                            
                            // Dispatch fallback command
                            String cmd = region.getEntryFallbackCommand().replace("%player%", player.getName());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                            
                            break;
                        }
                    }

                    boolean success = processEntrySequence(player, region);
                    if (success) {
                        // Start cooldown
                        long cooldownMs = region.getEntryCooldownMinutes() * 60 * 1000L;
                        if (cooldownMs > 0) {
                            region.getPlayerEntryCooldowns().put(uuid, now + cooldownMs);
                        }
                        
                        // Add to Active Drop Zone tracking for Slow Falling removal
                        plugin.getRegionManager().addActiveDropZonePlayer(uuid);
                    }
                    
                    // Only process one entry region per tick to prevent chaining if overlapping
                    break;
                }
            }
        }
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
    }

    // Extracted for unit testing
    public boolean isInsideEntryBounds(Location loc, SavedRegion region) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(region.getWorld())) {
            return false;
        }
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        
        return x >= Math.min(region.getMinX(), region.getMaxX()) && 
               x <= Math.max(region.getMinX(), region.getMaxX()) &&
               y >= Math.min(region.getMinY(), region.getMaxY()) &&
               y <= Math.max(region.getMinY(), region.getMaxY()) &&
               z >= Math.min(region.getMinZ(), region.getMaxZ()) && 
               z <= Math.max(region.getMinZ(), region.getMaxZ());
    }

    public boolean processEntrySequence(Player player, SavedRegion region) {
        World dropWorld = Bukkit.getWorld(region.getDropWorld());
        if (dropWorld == null) {
            plugin.getLogger().warning("Entry Region " + region.getId() + " points to a null/unloaded World: " + region.getDropWorld());
            return false;
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
        return true;
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
