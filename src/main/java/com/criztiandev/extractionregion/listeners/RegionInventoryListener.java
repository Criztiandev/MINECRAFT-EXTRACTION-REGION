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
                    player.closeInventory();
                    plugin.getRegionManager().addConduitSelectingPlayer(player.getUniqueId(), regionId);
                    player.sendMessage("§a[ExtractionRegion] §ePlease punch or click the block you want to be the conduit for §b" + regionId + "§e.");
                    player.sendMessage("§7(Type 'cancel' in chat or switch items to abort)");
                }
            }
            return;
        }

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
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "extrc_cooldown_" + regionId);
                        player.sendMessage("§aPlease type the exact cooldown sequence (e.g. 15,25,30) for §e" + regionId + "§a:");
                        return;
                    }
                    
                    java.util.List<java.util.List<Integer>> presets = java.util.Arrays.asList(
                        java.util.Arrays.asList(10),
                        java.util.Arrays.asList(5, 10, 15),
                        java.util.Arrays.asList(15, 25, 30),
                        java.util.Arrays.asList(30, 45, 60)
                    );
                    
                    java.util.List<Integer> current = region.getCooldownSequence();
                    int currentIndex = -1;
                    for (int i = 0; i < presets.size(); i++) {
                        if (presets.get(i).equals(current)) {
                            currentIndex = i;
                            break;
                        }
                    }
                    
                    if (event.getClick().isLeftClick()) {
                        currentIndex = (currentIndex + 1) % presets.size();
                    } else if (event.getClick().isRightClick()) {
                        currentIndex = currentIndex - 1;
                        if (currentIndex < 0) currentIndex = presets.size() - 1;
                    }
                    
                    region.setCooldownSequence(presets.get(currentIndex));
                    region.setCooldownIndex(0);
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                } else if ("min_cap".equals(action)) {
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "extrc_min_cap_" + regionId);
                        player.sendMessage("§aPlease type the exact MINIMUM capacity for §e" + regionId + "§a:");
                        return;
                    }
                    int c = region.getMinCapacity();
                    if (event.getClick().isLeftClick()) c++;
                    else if (event.getClick().isRightClick()) c = Math.max(1, c - 1);
                    if (c > region.getMaxCapacity()) region.setMaxCapacity(c);
                    region.setMinCapacity(c);
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                } else if ("max_cap".equals(action)) {
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "extrc_max_cap_" + regionId);
                        player.sendMessage("§aPlease type the exact MAXIMUM capacity for §e" + regionId + "§a:");
                        return;
                    }
                    int c = region.getMaxCapacity();
                    if (event.getClick().isLeftClick()) c++;
                    else if (event.getClick().isRightClick()) c = Math.max(1, c - 1);
                    if (c < region.getMinCapacity()) region.setMinCapacity(c);
                    region.setMaxCapacity(c);
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                } else if ("duration".equals(action)) {
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "extrc_duration_" + regionId);
                        player.sendMessage("§aPlease type the exact duration sequence (e.g. 3,5,10) for §e" + regionId + "§a:");
                        return;
                    }
                    
                    java.util.List<java.util.List<Integer>> presets = java.util.Arrays.asList(
                        java.util.Arrays.asList(5),
                        java.util.Arrays.asList(3, 5, 1, 10),
                        java.util.Arrays.asList(10, 15),
                        java.util.Arrays.asList(30)
                    );
                    
                    java.util.List<Integer> current = region.getPossibleDurations();
                    int currentIndex = -1;
                    for (int i = 0; i < presets.size(); i++) {
                        if (presets.get(i).equals(current)) {
                            currentIndex = i;
                            break;
                        }
                    }
                    
                    if (event.getClick().isLeftClick()) {
                        currentIndex = (currentIndex + 1) % presets.size();
                    } else if (event.getClick().isRightClick()) {
                        currentIndex = currentIndex - 1;
                        if (currentIndex < 0) currentIndex = presets.size() - 1;
                    }
                    
                    region.setPossibleDurations(presets.get(currentIndex));
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                } else if ("mimic".equals(action)) {
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "extrc_mimic_" + regionId);
                        player.sendMessage("§aPlease type the exact mimic chance percentage (0-100) for §e" + regionId + "§a:");
                        return;
                    }

                    if (event.getClick().isLeftClick()) {
                        region.setMimicEnabled(!region.isMimicEnabled());
                    } else if (event.getClick().isRightClick()) {
                        int c = region.getMimicChance();
                        c += 5;
                        if (c > 100) c = 0;
                        region.setMimicChance(c);
                    }
                    
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                } else if ("radius".equals(action)) {
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "extrc_radius_" + regionId);
                        player.sendMessage("§aPlease type the exact announcement radius (0 for global) for §e" + regionId + "§a:");
                        return;
                    }
                    
                    int r = region.getAnnouncementRadius();
                    if (event.getClick().isLeftClick()) {
                        r += 10;
                    } else if (event.getClick().isRightClick()) {
                        r = Math.max(0, r - 10);
                    }
                    
                    region.setAnnouncementRadius(r);
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                } else if ("beam".equals(action)) {
                    player.closeInventory();
                    plugin.getRegionManager().addPromptState(player.getUniqueId(), "extrc_beam_" + regionId);
                    player.sendMessage("§aPlease type the hex color (e.g., #FF0000) for the beam of §e" + regionId + "§a:");
                } else if ("alarm".equals(action)) {
                    player.closeInventory();
                    plugin.getRegionManager().addPromptState(player.getUniqueId(), "extrc_alarm_" + regionId);
                    player.sendMessage("§aPlease type the Sound enum (e.g., ENTITY_ENDER_DRAGON_GROWL) for the alarm of §e" + regionId + "§a:");
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
