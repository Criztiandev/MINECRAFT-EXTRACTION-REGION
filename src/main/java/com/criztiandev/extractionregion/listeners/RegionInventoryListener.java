package com.criztiandev.extractionregion.listeners;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.gui.RegionActionGUI;
import com.criztiandev.extractionregion.gui.RegionAutoSpawnGUI;
import com.criztiandev.extractionregion.gui.RegionListGUI;
import com.criztiandev.extractionregion.gui.RegionMainGUI;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
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
                    player.chat("/re wand");
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

                if ("spawns".equals(action)) {
                    new RegionAutoSpawnGUI(plugin).openMenu(player, region);
                } else if ("force".equals(action)) {
                    player.chat("/re spawn " + regionId);
                    player.closeInventory();
                } else if ("delete".equals(action)) {
                    player.chat("/re delete " + regionId);
                    player.closeInventory();
                }
            }
            return;
        }

        // Handle Region Auto Spawn
        if (title.equals(plugin.getConfig().getString("region-gui.title", "§8▶ §dConfigure Auto Spawns").replace("&", "§"))) {
            if (data.has(new NamespacedKey(plugin, "region-gui-type"), PersistentDataType.STRING)) {
                String type = data.get(new NamespacedKey(plugin, "region-gui-type"), PersistentDataType.STRING);
                String regionId = data.get(new NamespacedKey(plugin, "region-gui-region"), PersistentDataType.STRING);
                
                if (regionId == null || type == null) return;
                SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                if (region == null) return;

                int amount = region.getAutoSpawnAmount(type);
                if (event.getClick() == ClickType.LEFT) {
                    amount++;
                } else if (event.getClick() == ClickType.RIGHT) {
                    amount = Math.max(0, amount - 1);
                }

                region.setAutoSpawn(type, amount);
                plugin.getRegionManager().saveRegion(region);
                
                // Refresh GUI
                new RegionAutoSpawnGUI(plugin).openMenu(player, region);
            }
        }
    }
}
