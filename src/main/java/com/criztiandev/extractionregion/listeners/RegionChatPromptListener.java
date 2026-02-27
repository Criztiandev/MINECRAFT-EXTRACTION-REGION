package com.criztiandev.extractionregion.listeners;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class RegionChatPromptListener implements Listener {

    private final ExtractionRegionPlugin plugin;

    public RegionChatPromptListener(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (plugin.getRegionManager().isCreatingPlayer(player.getUniqueId())) {
            event.setCancelled(true);
            String input = event.getMessage().trim();

            if (input.equalsIgnoreCase("cancel")) {
                plugin.getRegionManager().removeCreatingPlayer(player.getUniqueId());
                player.sendMessage("§cRegion creation cancelled.");
                return;
            }

            if (input.contains(" ")) {
                player.sendMessage("§cRegion IDs cannot contain spaces. Try again, or type 'cancel'.");
                return;
            }

            // Retrieve intended type
            com.criztiandev.extractionregion.models.RegionType type = plugin.getRegionManager().getCreatingPlayerType(player.getUniqueId());
            String typeStr = "chest";
            if (type == com.criztiandev.extractionregion.models.RegionType.EXTRACTION) typeStr = "extraction";
            else if (type == com.criztiandev.extractionregion.models.RegionType.ENTRY_REGION) typeStr = "entry";

            // Execute creation command sync
            String finalTypeStr = typeStr;
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getRegionManager().removeCreatingPlayer(player.getUniqueId());
                player.chat("/lr create " + input + " " + finalTypeStr);
            });
        }
    }
}
