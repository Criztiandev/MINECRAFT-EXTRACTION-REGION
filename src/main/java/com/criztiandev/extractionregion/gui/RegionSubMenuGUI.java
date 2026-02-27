package com.criztiandev.extractionregion.gui;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class RegionSubMenuGUI {

    private final ExtractionRegionPlugin plugin;
    public static final String TITLE_PREFIX = "§8Region Menu: ";

    public RegionSubMenuGUI(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, RegionType type) {
        String typeName = type == RegionType.CHEST_REPLENISH ? "Chest" : 
                          type == RegionType.EXTRACTION ? "Extraction" : "Entry";
                          
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PREFIX + typeName);

        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(" ");
            border.setItemMeta(bm);
        }
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // Get Wand Button
        org.bukkit.inventory.ItemStack wandItem = com.criztiandev.extractionregion.utils.WandUtil.getWand(plugin, type);
        org.bukkit.inventory.meta.ItemMeta wandMeta = wandItem.getItemMeta();
        if (wandMeta != null) {
            wandMeta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "region-submenu"), org.bukkit.persistence.PersistentDataType.STRING, "wand");
            wandMeta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "region-type"), org.bukkit.persistence.PersistentDataType.STRING, type.name());
            wandItem.setItemMeta(wandMeta);
        }
        inv.setItem(11, wandItem);

        // Create Region Button
        ItemStack createItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta createMeta = createItem.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName("§a§lCreate " + typeName + " Region");
            createMeta.setLore(Arrays.asList(
                "§7Click to create a new region",
                "§7from your current wand selection.",
                "§7You will be prompted to type",
                "§7the Region ID in chat."
            ));
            createMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-submenu"), PersistentDataType.STRING, "create");
            createMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-type"), PersistentDataType.STRING, type.name());
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
                "§7" + typeName + " Regions and edit them."
            ));
            manageMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-submenu"), PersistentDataType.STRING, "manage");
            manageMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-type"), PersistentDataType.STRING, type.name());
            manageItem.setItemMeta(manageMeta);
        }
        inv.setItem(15, manageItem);
        
        // Back Button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cGo Back");
            backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-submenu"), PersistentDataType.STRING, "back");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(18, backItem);

        player.openInventory(inv);
    }
}
