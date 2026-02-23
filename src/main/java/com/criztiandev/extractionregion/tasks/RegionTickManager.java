package com.criztiandev.extractionregion.tasks;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.scheduler.BukkitRunnable;

public class RegionTickManager extends BukkitRunnable {

    private final ExtractionRegionPlugin plugin;

    public RegionTickManager(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        for (SavedRegion region : plugin.getRegionManager().getRegions()) {
            if (region.getNextResetTime() <= now) {
                int expectedSpawns = 0;
                for (int amount : region.getAutoSpawns().values()) {
                    expectedSpawns += amount;
                }

                if (expectedSpawns > 0) {
                    int spawned = plugin.getRegionManager().runAutoSpawns(region);
                    plugin.getLogger().info("Region " + region.getId() + " reset! Scattered " + spawned + " chests.");
                }

                // Calculate next time
                long nextTime = now + (region.getResetIntervalMinutes() * 60L * 1000L);
                region.setNextResetTime(nextTime);
                plugin.getRegionManager().saveRegion(region);
            }
        }
    }
}
