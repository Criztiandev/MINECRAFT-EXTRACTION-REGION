package com.criztiandev.extractionregion.tasks;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class RegionTickManager extends BukkitRunnable {

    private final ExtractionRegionPlugin plugin;
    private final ZoneId configuredZone;

    public RegionTickManager(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
        String tzStr = plugin.getConfig().getString("region.timezone", "Asia/Manila");
        ZoneId zone;
        try {
            zone = ZoneId.of(tzStr);
        } catch (Exception e) {
            plugin.getLogger().warning("[RegionEditor] Invalid timezone '" + tzStr + "' — falling back to Asia/Manila.");
            zone = ZoneId.of("Asia/Manila");
        }
        this.configuredZone = zone;
    }

    @Override
    public void run() {
        ZonedDateTime now = ZonedDateTime.now(configuredZone);
        int currentHour   = now.getHour();
        int currentMinute = now.getMinute();

        boolean announceEnabled = plugin.getConfig().getBoolean("region.replenish-announcement.enabled", true);
        String replenishMsg     = plugin.getConfig().getString("region.replenish-announcement.message",
                "&8[&a!&8] &aThe extraction region &b%region% &ahas been replenished!");
        String preAnnounceMsg   = plugin.getConfig().getString("region.replenish-announcement.pre-announce-message",
                "&8[&e!&8] &eThe extraction region &b%region% &ewill replenish in &f%minutes% minute(s)&e!");
        List<?> rawOffsets      = plugin.getConfig().getList("region.replenish-announcement.pre-announce-minutes");

        // Sound settings — read once per tick, not per region
        String replenishSoundName  = plugin.getConfig().getString("region.replenish-announcement.sound", "ENTITY_ENDER_DRAGON_GROWL");
        float  replenishVol        = (float) plugin.getConfig().getDouble("region.replenish-announcement.sound-volume", 1.0);
        float  replenishPitch      = (float) plugin.getConfig().getDouble("region.replenish-announcement.sound-pitch", 1.5);
        String preAnnounceSoundName = plugin.getConfig().getString("region.replenish-announcement.pre-announce-sound", "BLOCK_NOTE_BLOCK_PLING");
        float  preAnnounceVol      = (float) plugin.getConfig().getDouble("region.replenish-announcement.pre-announce-sound-volume", 1.0);
        float  preAnnouncePitch    = (float) plugin.getConfig().getDouble("region.replenish-announcement.pre-announce-sound-pitch", 2.0);

        org.bukkit.Sound replenishSound  = parseSound(replenishSoundName);
        org.bukkit.Sound preAnnounceSound = parseSound(preAnnounceSoundName);

        for (SavedRegion region : plugin.getRegionManager().getRegions()) {
            int intervalMinutes = region.getResetIntervalMinutes();
            if (intervalMinutes <= 0) continue;

            int intervalHours = intervalMinutes / 60;
            if (intervalHours <= 0) continue;

            // --- REPLENISH ---
            // Fires exactly at minute 0 of each interval hour.
            if (currentMinute == 0 && currentHour % intervalHours == 0) {
                if (region.getLastResetMinuteOfDay() != currentHour) {
                    region.setLastResetMinuteOfDay(currentHour);
                    int replenished = plugin.getRegionManager().forceReplenish(region);
                    if (replenished > 0) {
                        plugin.getLogger().info("Cron replenish: " + region.getId() + " (" + replenished + " chests).");
                        if (announceEnabled) {
                            String msg = replenishMsg.replace("%region%", region.getId()).replace("&", "\u00a7");
                            Bukkit.broadcastMessage(msg);
                            // Play sound to every online player
                            if (replenishSound != null) {
                                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                                    p.playSound(p.getLocation(), replenishSound, replenishVol, replenishPitch);
                                }
                            }
                        }
                    }
                }
            }

            // --- PRE-ANNOUNCE WARNINGS ---
            // For each configured offset (e.g. 10, 5, 1 min before replenish),
            // calculate the wall-clock minute when that warning should fire and
            // broadcast it exactly once per offset per cycle.
            if (announceEnabled && rawOffsets != null) {
                for (Object raw : rawOffsets) {
                    if (!(raw instanceof Number)) continue;
                    int offsetMin = ((Number) raw).intValue();
                    if (offsetMin <= 0) continue;

                    // Find the next replenish hour, then subtract the offset in minutes.
                    // nextReplenishHour is the smallest future h>currentHour (or wraps) where h%intervalHours==0.
                    int nextReplenishHour = currentHour;
                    for (int h = currentHour; h <= currentHour + intervalHours; h++) {
                        if (h % intervalHours == 0 && (h > currentHour || currentMinute == 0)) {
                            nextReplenishHour = h;
                            break;
                        }
                    }
                    // Total minutes until replenish from start of current hour
                    int minutesUntil = (nextReplenishHour - currentHour) * 60 - currentMinute;
                    if (minutesUntil == offsetMin) {
                        // Encode uniquely: hour+offsetMin so each fires once per cycle
                        int preAnnounceKey = nextReplenishHour * 1000 + offsetMin;
                        if (region.getLastResetMinuteOfDay() != preAnnounceKey) {
                            // Don't overwrite the replenish key — only write the preAnnounce key
                            // when we're NOT on the replenish minute itself.
                            if (currentMinute != 0) {
                                region.setLastResetMinuteOfDay(preAnnounceKey);
                            }
                            String msg = preAnnounceMsg
                                    .replace("%region%", region.getId())
                                    .replace("%minutes%", String.valueOf(offsetMin))
                                    .replace("&", "\u00a7");
                            Bukkit.broadcastMessage(msg);
                            // Play pre-announce sound to every online player
                            if (preAnnounceSound != null) {
                                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                                    p.playSound(p.getLocation(), preAnnounceSound, preAnnounceVol, preAnnouncePitch);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Safely parses a Bukkit Sound by name. Returns null (no sound) if the name is invalid. */
    private org.bukkit.Sound parseSound(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            return org.bukkit.Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[RegionEditor] Unknown sound '" + name + "' in config.yml — no sound will play.");
            return null;
        }
    }
}
