package com.criztiandev.extractionregion.tasks;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.scheduler.BukkitRunnable;
import java.time.ZoneId;

public class RegionTickManager extends BukkitRunnable {

    private final ExtractionRegionPlugin plugin;

    private final ZoneId phZoneId = ZoneId.of("Asia/Manila");

    public RegionTickManager(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        java.time.ZonedDateTime manilaTime = java.time.ZonedDateTime.now(phZoneId);
        int currentHour = manilaTime.getHour();
        int currentMinute = manilaTime.getMinute();

        for (SavedRegion region : plugin.getRegionManager().getRegions()) {
            int intervalMinutes = region.getResetIntervalMinutes();
            if (intervalMinutes > 0) {
                // If the interval is configured in hours (e.g., 60, 120, 180 minutes)
                int intervalHours = intervalMinutes / 60;
                
                // Only trigger exactly at the top of the hour (minute 0)
                if (currentMinute == 0 && intervalHours > 0) {
                    // Check if current hour aligns with the interval block
                    if (currentHour % intervalHours == 0) {
                        // Prevent double-triggering within the same hour
                        if (region.getLastResetMinuteOfDay() != currentHour) {
                            region.setLastResetMinuteOfDay(currentHour);
                            int replenished = plugin.getRegionManager().forceReplenish(region);
                            if (replenished > 0) {
                                plugin.getLogger().info("Cron trigger: Region " + region.getId() + " replenishing " + replenished + " chests.");
                                
                                // Broadcast to all players globally
                                String broadcastMsg = plugin.getConfig().getString("region.messages.replenish_broadcast", "&8[&a!&8] &aThe extraction region &b%region% &ahas been replenished!");
                                org.bukkit.Bukkit.broadcastMessage(broadcastMsg.replace("%region%", region.getId()).replace("&", "§"));
                                
                                // Play sound globally
                                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
