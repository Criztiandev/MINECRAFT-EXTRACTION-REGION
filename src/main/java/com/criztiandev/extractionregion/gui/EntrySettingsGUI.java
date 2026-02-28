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

public class EntrySettingsGUI {

    private final ExtractionRegionPlugin plugin;
    public static final String TITLE = "§8Entry Settings";

    public EntrySettingsGUI(ExtractionRegionPlugin plugin) {
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

        // Slot 11: Slow Falling Duration
        ItemStack slowFallItem = new ItemStack(Material.FEATHER);
        ItemMeta slowFallMeta = slowFallItem.getItemMeta();
        if (slowFallMeta != null) {
            slowFallMeta.setDisplayName("§b§lSlow Falling Duration");
            slowFallMeta.setLore(Arrays.asList(
                "§7Current: §f" + region.getSlowFallingSeconds() + " Seconds",
                "",
                "§eClick to cycle:",
                "§e0s -> 5s -> 10s -> 15s"
            ));
            slowFallMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "entry-action"), PersistentDataType.STRING, "slowfall");
            slowFallMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            slowFallItem.setItemMeta(slowFallMeta);
        }
        inv.setItem(11, slowFallItem);

        // Slot 15: Blindness Duration
        ItemStack blindItem = new ItemStack(Material.ENDER_PEARL);
        ItemMeta blindMeta = blindItem.getItemMeta();
        if (blindMeta != null) {
            blindMeta.setDisplayName("§8§lBlindness Duration");
            blindMeta.setLore(Arrays.asList(
                "§7Current: §f" + region.getBlindnessSeconds() + " Seconds",
                "",
                "§eClick to cycle:",
                "§e0s -> 1s -> 3s -> 5s"
            ));
            blindMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "entry-action"), PersistentDataType.STRING, "blindness");
            blindMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            blindItem.setItemMeta(blindMeta);
        }
        inv.setItem(15, blindItem);

        // Slot 13: Entry Cooldown
        ItemStack cooldownItem = new ItemStack(Material.CLOCK);
        ItemMeta cooldownMeta = cooldownItem.getItemMeta();
        if (cooldownMeta != null) {
            cooldownMeta.setDisplayName("§6§lEntry Cooldown");
            cooldownMeta.setLore(Arrays.asList(
                "§7Current: §f" + region.getEntryCooldownMinutes() + " Minutes",
                "",
                "§eClick to cycle:",
                "§e0m -> 1m -> 5m -> 10m -> 30m"
            ));
            cooldownMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "entry-action"), PersistentDataType.STRING, "cooldown");
            cooldownMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            cooldownItem.setItemMeta(cooldownMeta);
        }
        inv.setItem(13, cooldownItem);

        // Back Button (Slot 18)
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cGo Back to Actions");
            backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "entry-action"), PersistentDataType.STRING, "back");
            backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(18, backItem);

        player.openInventory(inv);
    }
}
