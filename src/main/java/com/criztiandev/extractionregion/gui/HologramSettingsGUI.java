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

public class HologramSettingsGUI {

    private final ExtractionRegionPlugin plugin;
    public static final String TITLE = "§d§lHologram Settings";

    public HologramSettingsGUI(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, SavedRegion region) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE + " - " + region.getId());

        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(" ");
            border.setItemMeta(bm);
        }
        for (int i = 0; i < 36; i++) inv.setItem(i, border);

        // Slot 11: X Offset
        ItemStack xItem = new ItemStack(Material.RED_STAINED_GLASS);
        ItemMeta xMeta = xItem.getItemMeta();
        if (xMeta != null) {
            xMeta.setDisplayName("§c§lX Offset");
            xMeta.setLore(Arrays.asList(
                "§7Current: §f" + String.format("%.2f", region.getHologramOffsetX()),
                "",
                "§eLeft-Click: §f+0.1",
                "§cRight-Click: §f-0.1",
                "§dShift-Click: §fSet exact value"
            ));
            xMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "holo-action"), PersistentDataType.STRING, "offset_x");
            xMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            xItem.setItemMeta(xMeta);
        }
        inv.setItem(11, xItem);

        // Slot 13: Y Offset
        ItemStack yItem = new ItemStack(Material.LIME_STAINED_GLASS);
        ItemMeta yMeta = yItem.getItemMeta();
        if (yMeta != null) {
            yMeta.setDisplayName("§a§lY Offset");
            yMeta.setLore(Arrays.asList(
                "§7Current: §f" + String.format("%.2f", region.getHologramOffsetY()),
                "",
                "§eLeft-Click: §f+0.1",
                "§cRight-Click: §f-0.1",
                "§dShift-Click: §fSet exact value"
            ));
            yMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "holo-action"), PersistentDataType.STRING, "offset_y");
            yMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            yItem.setItemMeta(yMeta);
        }
        inv.setItem(13, yItem);

        // Slot 15: Z Offset
        ItemStack zItem = new ItemStack(Material.BLUE_STAINED_GLASS);
        ItemMeta zMeta = zItem.getItemMeta();
        if (zMeta != null) {
            zMeta.setDisplayName("§9§lZ Offset");
            zMeta.setLore(Arrays.asList(
                "§7Current: §f" + String.format("%.2f", region.getHologramOffsetZ()),
                "",
                "§eLeft-Click: §f+0.1",
                "§cRight-Click: §f-0.1",
                "§dShift-Click: §fSet exact value"
            ));
            zMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "holo-action"), PersistentDataType.STRING, "offset_z");
            zMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            zItem.setItemMeta(zMeta);
        }
        inv.setItem(15, zItem);

        // Slot 22: Scale (Font Size)
        ItemStack scaleItem = new ItemStack(Material.NAME_TAG);
        ItemMeta scaleMeta = scaleItem.getItemMeta();
        if (scaleMeta != null) {
            scaleMeta.setDisplayName("§6§lScale (Font Size)");
            scaleMeta.setLore(Arrays.asList(
                "§7Current: §f" + String.format("%.2f", region.getHologramScale()),
                "§7Adjust the overall size of the text.",
                "",
                "§eLeft-Click: §f+0.1",
                "§cRight-Click: §f-0.1",
                "§dShift-Click: §fSet exact value"
            ));
            scaleMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "holo-action"), PersistentDataType.STRING, "scale");
            scaleMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            scaleItem.setItemMeta(scaleMeta);
        }
        inv.setItem(22, scaleItem);

        // Slot 31: Force Cleanup
        ItemStack cleanupItem = new ItemStack(Material.SPONGE);
        ItemMeta cleanupMeta = cleanupItem.getItemMeta();
        if (cleanupMeta != null) {
            cleanupMeta.setDisplayName("§e§lForce Cleanup Holograms");
            cleanupMeta.setLore(Arrays.asList(
                "§7Click to force remove",
                "§7legacy/stuck holograms",
                "§7and text displays near",
                "§7this extraction point."
            ));
            cleanupMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "holo-action"), PersistentDataType.STRING, "cleanup");
            cleanupMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            cleanupItem.setItemMeta(cleanupMeta);
        }
        inv.setItem(31, cleanupItem);

        // Back Button (Slot 27)
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cGo Back to Map");
            backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "holo-action"), PersistentDataType.STRING, "back");
            backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(27, backItem);

        player.openInventory(inv);
    }
}
