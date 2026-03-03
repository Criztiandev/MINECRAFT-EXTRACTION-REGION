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
import java.util.List;

public class RegionChestsGUI {

    private final ExtractionRegionPlugin plugin;
    public static final String TITLE = "§8Region Chests";

    public RegionChestsGUI(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, SavedRegion region, int page) {
        java.util.Map<com.criztiandev.extractionchest.models.ChestTier, Integer> tierCounts = new java.util.HashMap<>();
        for (com.criztiandev.extractionchest.models.ChestTier tier : com.criztiandev.extractionchest.models.ChestTier.values()) {
            tierCounts.put(tier, 0);
        }

        for (com.criztiandev.extractionchest.models.ChestInstance inst : plugin.getExtractionChestApi().getChestInstanceManager().getAllInstances()) {
            if (inst.getWorld().equals(region.getWorld())) {
                org.bukkit.Location loc = inst.getLocation(org.bukkit.Bukkit.getWorld(inst.getWorld()));
                if (loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX() &&
                    loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ()) {
                    
                    com.criztiandev.extractionchest.models.ParentChestDefinition def = plugin.getExtractionChestApi().getLootTableManager().getDefinition(inst.getParentName());
                    if (def != null) {
                        tierCounts.put(def.getTier(), tierCounts.get(def.getTier()) + 1);
                    }
                }
            }
        }

        Inventory inv = Bukkit.createInventory(null, 27, TITLE + " - " + region.getId());
        
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(" ");
            border.setItemMeta(bm);
        }
        for (int i = 18; i < 27; i++) inv.setItem(i, border);

        int[] slots = {11, 12, 13, 14, 15};
        com.criztiandev.extractionchest.models.ChestTier[] tiers = com.criztiandev.extractionchest.models.ChestTier.values();
        
        for (int i = 0; i < tiers.length && i < slots.length; i++) {
            com.criztiandev.extractionchest.models.ChestTier tier = tiers[i];
            int count = tierCounts.get(tier);
            
            Material mat = switch (tier) {
                case COMMON -> Material.CHEST;
                case UNCOMMON -> Material.TRAPPED_CHEST;
                case RARE -> Material.ENDER_CHEST;
                case EPIC -> Material.BARREL;
                case MYTHIC -> Material.SHULKER_BOX;
                default -> Material.CHEST;
            };
            
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(getTierColor(tier) + "§l" + tier.name() + " CHESTS");
                meta.setLore(Arrays.asList(
                    "§7Total Placed: §f" + count,
                    "",
                    "§eClick to view and manage",
                    "§eall " + tier.name() + " chests."
                ));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-chests-action"), PersistentDataType.STRING, "view_tier");
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING, tier.name());
                item.setItemMeta(meta);
            }
            inv.setItem(slots[i], item);
        }

        // Back button
        ItemStack back = new ItemStack(Material.OAK_DOOR);
        ItemMeta bmkn = back.getItemMeta();
        if (bmkn != null) {
            bmkn.setDisplayName("§cGo Back");
            bmkn.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-chests-action"), PersistentDataType.STRING, "back");
            bmkn.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            back.setItemMeta(bmkn);
        }
        inv.setItem(22, back);

        player.openInventory(inv);
    }
    
    private String getTierColor(com.criztiandev.extractionchest.models.ChestTier tier) {
        return switch (tier) {
            case COMMON -> "§f";
            case UNCOMMON -> "§a";
            case RARE -> "§9";
            case EPIC -> "§5";
            case MYTHIC -> "§6";
            default -> "§f";
        };
    }
}
