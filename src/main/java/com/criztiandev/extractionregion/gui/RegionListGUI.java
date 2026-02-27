package com.criztiandev.extractionregion.gui;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public class RegionListGUI {

    private final ExtractionRegionPlugin plugin;
    public static final String TITLE = "§8Select a Region...";

    public RegionListGUI(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, int page, com.criztiandev.extractionregion.models.RegionType filterType) {
        List<SavedRegion> allRegions = new java.util.ArrayList<>(plugin.getRegionManager().getRegions());
        List<SavedRegion> regions = new java.util.ArrayList<>();
        if (filterType != null) {
            for (SavedRegion r : allRegions) {
                if (r.getType() == filterType) regions.add(r);
            }
        } else {
            regions.addAll(allRegions);
        }
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(" ");
            border.setItemMeta(bm);
        }
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        int maxItemsPerPage = 45;
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, regions.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            SavedRegion region = regions.get(i);
            
            boolean isExtraction = region.getType() == com.criztiandev.extractionregion.models.RegionType.EXTRACTION;
            boolean isEntry = region.getType() == com.criztiandev.extractionregion.models.RegionType.ENTRY_REGION;
            Material mat = isEntry ? Material.IRON_DOOR : (isExtraction ? Material.BEACON : Material.GRASS_BLOCK);
            String prefix = isEntry ? "§a[Entry] " : (isExtraction ? "§e[Extraction] §a" : "§a[Chest] ");
            
            ItemStack item = new ItemStack(mat); // Representing a region
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(prefix + region.getId());
                java.util.List<String> lore = new java.util.ArrayList<>(Arrays.asList(
                    "§7World: §f" + region.getWorld(),
                    "§7Pos1: §f" + region.getMinX() + ", " + region.getMinZ(),
                    "§7Pos2: §f" + region.getMaxX() + ", " + region.getMaxZ()
                ));

                if (isExtraction) {
                    lore.add("§7Type: §eExtraction Zone");
                } else if (isEntry) {
                    lore.add("§7Type: §aEntry/Drop Zone");
                } else {
                    long nextReset = region.getNextResetTime();
                    if (nextReset > 0) {
                        long remaining = nextReset - System.currentTimeMillis();
                        if (remaining > 0) {
                            lore.add("§eNext Reset: §f" + com.criztiandev.extractionregion.utils.TimeUtil.formatDuration(remaining));
                        } else {
                            lore.add("§eNext Reset: §cPending/Processing...");
                        }
                    } else {
                        lore.add("§eNext Reset: §fEvery " + region.getResetIntervalMinutes() + "m");
                    }
                }

                lore.add("");
                lore.add("§eClick to manage this region.");
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        // Back button
        ItemStack back = new ItemStack(Material.OAK_DOOR);
        ItemMeta bmkn = back.getItemMeta();
        if (bmkn != null) {
            bmkn.setDisplayName("§cGo Back");
            bmkn.getPersistentDataContainer().set(new NamespacedKey(plugin, "list-back"), PersistentDataType.STRING, "true");
            if (filterType != null) {
                bmkn.getPersistentDataContainer().set(new NamespacedKey(plugin, "pagination-filter"), PersistentDataType.STRING, filterType.name());
            }
            back.setItemMeta(bmkn);
        }
        inv.setItem(49, back);

        // Pagination buttons
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            if (pm != null) {
                pm.setDisplayName("§cPrevious Page");
                pm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "pagination-page"), org.bukkit.persistence.PersistentDataType.INTEGER, page - 1);
                if (filterType != null) {
                    pm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "pagination-filter"), org.bukkit.persistence.PersistentDataType.STRING, filterType.name());
                }
                prev.setItemMeta(pm);
            }
            inv.setItem(45, prev);
        }

        if (endIndex < regions.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            if (nm != null) {
                nm.setDisplayName("§aNext Page");
                nm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "pagination-page"), org.bukkit.persistence.PersistentDataType.INTEGER, page + 1);
                if (filterType != null) {
                    nm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "pagination-filter"), org.bukkit.persistence.PersistentDataType.STRING, filterType.name());
                }
                next.setItemMeta(nm);
            }
            inv.setItem(53, next);
        }

        player.openInventory(inv);
    }
}

