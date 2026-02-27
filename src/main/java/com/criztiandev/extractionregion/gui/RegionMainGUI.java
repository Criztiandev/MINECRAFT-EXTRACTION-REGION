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

        // Chest Region Button
        ItemStack chestItem = new ItemStack(Material.CHEST);
        ItemMeta chestMeta = chestItem.getItemMeta();
        if (chestMeta != null) {
            chestMeta.setDisplayName("§6§lChest Regions");
            chestMeta.setLore(Arrays.asList(
                "§7Manage regions that automatically",
                "§7replenish loot chests inside."
            ));
            chestMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-main"), PersistentDataType.STRING, "cat_chest");
            chestItem.setItemMeta(chestMeta);
        }
        inv.setItem(11, chestItem);

        // Extraction Region Button
        ItemStack extItem = new ItemStack(Material.BEACON);
        ItemMeta extMeta = extItem.getItemMeta();
        if (extMeta != null) {
            extMeta.setDisplayName("§b§lExtraction Regions");
            extMeta.setLore(Arrays.asList(
                "§7Manage regions where players",
                "§7can extract to safety with loot."
            ));
            extMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-main"), PersistentDataType.STRING, "cat_extraction");
            extItem.setItemMeta(extMeta);
        }
        inv.setItem(13, extItem);

        // Entry Region Button
        ItemStack entryItem = new ItemStack(Material.IRON_DOOR);
        ItemMeta entryMeta = entryItem.getItemMeta();
        if (entryMeta != null) {
            entryMeta.setDisplayName("§a§lEntry Regions");
            entryMeta.setLore(Arrays.asList(
                "§7Manage entry or spawn zones."
            ));
            entryMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-main"), PersistentDataType.STRING, "cat_entry");
            entryItem.setItemMeta(entryMeta);
        }
        inv.setItem(15, entryItem);

        // Get Wands Button
        ItemStack wandItem = new ItemStack(Material.STICK);
        ItemMeta wandMeta = wandItem.getItemMeta();
        if (wandMeta != null) {
            wandMeta.setDisplayName("§d§lGet Region Wands");
            wandMeta.setLore(Arrays.asList(
                "§7Click to open the wand",
                "§7selection menu."
            ));
            wandMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-main"), PersistentDataType.STRING, "wands");
            wandItem.setItemMeta(wandMeta);
        }
        inv.setItem(22, wandItem);

        // Global Visibility Toggle Button (Slot 26)
        ItemStack visItem = new ItemStack(Material.ENDER_EYE);
        ItemMeta visMeta = visItem.getItemMeta();
        if (visMeta != null) {
            boolean isVisible = plugin.getConfig().getBoolean("region.always-show-regions", false);
            visMeta.setDisplayName(isVisible ? "§a§lGlobal Visibility: ON" : "§c§lGlobal Visibility: OFF");
            visMeta.setLore(Arrays.asList(
                "§7When enabled, Admins will",
                "§7always see the boundaries of",
                "§7any region they are standing in,",
                "§7even without holding a wand.",
                "",
                "§eClick to toggle this setting."
            ));
            visMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-main"), PersistentDataType.STRING, "toggle_visibility");
            visItem.setItemMeta(visMeta);
        }
        inv.setItem(26, visItem);

        player.openInventory(inv);
    }
}
