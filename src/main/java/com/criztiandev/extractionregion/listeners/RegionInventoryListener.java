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
                              title.startsWith(com.criztiandev.extractionregion.gui.ExtractionSettingsGUI.TITLE) ||
                              title.startsWith(com.criztiandev.extractionregion.gui.EntrySettingsGUI.TITLE) ||
                              title.startsWith(com.criztiandev.extractionregion.gui.RegionSubMenuGUI.TITLE_PREFIX) ||
                              title.startsWith(com.criztiandev.extractionregion.gui.RegionChestsGUI.TITLE) ||
                              title.equals(com.criztiandev.extractionregion.gui.WandMenuGUI.TITLE) ||
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
                if ("wands".equals(action)) {
                    new com.criztiandev.extractionregion.gui.WandMenuGUI(plugin).openMenu(player);
                } else if ("toggle_visibility".equals(action)) {
                    boolean current = plugin.getConfig().getBoolean("region.always-show-regions", false);
                    plugin.getConfig().set("region.always-show-regions", !current);
                    plugin.saveConfig();
                    new RegionMainGUI(plugin).openMenu(player);
                } else if ("cat_chest".equals(action)) {
                    new com.criztiandev.extractionregion.gui.RegionSubMenuGUI(plugin).openMenu(player, com.criztiandev.extractionregion.models.RegionType.CHEST_REPLENISH);
                } else if ("cat_extraction".equals(action)) {
                    new com.criztiandev.extractionregion.gui.RegionSubMenuGUI(plugin).openMenu(player, com.criztiandev.extractionregion.models.RegionType.EXTRACTION);
                } else if ("cat_entry".equals(action)) {
                    new com.criztiandev.extractionregion.gui.RegionSubMenuGUI(plugin).openMenu(player, com.criztiandev.extractionregion.models.RegionType.ENTRY_REGION);
                }
            }
            return;
        }

        // Handle SubMenu GUI
        if (title.startsWith(com.criztiandev.extractionregion.gui.RegionSubMenuGUI.TITLE_PREFIX)) {
            if (data.has(new NamespacedKey(plugin, "region-submenu"), PersistentDataType.STRING)) {
                String action = data.get(new NamespacedKey(plugin, "region-submenu"), PersistentDataType.STRING);
                if ("back".equals(action)) {
                    new RegionMainGUI(plugin).openMenu(player);
                    return;
                }
                
                String typeStr = data.get(new NamespacedKey(plugin, "region-type"), PersistentDataType.STRING);
                com.criztiandev.extractionregion.models.RegionType type = typeStr != null ? 
                    com.criztiandev.extractionregion.models.RegionType.valueOf(typeStr) : null;
                
                if ("wand".equals(action)) {
                    player.closeInventory();
                    if (type != null) {
                        player.getInventory().addItem(com.criztiandev.extractionregion.utils.WandUtil.getWand(plugin, type));
                        player.sendMessage("§aYou have received the " + type.name().replace("_", " ") + " Wand.");
                    } else {
                        new com.criztiandev.extractionregion.gui.WandMenuGUI(plugin).openMenu(player);
                    }
                } else if ("create".equals(action) && type != null) {
                    player.closeInventory();
                    plugin.getRegionManager().addCreatingPlayer(player.getUniqueId(), type);
                    player.sendMessage("§aPlease type the name of your new region in chat (or type 'cancel' to abort).");
                } else if ("manage".equals(action) && type != null) {
                    new RegionListGUI(plugin).openMenu(player, 0, type);
                }
            }
            return;
        }

        // Handle Wand Menu
        if (title.equals(com.criztiandev.extractionregion.gui.WandMenuGUI.TITLE)) {
            if (data.has(new NamespacedKey(plugin, "wand-menu-action"), PersistentDataType.STRING)) {
                String action = data.get(new NamespacedKey(plugin, "wand-menu-action"), PersistentDataType.STRING);
                if ("back".equals(action)) {
                    new RegionMainGUI(plugin).openMenu(player);
                    return;
                }
                
                try {
                    com.criztiandev.extractionregion.models.RegionType wandType = com.criztiandev.extractionregion.models.RegionType.valueOf(action);
                    player.getInventory().addItem(com.criztiandev.extractionregion.utils.WandUtil.getWand(plugin, wandType));
                    player.sendMessage("§aYou have received the " + wandType.name().replace("_", " ") + " Wand.");
                    player.closeInventory();
                } catch (IllegalArgumentException e) {
                    // Not a valid RegionType enum name, skip
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
                String typeStr = data.get(new NamespacedKey(plugin, "pagination-filter"), PersistentDataType.STRING);
                com.criztiandev.extractionregion.models.RegionType type = typeStr != null ? 
                    com.criztiandev.extractionregion.models.RegionType.valueOf(typeStr) : null;
                new RegionListGUI(plugin).openMenu(player, page, type);
            } else if (data.has(new NamespacedKey(plugin, "list-back"), PersistentDataType.STRING)) {
                String typeStr = data.get(new NamespacedKey(plugin, "pagination-filter"), PersistentDataType.STRING);
                if (typeStr != null) {
                    com.criztiandev.extractionregion.models.RegionType type = com.criztiandev.extractionregion.models.RegionType.valueOf(typeStr);
                    new com.criztiandev.extractionregion.gui.RegionSubMenuGUI(plugin).openMenu(player, type);
                } else {
                    new RegionMainGUI(plugin).openMenu(player);
                }
            }
            return;
        }

        // Handle Region Action
        if (title.startsWith(RegionActionGUI.TITLE)) {
            if (data.has(new NamespacedKey(plugin, "region-action"), PersistentDataType.STRING)) {
                String action = data.get(new NamespacedKey(plugin, "region-action"), PersistentDataType.STRING);
                
                if ("back".equals(action)) {
                    new RegionListGUI(plugin).openMenu(player, 0, plugin.getRegionManager().getRegion(data.get(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING)).getType());
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
                    if (event.isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addTimerConfiguringPlayer(player.getUniqueId(), regionId);
                        player.sendMessage("§ePlease type the custom timer interval in minutes in chat (or type 'cancel' to abort).");
                        return;
                    }
                    
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
                } else if ("manage_chests".equals(action)) {
                    new com.criztiandev.extractionregion.gui.RegionChestsGUI(plugin).openMenu(player, region, 0);
                } else if ("edit_extraction".equals(action)) {
                    new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                } else if ("edit_entry".equals(action)) {
                    new com.criztiandev.extractionregion.gui.EntrySettingsGUI(plugin).openMenu(player, region);
                } else if ("set_dropzone".equals(action)) {
                    com.criztiandev.extractionregion.models.RegionSelection selection = plugin.getRegionManager().getSelection(player.getUniqueId());
                    if (selection == null || selection.getPos1() == null || selection.getPos2() == null) {
                        player.sendMessage("§cYou must first select an area using the Region Wand!");
                        player.closeInventory();
                        return;
                    }
                    if (selection.getPos1().getWorld() == null || !selection.getPos1().getWorld().equals(selection.getPos2().getWorld())) {
                        player.sendMessage("§cPositions must be in the same world!");
                        return;
                    }
                    region.setDropWorld(selection.getPos1().getWorld().getName());
                    region.setDropMinX(Math.min(selection.getPos1().getBlockX(), selection.getPos2().getBlockX()));
                    region.setDropMaxX(Math.max(selection.getPos1().getBlockX(), selection.getPos2().getBlockX()));
                    region.setDropMinY(Math.min(selection.getPos1().getBlockY(), selection.getPos2().getBlockY()));
                    region.setDropMaxY(Math.max(selection.getPos1().getBlockY(), selection.getPos2().getBlockY()));
                    region.setDropMinZ(Math.min(selection.getPos1().getBlockZ(), selection.getPos2().getBlockZ()));
                    region.setDropMaxZ(Math.max(selection.getPos1().getBlockZ(), selection.getPos2().getBlockZ()));
                    
                    plugin.getRegionManager().saveRegion(region);
                    player.sendMessage("§aDrop Zone successfully configured for Entry Region " + regionId + "!");
                    new RegionActionGUI(plugin).openMenu(player, region);
                } else if ("set_conduit".equals(action)) {
                    ItemStack wand = new ItemStack(Material.END_ROD);
                    ItemMeta wandMeta = wand.getItemMeta();
                    if (wandMeta != null) {
                        wandMeta.setDisplayName("§bConduit Selector Tool: " + regionId);
                        wandMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "conduit-wand"), PersistentDataType.BYTE, (byte) 1);
                        wandMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, regionId);
                        wand.setItemMeta(wandMeta);
                    }
                    player.getInventory().addItem(wand);
                    player.sendMessage("§aYou have received the Conduit Selector for §e" + regionId + "§a.");
                    player.sendMessage("§7Right-click a block inside the region to set it as the extraction point.");
                    player.closeInventory();
                }
            }
            return;
        }

        // Handle Extraction Settings
        if (title.startsWith(com.criztiandev.extractionregion.gui.ExtractionSettingsGUI.TITLE)) {
            if (data.has(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING)) {
                String action = data.get(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING);
                String regionId = data.get(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING);
                if (regionId == null) return;

                SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                if (region == null) return;

                if ("back".equals(action)) {
                    new RegionActionGUI(plugin).openMenu(player, region);
                } else if ("cooldown".equals(action)) {
                    int c = region.getCooldownMinutes();
                    if (c == 5) region.setCooldownMinutes(10);
                    else if (c == 10) region.setCooldownMinutes(15);
                    else if (c == 15) region.setCooldownMinutes(30);
                    else if (c == 30) region.setCooldownMinutes(60);
                    else region.setCooldownMinutes(5);
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                } else if ("capacity".equals(action)) {
                    int min = region.getMinCapacity();
                    int max = region.getMaxCapacity();
                    // Sequences: 1-1 -> 1-3 -> 3-5 -> 5-5
                    if (min == 1 && max == 1) { region.setMinCapacity(1); region.setMaxCapacity(3); }
                    else if (min == 1 && max == 3) { region.setMinCapacity(3); region.setMaxCapacity(5); }
                    else if (min == 3 && max == 5) { region.setMinCapacity(5); region.setMaxCapacity(5); }
                    else { region.setMinCapacity(1); region.setMaxCapacity(1); }
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                } else if ("mimic".equals(action)) {
                    region.setMimicEnabled(!region.isMimicEnabled());
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                }
            }
            return;
        }

        // Handle Entry Settings
        if (title.startsWith(com.criztiandev.extractionregion.gui.EntrySettingsGUI.TITLE)) {
            if (data.has(new NamespacedKey(plugin, "entry-action"), PersistentDataType.STRING)) {
                String action = data.get(new NamespacedKey(plugin, "entry-action"), PersistentDataType.STRING);
                String regionId = data.get(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING);
                if (regionId == null) return;

                SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                if (region == null) return;

                if ("back".equals(action)) {
                    new RegionActionGUI(plugin).openMenu(player, region);
                } else if ("slowfall".equals(action)) {
                    int sf = region.getSlowFallingSeconds();
                    if (sf == 0) region.setSlowFallingSeconds(5);
                    else if (sf == 5) region.setSlowFallingSeconds(10);
                    else if (sf == 10) region.setSlowFallingSeconds(15);
                    else region.setSlowFallingSeconds(0);
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.EntrySettingsGUI(plugin).openMenu(player, region);
                } else if ("blindness".equals(action)) {
                    int b = region.getBlindnessSeconds();
                    if (b == 0) region.setBlindnessSeconds(1);
                    else if (b == 1) region.setBlindnessSeconds(3);
                    else if (b == 3) region.setBlindnessSeconds(5);
                    else region.setBlindnessSeconds(0);
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.EntrySettingsGUI(plugin).openMenu(player, region);
                }
            }
            return;
        }

        // Handle Region Chests
        if (title.startsWith(com.criztiandev.extractionregion.gui.RegionChestsGUI.TITLE)) {
            if (data.has(new NamespacedKey(plugin, "region-chests-action"), PersistentDataType.STRING)) {
                String action = data.get(new NamespacedKey(plugin, "region-chests-action"), PersistentDataType.STRING);
                String regionId = data.get(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING);
                if (regionId == null) return;
                SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                if (region == null) return;

                if ("back".equals(action)) {
                    new RegionActionGUI(plugin).openMenu(player, region);
                } else if ("prev_page".equals(action) || "next_page".equals(action)) {
                    int page = data.get(new NamespacedKey(plugin, "pagination-page"), PersistentDataType.INTEGER);
                    new com.criztiandev.extractionregion.gui.RegionChestsGUI(plugin).openMenu(player, region, page);
                } else if ("edit_chest".equals(action)) {
                    String parentName = data.get(new NamespacedKey(plugin, "chest-parent"), PersistentDataType.STRING);
                    if (parentName != null) {
                        com.criztiandev.extractionchest.models.ParentChestDefinition def = plugin.getExtractionChestApi().getLootTableManager().getDefinition(parentName);
                        if (def != null) {
                            plugin.getExtractionChestApi().getChestSettingsGUI().openMenu(player, def, "chest-settings-main");
                        } else {
                            player.sendMessage("§cCould not find parent definition '" + parentName + "'.");
                        }
                    }
                }
            }
            return;
        }

        // Note: The Region Auto Spawn click-GUI was removed/replaced by Chest Drop GUI
    }

    // Region Chest Drop GUI logic removed as it's no longer used
}
