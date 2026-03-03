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

public class ChestConfigGUI {

    private final ExtractionRegionPlugin plugin;
    public static final String TITLE = "§8Chest Configuration";

    public ChestConfigGUI(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, SavedRegion region, String chestInstanceId) {
        openMenu(player, region, chestInstanceId, null);
    }

    public void openMenu(Player player, SavedRegion region, String chestInstanceId, String tierName) {
        com.criztiandev.extractionchest.models.ChestInstance inst = plugin.getExtractionChestApi().getChestInstanceManager().getInstanceById(chestInstanceId);
        if (inst == null) {
            player.sendMessage("§cCould not find that chest instance!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 45, TITLE);

        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(" ");
            border.setItemMeta(bm);
        }
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }

        // Chest Info item
        ItemStack info = new ItemStack(Material.CHEST);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§6Chest Information");
            infoMeta.setLore(Arrays.asList(
                "§7Location: §f" + inst.getX() + ", " + inst.getY() + ", " + inst.getZ(),
                "§7Primary Tier: §f" + inst.getParentName()
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(13, info);

        // Stationary Toggle
        ItemStack statItem = new ItemStack(inst.isStationary() ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta statMeta = statItem.getItemMeta();
        if (statMeta != null) {
            statMeta.setDisplayName(inst.isStationary() ? "§aStationary (Guaranteed) Mode" : "§cDynamic (Weighted) Mode");
            statMeta.setLore(Arrays.asList(
                "§7Currently: " + (inst.isStationary() ? "§aStationary" : "§cDynamic"),
                "",
                "§fStationary: §7This chest ignores shuffle rules",
                "§7and is guaranteed to spawn exactly where it",
                "§7is as its Primary Tier.",
                "",
                "§fDynamic: §7This chest can optionally fail its",
                "§7spawn roll and downgrade to a Fallback Tier.",
                "",
                "§eClick to toggle mode"
            ));
            statMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chestcfg-action"), PersistentDataType.STRING, "toggle_stationary");
            statMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-id"), PersistentDataType.STRING, chestInstanceId);
            statMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            statItem.setItemMeta(statMeta);
        }
        inv.setItem(21, statItem);

        if (!inst.isStationary()) {
            // Spawn Chance
            ItemStack chanceItem = new ItemStack(Material.EMERALD);
            ItemMeta chanceMeta = chanceItem.getItemMeta();
            if (chanceMeta != null) {
                chanceMeta.setDisplayName("§aPrimary Tier Spawn Chance");
                chanceMeta.setLore(Arrays.asList(
                    "§7Chance: §f" + inst.getSpawnChance() + "%",
                    "",
                    "§7The % chance this chest will spawn",
                    "§7as its Primary Tier (§f" + inst.getParentName() + "§7).",
                    "",
                    "§eLeft-Click to Add +5%",
                    "§eRight-Click to Subtract -5%",
                    "§eShift-Click to type exact % in chat"
                ));
                chanceMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chestcfg-action"), PersistentDataType.STRING, "spawn_chance");
                chanceMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-id"), PersistentDataType.STRING, chestInstanceId);
                chanceMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
                chanceItem.setItemMeta(chanceMeta);
            }
            inv.setItem(23, chanceItem);

            // Fallback Parent Name
            ItemStack fallbackItem = new ItemStack(Material.TRAPPED_CHEST);
            ItemMeta fallbackMeta = fallbackItem.getItemMeta();
            if (fallbackMeta != null) {
                fallbackMeta.setDisplayName("§6Fallback Tier");
                String fallbackStr = inst.getFallbackParentName() != null && !inst.getFallbackParentName().isEmpty() ? inst.getFallbackParentName() : "None (Will not spawn)";
                fallbackMeta.setLore(Arrays.asList(
                    "§7Fallback: §f" + fallbackStr,
                    "",
                    "§7If the "+ inst.getSpawnChance() + "% chance fails, what",
                    "§7type of chest should spawn here instead?",
                    "§7If 'None', the chest will physically despawn",
                    "§7for the duration of the cycle until it refreshes.",
                    "",
                    "§eClick to type a fallback Loot Table name in chat"
                ));
                fallbackMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chestcfg-action"), PersistentDataType.STRING, "fallback_tier");
                fallbackMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-id"), PersistentDataType.STRING, chestInstanceId);
                fallbackMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
                fallbackItem.setItemMeta(fallbackMeta);
            }
            inv.setItem(24, fallbackItem);
        }

        // Back button
        ItemStack back = new ItemStack(Material.OAK_DOOR);
        ItemMeta bmkn = back.getItemMeta();
        if (bmkn != null) {
            bmkn.setDisplayName("§cGo Back");
            bmkn.getPersistentDataContainer().set(new NamespacedKey(plugin, "chestcfg-action"), PersistentDataType.STRING, "back");
            bmkn.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            bmkn.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-id"), PersistentDataType.STRING, chestInstanceId);
            if (tierName != null) {
                bmkn.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING, tierName);
            }
            back.setItemMeta(bmkn);
        }
        inv.setItem(40, back);

        player.openInventory(inv);
    }
}
