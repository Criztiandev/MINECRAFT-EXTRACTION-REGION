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

    public Map<UUID, ExtractionSession> getSessions() {
        return sessions;
    }

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
                String msg = plugin.getConfig().getString("extraction.messages.cancelled_look", "&cExtraction cancelled: Looked away from conduit!");
                sendActionBar(player, msg.replace("&", "§"));
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f);
                it.remove();
                continue;
            }
            
            // Cancel if moved too much
            if (session.startLoc.distanceSquared(player.getLocation()) > 1.0) {
                String msg = plugin.getConfig().getString("extraction.messages.cancelled_move", "&cExtraction cancelled: You moved!");
                sendActionBar(player, msg.replace("&", "§"));
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f);
                it.remove();
                continue;
            }

            long elapsed = now - session.startTime;
            int requiredTime = session.targetDurationSeconds * 1000;

            if (elapsed >= requiredTime) {
                // Extract successful
                it.remove();
                executeExtraction(player, session.region);
            } else {
                // Progress
                int secondsLeft = (int) Math.ceil((requiredTime - elapsed) / 1000.0);
                String msg = plugin.getConfig().getString("extraction.messages.countdown_actionbar", "&aExtracting in %time%s... &7(Keep looking & don't move)");
                sendActionBar(player, msg.replace("%time%", String.valueOf(secondsLeft)).replace("&", "§"));
                
                // Play Alarm Sound once per second
                int elapsedSeconds = (int) (elapsed / 1000);
                if (elapsedSeconds != session.getLastAlarmSecond()) {
                    session.setLastAlarmSecond(elapsedSeconds);
                    if (plugin.getConfig().getBoolean("extraction.effects.alarm_enabled", true)) {
                        String alarmSound = session.region.getAlarmSound();
                        float alarmVolume = (float) plugin.getConfig().getDouble("extraction.effects.alarm_volume", 3.0);
                        float alarmPitch = (float) plugin.getConfig().getDouble("extraction.effects.alarm_pitch", 2.0);
                        try {
                            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.valueOf(alarmSound.toUpperCase()), alarmVolume, alarmPitch);
                        } catch (Exception ignored) {}
                    }
                }
                
                // Heartbeat sound & particles
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f + (elapsed / (float) requiredTime));
                
                // Beacon Beam effect above the conduit
                Location conduitLoc = session.region.getConduitLocation();
                if (conduitLoc != null) {
                    org.bukkit.World world = conduitLoc.getWorld();
                    Location center = conduitLoc.clone().add(0.5, 1.0, 0.5);
                    
                    String hexColor = session.region.getBeamColor();
                    java.awt.Color awtColor = java.awt.Color.RED;
                    try {
                        awtColor = java.awt.Color.decode(hexColor);
                    } catch (Exception ignored) {}
                    
                    org.bukkit.Color color = org.bukkit.Color.fromRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
                    org.bukkit.Particle.DustOptions dust = new org.bukkit.Particle.DustOptions(color, 2.0f);
                    
                    // Spawn particles upwards to simulate a solid beam
                    for (double y = 0; y < 20; y += 0.2) {
                        try {
                            world.spawnParticle(org.bukkit.Particle.DUST, center.clone().add(0, y, 0), 2, 0.1, 0.0, 0.1, 0.0, dust);
                        } catch (Exception ignored) {}
                        
                        if (y % 1.0 < 0.2) {
                            world.spawnParticle(org.bukkit.Particle.WITCH, center.clone().add(0, y, 0), 1, 0.1, 0.0, 0.1, 0.0);
                        }
                    }
                    // Ring effect around player
                    double angle = (elapsed / 200.0) % (2 * Math.PI);
                    world.spawnParticle(org.bukkit.Particle.FLAME, player.getLocation().add(Math.cos(angle), 1.0, Math.sin(angle)), 0, 0, 0, 0, 0);
                    world.spawnParticle(org.bukkit.Particle.FLAME, player.getLocation().add(Math.cos(angle + Math.PI), 1.0, Math.sin(angle + Math.PI)), 0, 0, 0, 0, 0);
                }
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
        
        // Mimic trap triggers instantly when button is pressed!
        if (region.isMimicEnabled() && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < (region.getMimicChance() / 100.0)) {
            triggerMimicTrap(region);
            return;
        }
        
        sessions.put(player.getUniqueId(), new ExtractionSession(now, player.getLocation(), region));
        
        // Announce to everyone in the chunk/region
        String startMsg = plugin.getConfig().getString("extraction.messages.start", "&8[&c!&8] &e⚠️ &aAn extraction has been initiated! &eStand by...");
        String actionMsg = plugin.getConfig().getString("extraction.messages.extracting_actionbar", "&a%player% is extracting us!");
        int radius = region.getAnnouncementRadius();
        String startAction = plugin.getConfig().getString("extraction.announcement.start_actionbar", "&cExtraction %region% | %x%, %y%, %z%");
        String startChat = plugin.getConfig().getString("extraction.announcement.start_chat", "&e[!] Extraction started at &b%region%&e! &7(%x%, %y%, %z%)");
        
        Location cLoc = region.getConduitLocation();
        String parsedStartAction = startAction.replace("%region%", region.getId())
                                              .replace("%x%", String.valueOf(cLoc.getBlockX()))
                                              .replace("%y%", String.valueOf(cLoc.getBlockY()))
                                              .replace("%z%", String.valueOf(cLoc.getBlockZ()))
                                              .replace("&", "§");

        String parsedStartChat = startChat.replace("%region%", region.getId())
                                          .replace("%x%", String.valueOf(cLoc.getBlockX()))
                                          .replace("%y%", String.valueOf(cLoc.getBlockY()))
                                          .replace("%z%", String.valueOf(cLoc.getBlockZ()))
                                          .replace("&", "§");

        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean inRegion = isInRegion(p.getLocation(), region);
            boolean inRadius = isInRadius(p.getLocation(), cLoc, radius);

            if (inRadius && !parsedStartChat.isEmpty()) {
                p.sendMessage(parsedStartChat);
            }

            if (inRegion) {
                p.sendMessage(startMsg.replace("&", "§"));
                sendActionBar(p, actionMsg.replace("%player%", player.getName()).replace("&", "§"));
            } else if (inRadius) {
                sendActionBar(p, parsedStartAction);
            }
        }
    }
    
    private boolean isInRadius(Location loc1, Location loc2, int radius) {
        if (!loc1.getWorld().getName().equals(loc2.getWorld().getName())) return false;
        if (radius <= 0) return true; // 0 or negative = infinite
        return loc1.distanceSquared(loc2) <= (radius * radius);
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
        Location spawn = region.getExtractionSpawnLocation();
        
        if (spawn == null) {
            String w = plugin.getConfig().getString("extraction.spawn.world");
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
        }
        
        if (spawn == null) {
            spawn = activator.getWorld().getSpawnLocation(); // Fallback
        }

        // Spawn Fireworks
        spawnFireworks(region.getConduitLocation());

        int extractedCount = 0;
        String successMsg = plugin.getConfig().getString("extraction.messages.success", "&aExtraction successful!");
        // Teleport everyone inside the extraction zone
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isInRegion(p.getLocation(), region)) {
                p.teleport(spawn);
                p.sendMessage(successMsg.replace("&", "§"));
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                extractedCount++;
                
                // Clear their session if they had one just in case
                sessions.remove(p.getUniqueId());
            }
        }

        applyCapacityAndCooldown(region, false);
    }

    private void applyCapacityAndCooldown(SavedRegion region, boolean isMimic) {
        // Capacity logic
        int currentCap = region.getCurrentCapacity();
        currentCap--;
        
        // Cooldown triggers immediately if capacity is going to be 0 or if mimic disables the point
        int nextCooldown = region.getCooldownSequence().get(region.getCooldownIndex());
        
        if (currentCap <= 0 || isMimic) {
            nextCooldown = region.getAndCycleNextCooldownMinutes();
            region.setCooldownEndTime(System.currentTimeMillis() + ((long) nextCooldown * 60 * 1000));
            region.setCurrentCapacity(-1); // Resets capacity roll
            plugin.getRegionManager().saveRegion(region); // persist
            
            if (!isMimic) {
                String departedMsg = plugin.getConfig().getString("extraction.messages.departed", "&8[&c!&8] &cExtraction point &b%region% &chas departed!");
                Bukkit.broadcastMessage(departedMsg.replace("%region%", region.getId()).replace("&", "§"));
            }
        } else {
            region.setCurrentCapacity(currentCap);
        }
        
        if (!isMimic) {
            // Announce extraction success to radius
            int radius = region.getAnnouncementRadius();
            String endChat = plugin.getConfig().getString("extraction.announcement.end_chat", "&eExtraction &b%region% &efinished! &7(Cap: &b%capacity%&7, Cooldown: &b%cooldown%m&7)");
            String parsedEndChat = endChat.replace("%region%", region.getId())
                                          .replace("%capacity%", String.valueOf(currentCap <= 0 ? 0 : currentCap))
                                          .replace("%cooldown%", String.valueOf(nextCooldown))
                                          .replace("&", "§");

            Location cLoc = region.getConduitLocation();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (isInRadius(p.getLocation(), cLoc, radius) && !isInRegion(p.getLocation(), region)) {
                    p.sendMessage(parsedEndChat);
                }
            }
        }
    }

    private void triggerMimicTrap(SavedRegion region) {
        if (plugin.getConfig().getBoolean("extraction.mimic.explode", true)) {
            float power = (float) plugin.getConfig().getDouble("extraction.mimic.explosion_power", 4.0);
            region.getConduitLocation().getWorld().createExplosion(region.getConduitLocation(), power, false, false);
        }
        
        String mobTypeStr = plugin.getConfig().getString("extraction.mimic.mob_type", "ZOMBIE");
        int spawnCount = plugin.getConfig().getInt("extraction.mimic.spawn_count", 2);
        String customName = plugin.getConfig().getString("extraction.mimic.custom_name", "&cMimic Guard");
        
        EntityType type;
        try {
            type = EntityType.valueOf(mobTypeStr.toUpperCase());
        } catch (Exception e) {
            type = EntityType.ZOMBIE;
        }
        
        for (int i = 0; i < spawnCount; i++) {
            org.bukkit.entity.Entity entity = region.getConduitLocation().getWorld().spawnEntity(region.getConduitLocation().add(0, 1.5, 0), type);
            entity.setCustomName(customName.replace("&", "§"));
            entity.setCustomNameVisible(true);
            
            if (entity instanceof org.bukkit.entity.LivingEntity) {
                org.bukkit.entity.LivingEntity living = (org.bukkit.entity.LivingEntity) entity;
                org.bukkit.inventory.EntityEquipment equip = living.getEquipment();
                if (equip != null) {
                    equip.setHelmet(getItem(plugin.getConfig().getString("extraction.mimic.equipment.helmet")));
                    equip.setChestplate(getItem(plugin.getConfig().getString("extraction.mimic.equipment.chestplate")));
                    equip.setLeggings(getItem(plugin.getConfig().getString("extraction.mimic.equipment.leggings")));
                    equip.setBoots(getItem(plugin.getConfig().getString("extraction.mimic.equipment.boots")));
                    equip.setItemInMainHand(getItem(plugin.getConfig().getString("extraction.mimic.equipment.main_hand")));
                    equip.setItemInOffHand(getItem(plugin.getConfig().getString("extraction.mimic.equipment.off_hand")));
                }
            }
        }
        
        String trapMsg = plugin.getConfig().getString("extraction.messages.mimic_trap", "&8[&c!&8] &cThe extraction point at &b%region% &cwas a Mimic Trap!");
        Bukkit.broadcastMessage(trapMsg.replace("%region%", region.getId()).replace("&", "§"));
        
        applyCapacityAndCooldown(region, true);
    }
    
    private org.bukkit.inventory.ItemStack getItem(String materialStr) {
        if (materialStr == null || materialStr.isEmpty() || materialStr.equalsIgnoreCase("NONE")) {
            return null;
        }
        try {
            return new org.bukkit.inventory.ItemStack(org.bukkit.Material.valueOf(materialStr.toUpperCase()));
        } catch (Exception e) {
            return null;
        }
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    public void cancelExtractionByDamage(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            ExtractionSession session = sessions.remove(player.getUniqueId());
            String combatMsg = plugin.getConfig().getString("extraction.messages.cancelled_combat", "&cExtraction cancelled by combat!");
            sendActionBar(player, combatMsg.replace("&", "§"));
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f);
        }
    }
    
    private void spawnFireworks(Location loc) {
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc.add(0, 1, 0), EntityType.FIREWORK_ROCKET);
        FireworkMeta fwm = fw.getFireworkMeta();
        
        java.util.List<String> hexColors = plugin.getConfig().getStringList("extraction.fireworks.colors");
        String fadeHex = plugin.getConfig().getString("extraction.fireworks.fade", "#FFFFFF");
        
        java.util.List<Color> colors = new java.util.ArrayList<>();
        if (hexColors != null && !hexColors.isEmpty()) {
            for (String hex : hexColors) {
                try {
                    if (hex.startsWith("#")) hex = hex.substring(1);
                    colors.add(Color.fromRGB(Integer.parseInt(hex, 16)));
                } catch (Exception ignored) { }
            }
        }
        if (colors.isEmpty()) {
            colors.add(Color.YELLOW);
            colors.add(Color.ORANGE);
            colors.add(Color.RED);
        }
        
        Color fadeColor = Color.WHITE;
        try {
            if (fadeHex != null && fadeHex.startsWith("#")) fadeHex = fadeHex.substring(1);
            if (fadeHex != null) fadeColor = Color.fromRGB(Integer.parseInt(fadeHex, 16));
        } catch (Exception ignored) {}
        
        // Setup firework colors
        fwm.addEffect(FireworkEffect.builder()
                .withColor(colors)
                .withFade(fadeColor)
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

    public static class ExtractionSession {
        private final long startTime;
        private final Location startLoc;
        private final SavedRegion region;
        private final int targetDurationSeconds;
        private int lastAlarmSecond = -1;

        public ExtractionSession(long startTime, Location startLoc, SavedRegion region) {
            this.startTime = startTime;
            this.startLoc = startLoc;
            this.region = region;
            this.targetDurationSeconds = region.getRandomDurationSeconds();
        }

        public long getStartTime() { return startTime; }
        public Location getStartLoc() { return startLoc; }
        public SavedRegion getRegion() { return region; }
        public int getTargetDurationSeconds() { return targetDurationSeconds; }
        public int getLastAlarmSecond() { return lastAlarmSecond; }
        public void setLastAlarmSecond(int second) { this.lastAlarmSecond = second; }
    }
}
