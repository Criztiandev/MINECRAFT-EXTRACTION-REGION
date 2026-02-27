package com.criztiandev.extractionregion.gui;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionType;
import com.criztiandev.extractionregion.utils.WandUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class WandMenuGUI {

    private final ExtractionRegionPlugin plugin;
    public static final String TITLE = "§8Select a Region Wand...";

    public WandMenuGUI(ExtractionRegionPlugin plugin) {
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

        // Chest Wand Button (Slot 11)
        ItemStack chestWand = WandUtil.getWand(plugin, RegionType.CHEST_REPLENISH);
        ItemMeta cwMeta = chestWand.getItemMeta();
        if (cwMeta != null) {
            java.util.List<String> lore = cwMeta.getLore();
            if (lore == null) lore = new java.util.ArrayList<>();
            lore.add("");
            lore.add("§aClick to receive.");
            cwMeta.setLore(lore);
            cwMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "wand-menu-action"), PersistentDataType.STRING, RegionType.CHEST_REPLENISH.name());
            chestWand.setItemMeta(cwMeta);
        }
        inv.setItem(11, chestWand);

        // Extraction Wand Button (Slot 13)
        ItemStack extWand = WandUtil.getWand(plugin, RegionType.EXTRACTION);
        ItemMeta ewMeta = extWand.getItemMeta();
        if (ewMeta != null) {
            java.util.List<String> lore = ewMeta.getLore();
            if (lore == null) lore = new java.util.ArrayList<>();
            lore.add("");
            lore.add("§aClick to receive.");
            ewMeta.setLore(lore);
            ewMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "wand-menu-action"), PersistentDataType.STRING, RegionType.EXTRACTION.name());
            extWand.setItemMeta(ewMeta);
        }
        inv.setItem(13, extWand);

        // Entry Wand Button (Slot 15)
        ItemStack entryWand = WandUtil.getWand(plugin, RegionType.ENTRY_REGION);
        ItemMeta enMeta = entryWand.getItemMeta();
        if (enMeta != null) {
            java.util.List<String> lore = enMeta.getLore();
            if (lore == null) lore = new java.util.ArrayList<>();
            lore.add("");
            lore.add("§aClick to receive.");
            enMeta.setLore(lore);
            enMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "wand-menu-action"), PersistentDataType.STRING, RegionType.ENTRY_REGION.name());
            entryWand.setItemMeta(enMeta);
        }
        inv.setItem(15, entryWand);

        // Back Button (Slot 18)
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cGo Back");
            backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "wand-menu-action"), PersistentDataType.STRING, "back");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(18, backItem);

        player.openInventory(inv);
    }
}
