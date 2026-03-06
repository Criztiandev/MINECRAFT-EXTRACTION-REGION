package com.criztiandev.extractionregion.papi;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.SavedRegion;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class ExtractionRegionPlaceholderExpansion extends PlaceholderExpansion {

    private final ExtractionRegionPlugin plugin;

    public ExtractionRegionPlaceholderExpansion(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true; 
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "extractionregion";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Criztiandev";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // Format: %extractionregion_entry_cooldown_<regionid>%
        if (params.startsWith("entry_cooldown_")) {
            String regionId = params.substring("entry_cooldown_".length());
            SavedRegion region = plugin.getRegionManager().getRegion(regionId);
            
            if (region != null) {
                long remainingMillis = region.getRemainingEntryCooldownTime(player.getUniqueId().toString());
                if (remainingMillis > 0) {
                    long seconds = remainingMillis / 1000;
                    return seconds + "s";
                }
                return "0s";
            }
            return "0s";
        }

        return null; // Unknown placeholder
    }
}
