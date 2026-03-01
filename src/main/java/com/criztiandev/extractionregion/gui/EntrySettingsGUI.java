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
                "§eLeft-Click: §fCycle presets",
                "§cRight-Click: §fCycle presets",
                "§dShift-Click: §fSet exact value in chat"
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
                "§eLeft-Click: §fCycle presets",
                "§cRight-Click: §fCycle presets",
                "§dShift-Click: §fSet exact value in chat"
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
                "§eLeft-Click: §fCycle presets",
                "§cRight-Click: §fCycle presets",
                "§dShift-Click: §fSet exact value in chat"
            ));
            cooldownMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "entry-action"), PersistentDataType.STRING, "cooldown");
            cooldownMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            cooldownItem.setItemMeta(cooldownMeta);
        }
        inv.setItem(13, cooldownItem);

        // Slot 22: Fallback TP Command
        ItemStack fallbackItem = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta fallbackMeta = fallbackItem.getItemMeta();
        if (fallbackMeta != null) {
            fallbackMeta.setDisplayName("§c§lFallback TP Command");
            fallbackMeta.setLore(Arrays.asList(
                "§7Command executed if player triggers",
                "§7the Drop Zone while on Cooldown.",
                "",
                "§7Current: §f" + region.getEntryFallbackCommand(),
                "",
                "§dShift-Click: §fType command in chat",
                "§8(Use %player% for their name)"
            ));
            fallbackMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "entry-action"), PersistentDataType.STRING, "fallback_cmd");
            fallbackMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            fallbackItem.setItemMeta(fallbackMeta);
        }
        inv.setItem(22, fallbackItem);

        // Slot 24: Maintenance Mode Toggle
        ItemStack maintenanceItem = new ItemStack(region.isEntryEnabled() ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta maintenanceMeta = maintenanceItem.getItemMeta();
        if (maintenanceMeta != null) {
            maintenanceMeta.setDisplayName(region.isEntryEnabled() ? "§a§lStatus: ENABLED" : "§c§lStatus: UNDER MAINTENANCE");
            maintenanceMeta.setLore(Arrays.asList(
                "§7If disabled, players cannot drop in.",
                "§7They will be teleported via the",
                "§7Fallback TP Command.",
                "",
                "§eClick to toggle"
            ));
            maintenanceMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "entry-action"), PersistentDataType.STRING, "toggle_enabled");
            maintenanceMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            maintenanceItem.setItemMeta(maintenanceMeta);
        }
        inv.setItem(24, maintenanceItem);

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
