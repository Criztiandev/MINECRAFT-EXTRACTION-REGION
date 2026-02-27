package com.criztiandev.extractionregion.listeners;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.gui.RegionActionGUI;
import com.criztiandev.extractionregion.gui.RegionListGUI;
import com.criztiandev.extractionregion.gui.RegionMainGUI;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class RegionInventoryListener implements Listener {

    private final ExtractionRegionPlugin plugin;

    public RegionInventoryListener(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        boolean isRegionGUI = title.equals(RegionMainGUI.TITLE) || 
                              title.equals(RegionListGUI.TITLE) || 
                              title.startsWith(RegionActionGUI.TITLE) ||
                              title.equals(plugin.getConfig().getString("region-gui.title", "§8▶ §dConfigure Auto Spawns").replace("&", "§"));

        if (!isRegionGUI) return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();

        // Handle Main Menu
        if (title.equals(RegionMainGUI.TITLE)) {
            if (data.has(new NamespacedKey(plugin, "region-main"), PersistentDataType.STRING)) {
                String action = data.get(new NamespacedKey(plugin, "region-main"), PersistentDataType.STRING);
                if ("wand".equals(action)) {
                    player.chat("/lr wand");
                    player.closeInventory();
                } else if ("create".equals(action)) {
                    player.closeInventory();
                    plugin.getRegionManager().addCreatingPlayer(player.getUniqueId());
                    player.sendMessage("§aPlease type the name of your new region in chat (or type 'cancel' to abort).");
                } else if ("manage".equals(action)) {
                    new RegionListGUI(plugin).openMenu(player, 0);
                }
            }
            return;
        }

        // Handle Region List
        if (title.equals(RegionListGUI.TITLE)) {
            if (data.has(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING)) {
                String regionId = data.get(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING);
                SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                if (region != null) {
                    new RegionActionGUI(plugin).openMenu(player, region);
                }
            } else if (data.has(new NamespacedKey(plugin, "pagination-page"), PersistentDataType.INTEGER)) {
                int page = data.get(new NamespacedKey(plugin, "pagination-page"), PersistentDataType.INTEGER);
                new RegionListGUI(plugin).openMenu(player, page);
            }
            return;
        }

        // Handle Region Action
        if (title.startsWith(RegionActionGUI.TITLE)) {
            if (data.has(new NamespacedKey(plugin, "region-action"), PersistentDataType.STRING)) {
                String action = data.get(new NamespacedKey(plugin, "region-action"), PersistentDataType.STRING);
                
                if ("back".equals(action)) {
                    new RegionListGUI(plugin).openMenu(player, 0);
                    return;
                }

                String regionId = data.get(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING);
                if (regionId == null) return;

                SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                if (region == null) return;

                if ("force".equals(action)) {
                    // Trigger force Replenish
                    player.closeInventory();
                    int count = plugin.getRegionManager().forceReplenish(region);
                    player.sendMessage("§aForce replenished " + count + " chests in region §e" + regionId + "§a.");
                } else if ("timer".equals(action)) {
                    // Cycle: 60 (1h) -> 120 (2h) -> 360 (6h) -> 720 (12h) -> 1440 (24h)
                    int current = region.getResetIntervalMinutes();
                    int next = 60; // default start
                    
                    if (current == 60) next = 120;
                    else if (current == 120) next = 360; // 6 hours
                    else if (current == 360) next = 720; // 12 hours
                    else if (current == 720) next = 1440; // 24 hours
                    else if (current == 1440) next = 60; // loop back to 1 hour
                    else next = 360; // if it was a weird custom number, snap to 6 hours
                    
                    region.setResetIntervalMinutes(next);
                    plugin.getRegionManager().saveRegion(region);
                    new RegionActionGUI(plugin).openMenu(player, region); // Refresh
                } else if ("delete".equals(action)) {
                    player.chat("/lr delete " + regionId);
                    player.closeInventory();
                } else if ("getwand".equals(action)) {
                    ItemStack wand = new ItemStack(Material.STICK);
                    ItemMeta wandMeta = wand.getItemMeta();
                    if (wandMeta != null) {
                        wandMeta.setDisplayName("§dRegion Wand: " + regionId);
                        wandMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-wand"), PersistentDataType.BYTE, (byte) 1);
                        wandMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, regionId);
                        wand.setItemMeta(wandMeta);
                    }
                    player.getInventory().addItem(wand);
                    player.sendMessage("§aYou have received the Region Wand for §e" + regionId + "§a.");
                    player.closeInventory();
                }
            }
            return;
        }

        // Note: The Region Auto Spawn click-GUI was removed/replaced by Chest Drop GUI
    }

    // Region Chest Drop GUI logic removed as it's no longer used
}
