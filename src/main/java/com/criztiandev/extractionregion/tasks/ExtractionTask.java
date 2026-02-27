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
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
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

        // Use a safe iterator since we might remove from sessions
        java.util.Iterator<Map.Entry<UUID, ExtractionSession>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ExtractionSession> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            ExtractionSession session = entry.getValue();
            
            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }

            // Cancel if area is on cooldown
            if (session.region.isOnCooldown()) {
                sendActionBar(player, "§cExtraction point is on cooldown!");
                it.remove();
                continue;
            }

            // Check if player is still looking at the conduit (within 5 blocks)
            org.bukkit.block.Block targetBlock = player.getTargetBlockExact(5);
            boolean lookingAtConduit = targetBlock != null && 
                                     targetBlock.getLocation().distanceSquared(session.region.getConduitLocation()) <= 2.0;

            if (!lookingAtConduit) {
                sendActionBar(player, "§cExtraction cancelled: Looked away from conduit!");
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f);
                it.remove();
                continue;
            }
            
            // Cancel if moved too much
            if (session.startLoc.distanceSquared(player.getLocation()) > 1.0) {
                sendActionBar(player, "§cExtraction cancelled: You moved!");
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f);
                it.remove();
                continue;
            }

            long elapsed = now - session.startTime;
            int requiredTime = 5000; // 5 seconds to extract

            if (elapsed >= requiredTime) {
                // Extract successful
                it.remove();
                executeExtraction(player, session.region);
            } else {
                // Progress
                int secondsLeft = (int) Math.ceil((requiredTime - elapsed) / 1000.0);
                sendActionBar(player, "§aExtracting in " + secondsLeft + "s... §7(Keep looking & don't move)");
                
                // Heartbeat sound & particles
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f + (elapsed / 5000f));
                player.getWorld().spawnParticle(org.bukkit.Particle.DRAGON_BREATH, player.getLocation().add(0, 1, 0), 2, 0.5, 0.5, 0.5, 0.05);
            }
        }
    }

    public void handleButtonPress(Player player, SavedRegion region) {
        long now = System.currentTimeMillis();
        
        if (region.isOnCooldown()) {
            long remaining = (region.getCooldownEndTime() - now) / 1000;
            player.sendMessage("§cExtraction point is on cooldown for " + remaining + "s");
            return;
        }
        
        if (sessions.containsKey(player.getUniqueId())) {
            // Already extracting
            return;
        }
        
        sessions.put(player.getUniqueId(), new ExtractionSession(now, player.getLocation(), region));
        
        // Announce to everyone in the chunk/region
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isInRegion(p.getLocation(), region)) {
                p.sendMessage("§8[§c!§8] §e⚠️ §aAn extraction has been initiated! §eStand by...");
                sendActionBar(p, "§a" + player.getName() + " is extracting us!");
            }
        }
    }
    
    private boolean isInRegion(Location loc, SavedRegion region) {
        if (!loc.getWorld().getName().equals(region.getWorld())) return false;
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        return x >= region.getMinX() && x <= region.getMaxX() &&
               z >= region.getMinZ() && z <= region.getMaxZ();
    }

    private void executeExtraction(Player activator, SavedRegion region) {
        // Find configured spawn
        String w = plugin.getConfig().getString("extraction.spawn.world");
        Location spawn = null;
        if (w != null) {
            org.bukkit.World world = Bukkit.getWorld(w);
            if (world != null) {
                double x = plugin.getConfig().getDouble("extraction.spawn.x");
                double y = plugin.getConfig().getDouble("extraction.spawn.y");
                double z = plugin.getConfig().getDouble("extraction.spawn.z");
                float yaw = (float) plugin.getConfig().getDouble("extraction.spawn.yaw");
                float pitch = (float) plugin.getConfig().getDouble("extraction.spawn.pitch");
                spawn = new Location(world, x, y, z, yaw, pitch);
            }
        }
        
        if (spawn == null) {
            spawn = activator.getWorld().getSpawnLocation(); // Fallback
        }

        // Spawn Fireworks
        spawnFireworks(region.getConduitLocation());

        int extractedCount = 0;
        // Teleport everyone inside the extraction zone
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isInRegion(p.getLocation(), region)) {
                p.teleport(spawn);
                p.sendMessage("§aExtraction successful!");
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                extractedCount++;
                
                // Clear their session if they had one just in case
                sessions.remove(p.getUniqueId());
            }
        }

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
    
    private void spawnFireworks(Location loc) {
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc.add(0, 1, 0), EntityType.FIREWORK_ROCKET);
        FireworkMeta fwm = fw.getFireworkMeta();
        
        // Setup firework colors
        fwm.addEffect(FireworkEffect.builder()
                .withColor(Color.YELLOW, Color.ORANGE, Color.RED)
                .withFade(Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build());
        
        fwm.setPower(1);
        fw.setFireworkMeta(fwm);
    }

    private void cancelSession(Player player) {
        sessions.remove(player.getUniqueId());
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
