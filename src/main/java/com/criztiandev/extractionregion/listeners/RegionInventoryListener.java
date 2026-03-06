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
                              title.startsWith(com.criztiandev.extractionregion.gui.RegionChestsBulkGUI.TITLE) ||
                              title.startsWith(com.criztiandev.extractionregion.gui.RegionChestsGUI.TITLE) ||
                              title.startsWith(com.criztiandev.extractionregion.gui.HologramSettingsGUI.TITLE) ||
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
                } else if ("emergency_fix".equals(action)) {
                    // Close UI first
                    player.closeInventory();
                    
                    int entitiesWiped = 0;
                    org.bukkit.World targetWorld = org.bukkit.Bukkit.getWorld(region.getWorld());
                    if (targetWorld != null) {
                        for (org.bukkit.entity.Entity e : targetWorld.getEntitiesByClasses(org.bukkit.entity.ArmorStand.class, org.bukkit.entity.TextDisplay.class)) {
                            org.bukkit.Location el = e.getLocation();
                            if (el.getBlockX() >= region.getMinX() && el.getBlockX() <= region.getMaxX() &&
                                el.getBlockZ() >= region.getMinZ() && el.getBlockZ() <= region.getMaxZ()) {
                                
                                // Cleanse any floating Hologram or TextDisplay inside the zone
                                e.remove();
                                entitiesWiped++;
                            }
                        }
                    }

                    if (region.getType() == com.criztiandev.extractionregion.models.RegionType.EXTRACTION) {
                        plugin.getExtractionTask().getSessions().values().removeIf(session -> session.getRegion().getId().equals(region.getId()));
                        plugin.getHologramManager().removeHologram(region.getId()); // Hologram loop will auto-revive it next tick
                        region.setCooldownEndTime(0);
                        region.setCurrentCapacity(-1);
                        plugin.getRegionManager().saveRegion(region);
                    } else if (region.getType() == com.criztiandev.extractionregion.models.RegionType.CHEST_REPLENISH) {
                        int chestsWiped = 0;
                        if (plugin.getExtractionChestApi() != null) {
                            for (com.criztiandev.extractionchest.models.ChestInstance inst : plugin.getExtractionChestApi().getChestInstanceManager().getAllInstances()) {
                                if (inst.getWorld().equals(region.getWorld())) {
                                    org.bukkit.World w = org.bukkit.Bukkit.getWorld(inst.getWorld());
                                    if (w != null) {
                                        org.bukkit.Location loc = inst.getLocation(w);
                                        if (loc != null &&
                                            loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX() &&
                                            loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ()) {
                                            
                                            // Secure wipe loop
                                            plugin.getExtractionChestApi().getChestInstanceManager().changeState(inst, com.criztiandev.extractionchest.models.ChestState.RESPAWNING);
                                            plugin.getExtractionChestApi().getHologramManager().createHologram(inst);
                                            chestsWiped++;
                                        }
                                    }
                                }
                            }
                        }
                        player.sendMessage("§a[RegionEditor] Force-Refilled §e" + chestsWiped + "§a chests within the boundary.");
                    }
                    
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 1.0f);
                    player.sendMessage("§a[RegionEditor] §e" + regionId + " §aEmergency Sweep Complete. Purged §e" + entitiesWiped + "§a stuck hologram entities and reset all caches.");
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
                } else if ("bypass".equals(action)) {
                    region.setBypassCooldown(!region.isBypassCooldown());
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.RegionActionGUI(plugin).openMenu(player, region);
                } else if ("evict_players".equals(action)) {
                    player.closeInventory();
                    int evicted = 0;
                    org.bukkit.World w = org.bukkit.Bukkit.getWorld(region.getWorld());
                    if (w != null) {
                        for (Player p : w.getPlayers()) {
                            if (p.isOp() || p.hasPermission("extractionregion.admin")) continue;
                            org.bukkit.Location loc = p.getLocation();
                            if (loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX() &&
                                loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ()) {
                                
                                // Evict them
                                org.bukkit.Location spawn = plugin.getConfig().getLocation("extraction.spawn");
                                if (spawn == null) {
                                    String worldStr = plugin.getConfig().getString("extraction.spawn.world");
                                    if (worldStr != null) {
                                        org.bukkit.World spWorld = Bukkit.getWorld(worldStr);
                                        if (spWorld != null) {
                                            spawn = new org.bukkit.Location(spWorld, 
                                                plugin.getConfig().getDouble("extraction.spawn.x"),
                                                plugin.getConfig().getDouble("extraction.spawn.y"),
                                                plugin.getConfig().getDouble("extraction.spawn.z"),
                                                (float) plugin.getConfig().getDouble("extraction.spawn.yaw"),
                                                (float) plugin.getConfig().getDouble("extraction.spawn.pitch")
                                            );
                                        }
                                    }
                                }
                                if (spawn == null) spawn = w.getSpawnLocation();
                                p.teleport(spawn);
                                p.sendMessage("§cYou have been forcefully evicted from the region because it is in lockdown/maintenance mode.");
                                evicted++;
                            }
                        }
                    }
                    player.sendMessage("§aSuccessfully evicted §e" + evicted + " §anon-OP players from §b" + region.getId());
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
                        java.util.Arrays.asList(5, 10, 15, 25),
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
                } else if ("destination".equals(action)) {
                    if (region.isExtractionUseCommand()) {
                        if (event.getClick().isLeftClick() && !event.getClick().isShiftClick()) {
                            region.setExtractionUseCommand(false);
                            plugin.getRegionManager().saveRegion(region);
                            new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                        } else if (event.getClick().isShiftClick()) {
                            player.closeInventory();
                            plugin.getRegionManager().addPromptState(player.getUniqueId(), "extrc_dest_cmd_" + regionId);
                            player.sendMessage("§a[ExtractionRegion] §ePlease type the exact command to run (e.g. 'spawn %player%' or '[CONSOLE] mvtp %player% world') for §b" + regionId + "§e:");
                        }
                    } else {
                        if (event.getClick().isLeftClick() && !event.getClick().isShiftClick()) {
                            region.setExtractionUseCommand(true);
                            plugin.getRegionManager().saveRegion(region);
                            new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                        } else if (event.getClick().isRightClick() && !event.getClick().isShiftClick()) {
                            org.bukkit.Location loc = player.getLocation();
                            region.setExtractionSpawnWorld(loc.getWorld().getName());
                            region.setExtractionSpawnX(loc.getX());
                            region.setExtractionSpawnY(loc.getY());
                            region.setExtractionSpawnZ(loc.getZ());
                            region.setExtractionSpawnYaw(loc.getYaw());
                            region.setExtractionSpawnPitch(loc.getPitch());
                            plugin.getRegionManager().saveRegion(region);
                            new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                            player.sendMessage("§aExtraction spawn successfully set to your current location!");
                        } else if (event.getClick().isShiftClick()) {
                            player.closeInventory();
                            plugin.getRegionManager().addPromptState(player.getUniqueId(), "extrc_dest_loc_" + regionId);
                            player.sendMessage("§a[ExtractionRegion] §ePlease type the exact coordinates in the format 'world,x,y,z' for §b" + regionId + "§e:");
                        }
                    }
                } else if ("beam".equals(action)) {
                    player.closeInventory();
                    plugin.getRegionManager().addPromptState(player.getUniqueId(), "extrc_beam_" + regionId);
                    player.sendMessage("§aPlease type the hex color (e.g., #FF0000) for the beam of §e" + regionId + "§a:");
                } else if ("alarm".equals(action)) {
                    player.closeInventory();
                    plugin.getRegionManager().addPromptState(player.getUniqueId(), "extrc_alarm_" + regionId);
                    player.sendMessage("§aPlease type the Sound enum (e.g., ENTITY_ENDER_DRAGON_GROWL) for the alarm of §e" + regionId + "§a:");
                } else if ("cooldown_cmd".equals(action)) {
                    if (event.getClick().isLeftClick() && !event.getClick().isShiftClick()) {
                        region.setUseCooldownCommand(!region.isUseCooldownCommand());
                        plugin.getRegionManager().saveRegion(region);
                        new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                    } else if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "extrc_cool_cmd_" + regionId);
                        player.sendMessage("§a[ExtractionRegion] §ePlease type the exact cooldown command (e.g. 'spawn %player%') for §b" + regionId + "§e:");
                    }
                } else if ("lockdown".equals(action)) {
                    region.setLockedDown(!region.isLockedDown());
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                } else if ("timer_extrc".equals(action)) {
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "region_interval_" + regionId);
                        player.sendMessage("§ePlease type the custom timer interval in minutes in chat (or type 'cancel' to abort).");
                        return;
                    }
                    
                    int current = region.getResetIntervalMinutes();
                    int next = 60; // 1h
                    if (current == 60) next = 120; // 2h
                    else if (current == 120) next = 360; // 6h
                    else if (current == 360) next = 720; // 12h
                    else if (current == 720) next = 1440; // 24h
                    else if (current == 1440) next = 60;
                    else next = 360;
                    
                    region.setResetIntervalMinutes(next);
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                } else if ("holo_menu".equals(action)) {
                    new com.criztiandev.extractionregion.gui.HologramSettingsGUI(plugin).openMenu(player, region);
                }
            }
            return;
        }

        // Handle Hologram Settings
        if (title.startsWith(com.criztiandev.extractionregion.gui.HologramSettingsGUI.TITLE)) {
            if (data.has(new NamespacedKey(plugin, "holo-action"), PersistentDataType.STRING)) {
                String action = data.get(new NamespacedKey(plugin, "holo-action"), PersistentDataType.STRING);
                String regionId = data.get(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING);
                if (regionId == null) return;

                SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                if (region == null) return;

                if ("back".equals(action)) {
                    new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                } else if ("offset_x".equals(action)) {
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "holo_offset_x_" + regionId);
                        player.sendMessage("§aPlease type the exact X offset for §e" + regionId + "§a:");
                        return;
                    }
                    double val = region.getHologramOffsetX();
                    if (event.getClick().isLeftClick()) val += 0.1;
                    else if (event.getClick().isRightClick()) val -= 0.1;
                    region.setHologramOffsetX(val);
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.HologramSettingsGUI(plugin).openMenu(player, region);
                } else if ("offset_y".equals(action)) {
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "holo_offset_y_" + regionId);
                        player.sendMessage("§aPlease type the exact Y offset for §e" + regionId + "§a:");
                        return;
                    }
                    double val = region.getHologramOffsetY();
                    if (event.getClick().isLeftClick()) val += 0.1;
                    else if (event.getClick().isRightClick()) val -= 0.1;
                    region.setHologramOffsetY(val);
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.HologramSettingsGUI(plugin).openMenu(player, region);
                } else if ("offset_z".equals(action)) {
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "holo_offset_z_" + regionId);
                        player.sendMessage("§aPlease type the exact Z offset for §e" + regionId + "§a:");
                        return;
                    }
                    double val = region.getHologramOffsetZ();
                    if (event.getClick().isLeftClick()) val += 0.1;
                    else if (event.getClick().isRightClick()) val -= 0.1;
                    region.setHologramOffsetZ(val);
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.HologramSettingsGUI(plugin).openMenu(player, region);
                } else if ("scale".equals(action)) {
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "holo_scale_" + regionId);
                        player.sendMessage("§aPlease type the exact scale (font size) for §e" + regionId + "§a:");
                        return;
                    }
                    double val = region.getHologramScale();
                    if (event.getClick().isLeftClick()) val += 0.1;
                    else if (event.getClick().isRightClick()) val = Math.max(0.1, val - 0.1);
                    region.setHologramScale(val);
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.HologramSettingsGUI(plugin).openMenu(player, region);
                } else if ("cleanup".equals(action)) {
                    plugin.getHologramManager().cleanupOldHolograms();
                    player.sendMessage("§aForce cleaned up old/broken holograms near all conduits and globally.");
                    player.closeInventory();
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
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "entry_slowfall_" + regionId);
                        player.sendMessage("§aPlease type the exact slow falling duration (in seconds) for §e" + regionId + "§a:");
                        return;
                    }
                    int sf = region.getSlowFallingSeconds();
                    if (event.getClick().isLeftClick()) {
                        if (sf == 0) region.setSlowFallingSeconds(5);
                        else if (sf == 5) region.setSlowFallingSeconds(10);
                        else if (sf == 10) region.setSlowFallingSeconds(15);
                        else region.setSlowFallingSeconds(0);
                    } else if (event.getClick().isRightClick()) {
                        if (sf == 0) region.setSlowFallingSeconds(15);
                        else if (sf == 5) region.setSlowFallingSeconds(0);
                        else if (sf == 10) region.setSlowFallingSeconds(5);
                        else region.setSlowFallingSeconds(10);
                    }
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.EntrySettingsGUI(plugin).openMenu(player, region);
                } else if ("blindness".equals(action)) {
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "entry_blindness_" + regionId);
                        player.sendMessage("§aPlease type the exact blindness duration (in seconds) for §e" + regionId + "§a:");
                        return;
                    }
                    int b = region.getBlindnessSeconds();
                    if (event.getClick().isLeftClick()) {
                        if (b == 0) region.setBlindnessSeconds(1);
                        else if (b == 1) region.setBlindnessSeconds(3);
                        else if (b == 3) region.setBlindnessSeconds(5);
                        else region.setBlindnessSeconds(0);
                    } else if (event.getClick().isRightClick()) {
                        if (b == 0) region.setBlindnessSeconds(5);
                        else if (b == 1) region.setBlindnessSeconds(0);
                        else if (b == 3) region.setBlindnessSeconds(1);
                        else region.setBlindnessSeconds(3);
                    }
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.EntrySettingsGUI(plugin).openMenu(player, region);
                } else if ("cooldown".equals(action)) {
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "entry_cooldown_" + regionId);
                        player.sendMessage("§aPlease type the exact entry cooldown (in minutes) for §e" + regionId + "§a:");
                        return;
                    }
                    int c = region.getEntryCooldownSeconds();
                    if (event.getClick().isLeftClick()) {
                        if (c == 0) region.setEntryCooldownSeconds(10);
                        else if (c == 10) region.setEntryCooldownSeconds(30);
                        else if (c == 30) region.setEntryCooldownSeconds(60);
                        else if (c == 60) region.setEntryCooldownSeconds(300);
                        else region.setEntryCooldownSeconds(0);
                    } else if (event.getClick().isRightClick()) {
                        if (c == 0) region.setEntryCooldownSeconds(300);
                        else if (c == 10) region.setEntryCooldownSeconds(0);
                        else if (c == 30) region.setEntryCooldownSeconds(10);
                        else if (c == 60) region.setEntryCooldownSeconds(30);
                        else region.setEntryCooldownSeconds(60);
                    }
                    plugin.getRegionManager().saveRegion(region);
                    new com.criztiandev.extractionregion.gui.EntrySettingsGUI(plugin).openMenu(player, region);
                } else if ("fallback_cmd".equals(action)) {
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "entry_fallback_" + regionId);
                        player.sendMessage("§aPlease type the Entry Fallback command for §e" + regionId + "§a:");
                        player.sendMessage("§7(Use %player% for the player's name. Example: spawn %player%)");
                    }
                } else if ("toggle_enabled".equals(action)) {
                    region.setEntryEnabled(!region.isEntryEnabled());
                    plugin.getRegionManager().saveRegion(region);
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    new com.criztiandev.extractionregion.gui.EntrySettingsGUI(plugin).openMenu(player, region);
                } else if ("req_armor".equals(action)) {
                    java.util.List<String> tiers = java.util.Arrays.asList("NONE", "LEATHER", "GOLDEN", "CHAINMAIL", "IRON", "DIAMOND", "NETHERITE");
                    String current = region.getRequiredArmorTier();
                    int idx = tiers.indexOf(current);
                    if (idx == -1) idx = 0;
                    
                    if (event.getClick().isLeftClick()) {
                        idx = (idx + 1) % tiers.size();
                    } else if (event.getClick().isRightClick()) {
                        idx = (idx - 1 + tiers.size()) % tiers.size();
                    }
                    region.setRequiredArmorTier(tiers.get(idx));
                    plugin.getRegionManager().saveRegion(region);
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    new com.criztiandev.extractionregion.gui.EntrySettingsGUI(plugin).openMenu(player, region);
                }
            }
            return;
        }

        // Handle Region Chests Category List (MUST be checked BEFORE RegionChestsGUI,
        // because the category title also starts with RegionChestsGUI.TITLE)
        if (title.startsWith("§8Region Chests - ")) {
            if (data.has(new NamespacedKey(plugin, "region-category-action"), PersistentDataType.STRING)) {
                String action = data.get(new NamespacedKey(plugin, "region-category-action"), PersistentDataType.STRING);
                String regionId = data.get(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING);
                if (regionId == null) return;
                SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                if (region == null) return;

                String tierName = data.get(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING);
                com.criztiandev.extractionchest.models.ChestTier tier = null;
                if (tierName != null) {
                    try {
                        tier = com.criztiandev.extractionchest.models.ChestTier.valueOf(tierName);
                    } catch (IllegalArgumentException ignored) {}
                }

                if ("back".equals(action)) {
                    new com.criztiandev.extractionregion.gui.RegionChestsGUI(plugin).openMenu(player, region, 0);
                } else if (tier != null && ("prev_page".equals(action) || "next_page".equals(action))) {
                    int page = data.get(new NamespacedKey(plugin, "pagination-page"), PersistentDataType.INTEGER);
                    new com.criztiandev.extractionregion.gui.RegionChestsCategoryGUI(plugin).openMenu(player, region, tier, page);
                } else if ("edit_chest".equals(action)) {
                    String instanceId = data.get(new NamespacedKey(plugin, "chest-id"), PersistentDataType.STRING);
                    String chestTierName = data.get(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING);
                    if (instanceId == null) return;

                    if (event.getClick().isShiftClick() && event.getClick().isLeftClick()) {
                        // Shift-Left-Click = teleport to chest
                        com.criztiandev.extractionchest.models.ChestInstance inst = plugin.getExtractionChestApi().getChestInstanceManager().getInstanceById(instanceId);
                        if (inst != null) {
                            org.bukkit.World w = org.bukkit.Bukkit.getWorld(inst.getWorld());
                            if (w != null) {
                                player.closeInventory();
                                player.teleport(inst.getLocation(w));
                                player.sendMessage("§aTeleported to the chest.");
                            }
                        }
                    } else if (event.getClick().isLeftClick() && !event.getClick().isShiftClick()) {
                        // Left-Click = open individual chest config
                        new com.criztiandev.extractionregion.gui.ChestConfigGUI(plugin).openMenu(player, region, instanceId, chestTierName);
                    }
                } else if ("bulk_configure_tier".equals(action)) {
                    String bulkTierName = data.get(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING);
                    if (bulkTierName == null) return;
                    try {
                        com.criztiandev.extractionchest.models.ChestTier bulkTier = com.criztiandev.extractionchest.models.ChestTier.valueOf(bulkTierName);
                        new com.criztiandev.extractionregion.gui.RegionChestsBulkGUI(plugin).openMenu(player, region, bulkTier);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        // Handle Region Chests Bulk GUI
        if (title.startsWith(com.criztiandev.extractionregion.gui.RegionChestsBulkGUI.TITLE)) {
            if (data.has(new NamespacedKey(plugin, "bulk-action"), PersistentDataType.STRING)) {
                String action = data.get(new NamespacedKey(plugin, "bulk-action"), PersistentDataType.STRING);
                String regionId = data.get(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING);
                if (regionId == null) return;
                SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                if (region == null) return;

                String bulkTierName = data.get(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING);
                if (bulkTierName == null) return;
                com.criztiandev.extractionchest.models.ChestTier bulkTier;
                try {
                    bulkTier = com.criztiandev.extractionchest.models.ChestTier.valueOf(bulkTierName);
                } catch (IllegalArgumentException e) {
                    return;
                }

                if ("back".equals(action)) {
                    new com.criztiandev.extractionregion.gui.RegionChestsCategoryGUI(plugin).openMenu(player, region, bulkTier, 0);
                } else if ("toggle_lock".equals(action)) {
                    int toggled = 0;
                    for (com.criztiandev.extractionchest.models.ChestInstance inst : plugin.getExtractionChestApi().getChestInstanceManager().getAllInstances()) {
                        if (!inst.getWorld().equals(region.getWorld())) continue;
                        org.bukkit.Location loc = inst.getLocation(org.bukkit.Bukkit.getWorld(inst.getWorld()));
                        if (loc.getBlockX() < region.getMinX() || loc.getBlockX() > region.getMaxX()) continue;
                        if (loc.getBlockZ() < region.getMinZ() || loc.getBlockZ() > region.getMaxZ()) continue;
                        com.criztiandev.extractionchest.models.ParentChestDefinition def = plugin.getExtractionChestApi().getLootTableManager().getDefinition(inst.getParentName());
                        if (def == null || def.getTier() != bulkTier) continue;
                        inst.setStationary(!inst.isStationary());
                        region.getChestStationaryOverrides().put(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ(), inst.isStationary());
                        plugin.getExtractionChestApi().getStorageProvider().saveInstance(inst);
                        toggled++;
                    }
                    plugin.getRegionManager().saveRegion(region);
                    player.sendMessage("§b[Bulk Configure] §aToggled Shuffling Lock for §e" + toggled + " §a" + bulkTierName + " chests.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    new com.criztiandev.extractionregion.gui.RegionChestsCategoryGUI(plugin).openMenu(player, region, bulkTier, 0);
                } else if ("set_chance".equals(action)) {
                    player.closeInventory();
                    plugin.getRegionManager().addPromptState(player.getUniqueId(), "bulk_chance_" + regionId + "_" + bulkTierName);
                    player.sendMessage("§b[Bulk Configure] §ePlease type the spawn chance (1-100) to apply to ALL §b" + bulkTierName + " §echests in §b" + regionId + "§e:");
                } else if ("set_fallback".equals(action)) {
                    player.closeInventory();
                    plugin.getRegionManager().addPromptState(player.getUniqueId(), "bulk_fallback_" + regionId + "_" + bulkTierName);
                    player.sendMessage("§b[Bulk Configure] §ePlease type the exact Loot Table to use as a fallback (or 'none' to remove fallback) to apply to ALL §b" + bulkTierName + " §echests in §b" + regionId + "§e:");
                }
            }
        }

        // Handle Region Chests overview (tier selector)
        if (title.startsWith(com.criztiandev.extractionregion.gui.RegionChestsGUI.TITLE)) {
            if (data.has(new NamespacedKey(plugin, "region-chests-action"), PersistentDataType.STRING)) {
                String action = data.get(new NamespacedKey(plugin, "region-chests-action"), PersistentDataType.STRING);
                String regionId = data.get(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING);
                if (regionId == null) return;
                SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                if (region == null) return;

                if ("back".equals(action)) {
                    new RegionActionGUI(plugin).openMenu(player, region);
                } else if ("view_tier".equals(action)) {
                    String tierName = data.get(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING);
                    if (tierName != null) {
                        try {
                            com.criztiandev.extractionchest.models.ChestTier tier = com.criztiandev.extractionchest.models.ChestTier.valueOf(tierName);
                            new com.criztiandev.extractionregion.gui.RegionChestsCategoryGUI(plugin).openMenu(player, region, tier, 0);
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            }
        }

        // Handle Chest Config GUI
        if (title.startsWith(com.criztiandev.extractionregion.gui.ChestConfigGUI.TITLE)) {
            if (data.has(new NamespacedKey(plugin, "chestcfg-action"), PersistentDataType.STRING)) {
                String action = data.get(new NamespacedKey(plugin, "chestcfg-action"), PersistentDataType.STRING);
                String regionId = data.get(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING);
                String chestId = data.get(new NamespacedKey(plugin, "chest-id"), PersistentDataType.STRING);
                if (regionId == null || chestId == null) return;
                SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                com.criztiandev.extractionchest.models.ChestInstance inst = plugin.getExtractionChestApi().getChestInstanceManager().getInstanceById(chestId);
                if (region == null || inst == null) return;

                String chestTierBack = data.get(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING);
                if ("back".equals(action)) {
                    if (chestTierBack != null) {
                        try {
                            com.criztiandev.extractionchest.models.ChestTier bt = com.criztiandev.extractionchest.models.ChestTier.valueOf(chestTierBack);
                            new com.criztiandev.extractionregion.gui.RegionChestsCategoryGUI(plugin).openMenu(player, region, bt, 0);
                        } catch (IllegalArgumentException e) {
                            new com.criztiandev.extractionregion.gui.RegionChestsGUI(plugin).openMenu(player, region, 0);
                        }
                    } else {
                        new com.criztiandev.extractionregion.gui.RegionChestsGUI(plugin).openMenu(player, region, 0);
                    }
                } else if ("toggle_stationary".equals(action)) {
                    inst.setStationary(!inst.isStationary());
                    org.bukkit.World w = org.bukkit.Bukkit.getWorld(inst.getWorld());
                    if (w != null) {
                        org.bukkit.Location loc = inst.getLocation(w);
                        region.getChestStationaryOverrides().put(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ(), inst.isStationary());
                        plugin.getRegionManager().saveRegion(region);
                    }
                    plugin.getExtractionChestApi().getStorageProvider().saveInstance(inst);
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    new com.criztiandev.extractionregion.gui.ChestConfigGUI(plugin).openMenu(player, region, chestId);
                } else if ("spawn_chance".equals(action)) {
                    if (event.getClick().isShiftClick()) {
                        player.closeInventory();
                        plugin.getRegionManager().addPromptState(player.getUniqueId(), "chest_chance_" + regionId + "_" + chestId);
                        player.sendMessage("§aPlease type the exact spawn chance (1-100) for this chest:");
                        return;
                    }
                    int chance = inst.getSpawnChance();
                    if (event.getClick().isLeftClick()) {
                        chance = Math.min(100, chance + 5);
                    } else if (event.getClick().isRightClick()) {
                        chance = Math.max(1, chance - 5);
                    }
                    inst.setSpawnChance(chance);
                    plugin.getExtractionChestApi().getStorageProvider().saveInstance(inst);
                    new com.criztiandev.extractionregion.gui.ChestConfigGUI(plugin).openMenu(player, region, chestId);
                } else if ("fallback_tier".equals(action)) {
                    player.closeInventory();
                    plugin.getRegionManager().addPromptState(player.getUniqueId(), "chest_fallback_" + regionId + "_" + chestId);
                    player.sendMessage("§aPlease type the exact Loot Table filename you want as the Fallback Tier:");
                    player.sendMessage("§7(Or type 'none' to remove the fallback)");
                }
            }
            return;
        }

    }

}
