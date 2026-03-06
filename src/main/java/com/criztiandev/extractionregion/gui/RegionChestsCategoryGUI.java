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

public class RegionChestsCategoryGUI {

    private final ExtractionRegionPlugin plugin;
    public static final String TITLE = "§8Region Chests - "; // Will append tier

    public RegionChestsCategoryGUI(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, SavedRegion region, com.criztiandev.extractionchest.models.ChestTier tier, int page) {
        List<com.criztiandev.extractionchest.models.ChestInstance> allInstances = new java.util.ArrayList<>();
        
        for (com.criztiandev.extractionchest.models.ChestInstance inst : plugin.getExtractionChestApi().getChestInstanceManager().getAllInstances()) {
            if (inst.getWorld().equals(region.getWorld())) {
                org.bukkit.Location loc = inst.getLocation(org.bukkit.Bukkit.getWorld(inst.getWorld()));
                if (loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX() &&
                    loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ()) {
                    
                    com.criztiandev.extractionchest.models.ParentChestDefinition def = plugin.getExtractionChestApi().getLootTableManager().getDefinition(inst.getParentName());
                    if (def != null && def.getTier() == tier) {
                        allInstances.add(inst);
                    }
                }
            }
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE + tier.name());
        
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(" ");
            border.setItemMeta(bm);
        }
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        int maxItemsPerPage = 45;
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, allInstances.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            com.criztiandev.extractionchest.models.ChestInstance inst = allInstances.get(i);
            
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
                meta.setDisplayName("§6Chest at " + inst.getX() + ", " + inst.getY() + ", " + inst.getZ());
                meta.setLore(Arrays.asList(
                    "§7Type: §f" + inst.getParentName(),
                    "§7World: §f" + inst.getWorld(),
                    "§7Stationary Toggle: " + (inst.isStationary() ? "§aYes" : "§cNo"),
                    "§7Spawn Chance: §e" + inst.getSpawnChance() + "%",
                    "",
                    "§eLeft-Click §7to open Config",
                    "§eShift-Click §7to teleport to chest"
                ));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-parent"), PersistentDataType.STRING, inst.getParentName());
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-id"), PersistentDataType.STRING, inst.getId());
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-category-action"), PersistentDataType.STRING, "edit_chest");
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING, tier.name());
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        // Back button
        ItemStack back = new ItemStack(Material.OAK_DOOR);
        ItemMeta bmkn = back.getItemMeta();
        if (bmkn != null) {
            bmkn.setDisplayName("§cGo Back");
            bmkn.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-category-action"), PersistentDataType.STRING, "back");
            bmkn.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            back.setItemMeta(bmkn);
        }
        inv.setItem(49, back);

        // Bulk Configure button
        ItemStack bulk = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta bulkMeta = bulk.getItemMeta();
        if (bulkMeta != null) {
            bulkMeta.setDisplayName("§b§lBulk Configure All §7(" + allInstances.size() + " chests)");
            bulkMeta.setLore(Arrays.asList(
                "§7Apply settings to ALL §f" + tier.name() + " §7chests",
                "§7in this region at once.",
                "",
                "§eLeft-Click §7to set spawn chance for all",
                "§eRight-Click §7to toggle stationary for all",
                "§eShift-Right-Click §7to set fallback for all"
            ));
            bulkMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-category-action"), PersistentDataType.STRING, "bulk_configure_tier");
            bulkMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            bulkMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING, tier.name());
            bulk.setItemMeta(bulkMeta);
        }
        inv.setItem(47, bulk);

        // Pagination buttons
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            if (pm != null) {
                pm.setDisplayName("§cPrevious Page");
                pm.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-category-action"), PersistentDataType.STRING, "prev_page");
                pm.getPersistentDataContainer().set(new NamespacedKey(plugin, "pagination-page"), PersistentDataType.INTEGER, page - 1);
                pm.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
                pm.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING, tier.name());
                prev.setItemMeta(pm);
            }
            inv.setItem(45, prev);
        }

        if (endIndex < allInstances.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            if (nm != null) {
                nm.setDisplayName("§aNext Page");
                nm.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-category-action"), PersistentDataType.STRING, "next_page");
                nm.getPersistentDataContainer().set(new NamespacedKey(plugin, "pagination-page"), PersistentDataType.INTEGER, page + 1);
                nm.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
                nm.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-tier"), PersistentDataType.STRING, tier.name());
                next.setItemMeta(nm);
            }
            inv.setItem(53, next);
        }

        player.openInventory(inv);
    }
}
