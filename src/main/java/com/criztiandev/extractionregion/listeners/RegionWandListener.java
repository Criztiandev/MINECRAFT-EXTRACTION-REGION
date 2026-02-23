package com.criztiandev.extractionregion.listeners;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionSelection;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class RegionWandListener implements Listener {

    private final ExtractionRegionPlugin plugin;

    public RegionWandListener(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "region-wand"), PersistentDataType.BYTE)) {
            return;
        }

        if (!player.hasPermission("extractionchest.admin")) {
            return;
        }

        Action action = event.getAction();
        Block block = event.getClickedBlock();

        if (block == null) return;

        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            RegionSelection sel = plugin.getRegionManager().getOrCreateSelection(player.getUniqueId());
            sel.setPos1(block.getLocation());
            player.sendMessage("§dPos 1 set to (" + block.getX() + ", " + block.getY() + ", " + block.getZ() + ")");
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            RegionSelection sel = plugin.getRegionManager().getOrCreateSelection(player.getUniqueId());
            sel.setPos2(block.getLocation());
            player.sendMessage("§dPos 2 set to (" + block.getX() + ", " + block.getY() + ", " + block.getZ() + ")");
        }
    }
}

