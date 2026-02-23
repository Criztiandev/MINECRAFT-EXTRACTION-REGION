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

    public void openMenu(Player player, int page) {
        List<SavedRegion> regions = new java.util.ArrayList<>(plugin.getRegionManager().getRegions());
        
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
            ItemStack item = new ItemStack(Material.GRASS_BLOCK); // Representing a region
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a" + region.getId());
                meta.setLore(Arrays.asList(
                    "§7World: §f" + region.getWorld(),
                    "§7Pos1: §f" + region.getMinX() + ", " + region.getMinZ(),
                    "§7Pos2: §f" + region.getMaxX() + ", " + region.getMaxZ(),
                    "§7Auto-Spawns Configured: §f" + region.getAutoSpawns().size(),
                    "",
                    "§eClick to manage this region."
                ));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        // Pagination buttons
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            if (pm != null) {
                pm.setDisplayName("§cPrevious Page");
                pm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "pagination-page"), org.bukkit.persistence.PersistentDataType.INTEGER, page - 1);
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
                next.setItemMeta(nm);
            }
            inv.setItem(53, next);
        }

        player.openInventory(inv);
    }
}

