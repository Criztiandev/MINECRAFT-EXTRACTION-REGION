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

public class RegionActionGUI {

    private final ExtractionRegionPlugin plugin;
    public static final String TITLE = "§8Region Actions";

    public RegionActionGUI(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, SavedRegion region) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE + " - " + region.getId());

        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(" ");
            border.setItemMeta(bm);
        }
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // Spawn Mode Button
        ItemStack modeItem = new ItemStack(Material.REPEATER);
        ItemMeta modeMeta = modeItem.getItemMeta();
        if (modeMeta != null) {
            modeMeta.setDisplayName("§e§lSpawn Mode: §f" + region.getSpawnMode().name());
            modeMeta.setLore(Arrays.asList(
                "§7Click to toggle between",
                "§7RANDOM and SPECIFIC modes."
            ));
            modeMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-action"), PersistentDataType.STRING, "mode");
            modeMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            modeItem.setItemMeta(modeMeta);
        }
        inv.setItem(11, modeItem);

        // Capture Button
        ItemStack captureItem = new ItemStack(Material.TARGET);
        ItemMeta captureMeta = captureItem.getItemMeta();
        if (captureMeta != null) {
            captureMeta.setDisplayName("§d§lCapture Chests");
            captureMeta.setLore(Arrays.asList(
                "§7Click to save the exact locations",
                "§7of manually placed Extraction",
                "§7Chests in this region."
            ));
            captureMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-action"), PersistentDataType.STRING, "capture");
            captureMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            captureItem.setItemMeta(captureMeta);
        }
        inv.setItem(12, captureItem);

        // Auto Spawns Button
        ItemStack spawnsItem = new ItemStack(Material.DISPENSER);
        ItemMeta spawnsMeta = spawnsItem.getItemMeta();
        if (spawnsMeta != null) {
            spawnsMeta.setDisplayName("§b§lManage Chest Spawns");
            spawnsMeta.setLore(Arrays.asList(
                "§7Click to open a drop",
                "§7inventory where you can",
                "§7place Extraction Chests."
            ));
            spawnsMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-action"), PersistentDataType.STRING, "chest");
            spawnsMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            spawnsItem.setItemMeta(spawnsMeta);
        }
        inv.setItem(13, spawnsItem);

        // Force Spawn Button
        ItemStack forceItem = new ItemStack(Material.BEACON);
        ItemMeta forceMeta = forceItem.getItemMeta();
        if (forceMeta != null) {
            forceMeta.setDisplayName("§a§lTrigger Auto-Spawns");
            forceMeta.setLore(Arrays.asList(
                "§7Click to immediately scatter",
                "§7the configured chests into",
                "§7this region right now."
            ));
            forceMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-action"), PersistentDataType.STRING, "force");
            forceMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            forceItem.setItemMeta(forceMeta);
        }
        inv.setItem(14, forceItem);
        
        // Delete Region Button
        ItemStack deleteItem = new ItemStack(Material.BARRIER);
        ItemMeta deleteMeta = deleteItem.getItemMeta();
        if (deleteMeta != null) {
            deleteMeta.setDisplayName("§c§lDelete Region");
            deleteMeta.setLore(Arrays.asList(
                "§7Click to permanently delete",
                "§7this region boundary."
            ));
            deleteMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-action"), PersistentDataType.STRING, "delete");
            deleteMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            deleteItem.setItemMeta(deleteMeta);
        }
        inv.setItem(16, deleteItem);
        
        // Get Region Wand Button
        ItemStack getWandItem = new ItemStack(Material.STICK);
        ItemMeta getWandMeta = getWandItem.getItemMeta();
        if (getWandMeta != null) {
            getWandMeta.setDisplayName("§d§lGet Region Wand");
            getWandMeta.setLore(Arrays.asList(
                "§7Click to receive a wand",
                "§7that shows the borders",
                "§7of this specific region."
            ));
            getWandMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-action"), PersistentDataType.STRING, "getwand");
            getWandMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            getWandItem.setItemMeta(getWandMeta);
        }
        inv.setItem(22, getWandItem);
        
        // Back Button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cGo Back");
            backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-action"), PersistentDataType.STRING, "back");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(18, backItem);

        player.openInventory(inv);
    }
}

