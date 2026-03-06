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

public class RegionChestsBulkGUI {

    private final ExtractionRegionPlugin plugin;
    public static final String TITLE = "§8Bulk Configure - ";

    public RegionChestsBulkGUI(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, SavedRegion region, com.criztiandev.extractionchest.models.ChestTier tier) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE + tier.name());

        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(" ");
            border.setItemMeta(bm);
        }
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }

        // Toggle Shuffling Lock
        ItemStack lockItem = new ItemStack(Material.LEVER);
        ItemMeta lockMeta = lockItem.getItemMeta();
        if (lockMeta != null) {
            lockMeta.setDisplayName("§eToggle Shuffling Lock");
            lockMeta.setLore(Arrays.asList(
                "§7Click to toggle the Shuffling Lock",
                "§7(Stationary State) for ALL §f" + tier.name() + " §7chests."
            ));
            lockMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bulk-action"), PersistentDataType.STRING, "toggle_lock");
            lockMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            lockMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING, tier.name());
            lockItem.setItemMeta(lockMeta);
        }
        inv.setItem(11, lockItem);

        // Set Spawn Chance
        ItemStack chanceItem = new ItemStack(Material.EMERALD);
        ItemMeta chanceMeta = chanceItem.getItemMeta();
        if (chanceMeta != null) {
            chanceMeta.setDisplayName("§aSet Spawn Chance");
            chanceMeta.setLore(Arrays.asList(
                "§7Click to set the exact spawn chance",
                "§7for ALL §f" + tier.name() + " §7chests via chat."
            ));
            chanceMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bulk-action"), PersistentDataType.STRING, "set_chance");
            chanceMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            chanceMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING, tier.name());
            chanceItem.setItemMeta(chanceMeta);
        }
        inv.setItem(13, chanceItem);

        // Set Fallback Tier
        ItemStack fallbackItem = new ItemStack(Material.TRAPPED_CHEST);
        ItemMeta fallbackMeta = fallbackItem.getItemMeta();
        if (fallbackMeta != null) {
            fallbackMeta.setDisplayName("§6Set Fallback Tier");
            fallbackMeta.setLore(Arrays.asList(
                "§7Click to specify the fallback Loot Table",
                "§7for ALL §f" + tier.name() + " §7chests via chat."
            ));
            fallbackMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bulk-action"), PersistentDataType.STRING, "set_fallback");
            fallbackMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            fallbackMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING, tier.name());
            fallbackItem.setItemMeta(fallbackMeta);
        }
        inv.setItem(15, fallbackItem);

        // Back button
        ItemStack back = new ItemStack(Material.OAK_DOOR);
        ItemMeta bmkn = back.getItemMeta();
        if (bmkn != null) {
            bmkn.setDisplayName("§cGo Back");
            bmkn.getPersistentDataContainer().set(new NamespacedKey(plugin, "bulk-action"), PersistentDataType.STRING, "back");
            bmkn.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            bmkn.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING, tier.name());
            back.setItemMeta(bmkn);
        }
        inv.setItem(22, back);

        player.openInventory(inv);
    }
}
