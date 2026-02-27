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

        // Clean minimalist centered layout for 27 slots:
        // Slot 11: Overview
        // Slot 12: Get Wand
        // Slot 13: Force Replenish Now (Center)
        // Slot 14: Timer Config
        // Slot 15: Delete Region
        
        // Slot 18: Back Button (Bottom left)
        
        // 1. Region Overview Button (Slot 11)
        ItemStack overviewItem = new ItemStack(Material.BOOK);
        ItemMeta overviewMeta = overviewItem.getItemMeta();
        if (overviewMeta != null) {
            overviewMeta.setDisplayName("§b§lRegion Overview");
            
            // Calculate captured chests dynamically
            int totalChests = 0;
            java.util.Map<String, Integer> tierCounts = new java.util.HashMap<>();
            for (com.criztiandev.extractionchest.models.ChestInstance inst : plugin.getExtractionChestApi().getChestInstanceManager().getAllInstances()) {
                if (inst.getWorld().equals(region.getWorld())) {
                    org.bukkit.Location loc = inst.getLocation(org.bukkit.Bukkit.getWorld(inst.getWorld()));
                    if (loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX() &&
                        loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ()) {
                        totalChests++;
                        tierCounts.put(inst.getParentName(), tierCounts.getOrDefault(inst.getParentName(), 0) + 1);
                    }
                }
            }
            
            java.util.List<String> lore = new java.util.ArrayList<>(Arrays.asList(
                "§7This region contains manually",
                "§7placed Extraction Chests.",
                "",
                "§e§lCaptured Chests: §f" + totalChests
            ));
            
            for (java.util.Map.Entry<String, Integer> entry : tierCounts.entrySet()) {
                lore.add("§8- §7" + entry.getKey() + ": §f" + entry.getValue());
            }
            
            overviewMeta.setLore(lore);
            overviewItem.setItemMeta(overviewMeta);
        }
        inv.setItem(11, overviewItem);
        
        // 2. Get Region Wand Button (Slot 12)
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
        inv.setItem(12, getWandItem);

        // 3. Force Replenish Button (Slot 13 - Center)
        ItemStack forceItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta forceMeta = forceItem.getItemMeta();
        if (forceMeta != null) {
            forceMeta.setDisplayName("§a§lForce Replenish Now");
            forceMeta.setLore(Arrays.asList(
                "§7Click to immediately refill",
                "§7and reset all chests",
                "§7captured inside this region."
            ));
            forceMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-action"), PersistentDataType.STRING, "force");
            forceMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            forceItem.setItemMeta(forceMeta);
        }
        inv.setItem(13, forceItem);

        // 4. Respawn Timer Button (Slot 14)
        ItemStack timerItem = new ItemStack(Material.CLOCK);
        ItemMeta timerMeta = timerItem.getItemMeta();
        if (timerMeta != null) {
            timerMeta.setDisplayName("§e§lTimer Config");
            int hours = region.getResetIntervalMinutes() / 60;
            String timeDisplay = (hours > 0) ? hours + " Hours" : region.getResetIntervalMinutes() + " Minutes";
            timerMeta.setLore(Arrays.asList(
                "§7Cron Schedule: Every §f" + timeDisplay,
                "§7(Syncs relative to 12 AM PH Time)",
                "",
                "§eClick to cycle the timer",
                "§ethat auto-replenishes chests",
                "§einside this region."
            ));
            timerMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-action"), PersistentDataType.STRING, "timer");
            timerMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            timerItem.setItemMeta(timerMeta);
        }
        inv.setItem(14, timerItem);

        // 5. Delete Region Button (Slot 15)
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
        inv.setItem(15, deleteItem);
        
        // Back Button (Slot 18)
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

