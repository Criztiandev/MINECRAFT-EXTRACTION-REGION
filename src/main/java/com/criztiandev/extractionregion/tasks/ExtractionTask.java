package com.criztiandev.extractionregion.tasks;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionType;
import com.criztiandev.extractionregion.models.SavedRegion;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExtractionTask extends BukkitRunnable {

    private final ExtractionRegionPlugin plugin;
    // Map of PlayerUUID to the time they started extracting (in milliseconds)
    private final Map<UUID, ExtractionSession> sessions = new HashMap<>();

    public ExtractionTask(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Find if player is in an active extraction region
            SavedRegion activeRegion = null;
            for (SavedRegion region : plugin.getRegionManager().getRegions()) {
                if (region.getType() == RegionType.EXTRACTION && region.getConduitLocation() != null) {
                    Location pLoc = player.getLocation();
                    if (pLoc.getWorld().getName().equals(region.getWorld())) {
                        if (pLoc.getBlockX() >= region.getMinX() && pLoc.getBlockX() <= region.getMaxX() &&
                            pLoc.getBlockZ() >= region.getMinZ() && pLoc.getBlockZ() <= region.getMaxZ()) {
                            activeRegion = region;
                            break;
                        }
                    }
                }
            }

            if (activeRegion == null) {
                cancelSession(player);
                continue;
            }

            // Check if looking at conduit
            org.bukkit.block.Block targetBlock = player.getTargetBlockExact(5);
            boolean lookingAtConduit = targetBlock != null && 
                                     targetBlock.getLocation().equals(activeRegion.getConduitLocation());

            // If region is on cooldown
            if (activeRegion.isOnCooldown()) {
                if (lookingAtConduit) {
                    long remaining = (activeRegion.getCooldownEndTime() - now) / 1000;
                    sendActionBar(player, "§cExtraction point is on cooldown for " + remaining + "s");
                }
                cancelSession(player);
                continue;
            }

            if (lookingAtConduit) {
                if (!sessions.containsKey(player.getUniqueId())) {
                    // Start new session
                    sessions.put(player.getUniqueId(), new ExtractionSession(now, player.getLocation(), activeRegion));
                    Bukkit.broadcastMessage("§8[§c!§8] §e⚠️ An extraction has been initiated at §b" + activeRegion.getId() + "§e!");
                } else {
                    // Update session
                    ExtractionSession session = sessions.get(player.getUniqueId());
                    
                    // Cancel if moved too much
                    if (session.startLoc.distanceSquared(player.getLocation()) > 1.0) {
                        sendActionBar(player, "§cExtraction cancelled: You moved!");
                        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f);
                        cancelSession(player);
                        continue;
                    }

                    long elapsed = now - session.startTime;
                    int requiredTime = 5000; // 5 seconds to extract
                    
                    if (elapsed >= requiredTime) {
                        // Extract successful
                        executeExtraction(player, activeRegion);
                        cancelSession(player);
                    } else {
                        // Progress
                        int secondsLeft = (int) Math.ceil((requiredTime - elapsed) / 1000.0);
                        sendActionBar(player, "§aExtracting in " + secondsLeft + "s... §7(Do not move)");
                        
                        // Heartbeat sound & particles
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f + (elapsed / 5000f));
                        player.getWorld().spawnParticle(org.bukkit.Particle.DRAGON_BREATH, player.getLocation().add(0, 1, 0), 2, 0.5, 0.5, 0.5, 0.05);
                    }
                }
            } else {
                if (sessions.containsKey(player.getUniqueId())) {
                    sendActionBar(player, "§cExtraction cancelled: Looked away from conduit!");
                    player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f);
                    cancelSession(player);
                }
            }
        }
    }

    private void executeExtraction(Player player, SavedRegion region) {
        // TP to spawn
        Location spawn = player.getWorld().getSpawnLocation();
        player.teleport(spawn);
        player.sendMessage("§aExtraction successful!");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

        // Capacity logic
        int currentCap = region.getCurrentCapacity();
        currentCap--;
        
        // Mimic check
        if (region.isMimicEnabled() && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.05) {
            // It's a trap! Explode at extraction point
            region.getConduitLocation().getWorld().createExplosion(region.getConduitLocation(), 4f, false, false);
            Bukkit.broadcastMessage("§8[§c!§8] §cThe extraction point at §b" + region.getId() + " §cwas a Mimic Trap!");
        }
        
        if (currentCap <= 0) {
            // Cooldown triggers immediately
            region.setCooldownEndTime(System.currentTimeMillis() + ((long) region.getCooldownMinutes() * 60 * 1000));
            region.setCurrentCapacity(-1); // Resets capacity roll
            plugin.getRegionManager().saveRegion(region); // persist
            Bukkit.broadcastMessage("§8[§c!§8] §cExtraction point §b" + region.getId() + " §chas departed!");
        } else {
            region.setCurrentCapacity(currentCap);
        }
    }

    private void cancelSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    public void cancelExtractionByDamage(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            ExtractionSession session = sessions.remove(player.getUniqueId());
            sendActionBar(player, "§cExtraction cancelled by combat!");
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f);
        }
    }

    private static class ExtractionSession {
        long startTime;
        Location startLoc;
        SavedRegion region;

        public ExtractionSession(long startTime, Location startLoc, SavedRegion region) {
            this.startTime = startTime;
            this.startLoc = startLoc;
            this.region = region;
        }
    }
}
