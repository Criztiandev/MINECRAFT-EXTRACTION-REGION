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
        int currentMinuteOfDay = manilaTime.getHour() * 60 + manilaTime.getMinute();

        for (SavedRegion region : plugin.getRegionManager().getRegions()) {
            int interval = region.getResetIntervalMinutes();
            if (interval > 0) {
                if (currentMinuteOfDay % interval == 0) {
                    if (region.getLastResetMinuteOfDay() != currentMinuteOfDay) {
                        region.setLastResetMinuteOfDay(currentMinuteOfDay);
                        int replenished = plugin.getRegionManager().forceReplenish(region);
                        if (replenished > 0) {
                            plugin.getLogger().info("Cron trigger: Region " + region.getId() + " replenishing " + replenished + " chests.");
                        }
                    }
                }
            }
        }
    }
}
