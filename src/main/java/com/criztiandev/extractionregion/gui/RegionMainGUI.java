package com.criztiandev.extractionregion.gui;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class RegionMainGUI {

    private final ExtractionRegionPlugin plugin;
    public static final String TITLE = "§8Extraction Region Editor";

    public RegionMainGUI(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(" ");
            border.setItemMeta(bm);
        }
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // Get Wand Button
        ItemStack wandItem = new ItemStack(Material.STICK);
        ItemMeta wandMeta = wandItem.getItemMeta();
        if (wandMeta != null) {
            wandMeta.setDisplayName("§d§lGet Region Wand");
            wandMeta.setLore(Arrays.asList(
                "§7Click to receive the selection wand.",
                "§7Left-click to set Position 1.",
                "§7Right-click to set Position 2."
            ));
            wandMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-main"), PersistentDataType.STRING, "wand");
            wandItem.setItemMeta(wandMeta);
        }
        inv.setItem(11, wandItem);

        // Create Region Button
        ItemStack createItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta createMeta = createItem.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName("§a§lCreate Region");
            createMeta.setLore(Arrays.asList(
                "§7Click to create a new region",
                "§7from your current wand selection.",
                "§7You will be prompted to type",
                "§7the Region ID in chat."
            ));
            createMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-main"), PersistentDataType.STRING, "create");
            createItem.setItemMeta(createMeta);
        }
        inv.setItem(13, createItem);

        // Manage Regions Button
        ItemStack manageItem = new ItemStack(Material.BOOK);
        ItemMeta manageMeta = manageItem.getItemMeta();
        if (manageMeta != null) {
            manageMeta.setDisplayName("§e§lManage Regions");
            manageMeta.setLore(Arrays.asList(
                "§7Click to view a list of all",
                "§7Extraction Regions, configure",
                "§7auto-spawns, and delete them."
            ));
            manageMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-main"), PersistentDataType.STRING, "manage");
            manageItem.setItemMeta(manageMeta);
        }
        inv.setItem(15, manageItem);

        player.openInventory(inv);
    }
}
