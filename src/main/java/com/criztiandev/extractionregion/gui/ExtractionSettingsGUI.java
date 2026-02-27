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

public class ExtractionSettingsGUI {

    private final ExtractionRegionPlugin plugin;
    public static final String TITLE = "§8Extraction Settings";

    public ExtractionSettingsGUI(ExtractionRegionPlugin plugin) {
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

        // Slot 11: Cooldown
        ItemStack cooldownItem = new ItemStack(Material.CLOCK);
        ItemMeta cooldownMeta = cooldownItem.getItemMeta();
        if (cooldownMeta != null) {
            cooldownMeta.setDisplayName("§e§lCooldown Timer");
            cooldownMeta.setLore(Arrays.asList(
                "§7Current: §f" + region.getCooldownMinutes() + " Minutes",
                "",
                "§eClick to cycle:",
                "§e5m -> 10m -> 15m -> 30m -> 60m"
            ));
            cooldownMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING, "cooldown");
            cooldownMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            cooldownItem.setItemMeta(cooldownMeta);
        }
        inv.setItem(11, cooldownItem);

        // Slot 13: Capacity RNG
        ItemStack capacityItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta capacityMeta = capacityItem.getItemMeta();
        if (capacityMeta != null) {
            capacityMeta.setDisplayName("§b§lCapacity Bounds (RNG)");
            capacityMeta.setLore(Arrays.asList(
                "§7Min: §f" + region.getMinCapacity() + " Players",
                "§7Max: §f" + region.getMaxCapacity() + " Players",
                "",
                "§eClick to cycle presets:",
                "§e(1-1) -> (1-3) -> (3-5) -> (5-5)"
            ));
            capacityMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING, "capacity");
            capacityMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            capacityItem.setItemMeta(capacityMeta);
        }
        inv.setItem(13, capacityItem);

        // Slot 15: Mimic Toggle
        boolean mimics = region.isMimicEnabled();
        ItemStack mimicItem = new ItemStack(mimics ? Material.REDSTONE_TORCH : Material.LEVER);
        ItemMeta mimicMeta = mimicItem.getItemMeta();
        if (mimicMeta != null) {
            mimicMeta.setDisplayName("§c§lMimic Conduit Trap");
            mimicMeta.setLore(Arrays.asList(
                "§7Enabled: " + (mimics ? "§aYes" : "§cNo"),
                "§7If true, there's a 5% chance",
                "§7the extraction is a fake and",
                "§7detonates instead!",
                "",
                "§eClick to toggle."
            ));
            mimicMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING, "mimic");
            mimicMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            mimicItem.setItemMeta(mimicMeta);
        }
        inv.setItem(15, mimicItem);

        // Back Button (Slot 18)
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cGo Back to Actions");
            backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING, "back");
            backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(18, backItem);

        player.openInventory(inv);
    }
}
