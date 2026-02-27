package com.criztiandev.extractionregion.utils;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class WandUtil {

    public static ItemStack getWand(ExtractionRegionPlugin plugin, RegionType type) {
        Material mat;
        String name;
        switch (type) {
            case EXTRACTION:
                mat = Material.DIAMOND_HOE;
                name = "§b§lExtraction Region Wand";
                break;
            case ENTRY_REGION:
                mat = Material.IRON_HOE;
                name = "§a§lEntry Region Wand";
                break;
            case CHEST_REPLENISH:
            default:
                mat = Material.GOLDEN_HOE;
                name = "§6§lChest Region Wand";
                break;
        }

        ItemStack wand = new ItemStack(mat);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(
                "§7Left-Click a block to set Pos1.",
                "§7Right-Click a block to set Pos2.",
                "",
                "§eType: §f" + type.name().replace("_", " ")
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-wand"), PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "wand-type"), PersistentDataType.STRING, type.name());
            wand.setItemMeta(meta);
        }
        return wand;
    }

    public static boolean isWand(ExtractionRegionPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "region-wand"), PersistentDataType.BYTE);
    }
}
