package com.criztiandev.extractionregion.tasks;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.scheduler.BukkitRunnable;

public class RegionCronTask extends BukkitRunnable {

    private final ExtractionRegionPlugin plugin;

    public RegionCronTask(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        long nextReset = plugin.getConfig().getLong("regions.next-reset-time", 0);
        long now = System.currentTimeMillis();

        if (now >= nextReset) {
            // It's time to reset all region auto-spawns
            int totalSpawns = 0;
            for (SavedRegion region : plugin.getRegionManager().getRegions()) {
                totalSpawns += plugin.getRegionManager().runAutoSpawns(region);
            }

            if (totalSpawns > 0) {
                plugin.getLogger().info("Region Cron Job executed! Scattered " + totalSpawns + " chests across all regions.");
            }

            // Calculate next time
            long intervalHours = plugin.getConfig().getLong("regions.auto-reset-hours", 8);
            long newTime = now + (intervalHours * 3600L * 1000L);
            
            plugin.getConfig().set("regions.next-reset-time", newTime);
            plugin.saveConfig();
        }
    }
}

