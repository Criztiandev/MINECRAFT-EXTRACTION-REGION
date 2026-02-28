package com.criztiandev.extractionregion.tasks;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class HologramTask extends BukkitRunnable {

    private final ExtractionRegionPlugin plugin;

    public HologramTask(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().updateHolograms();
        }
    }
}
