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
        List<com.criztiandev.extractionchest.models.ChestInstance> allInstances = new java.util.ArrayList<>();
        
        for (com.criztiandev.extractionchest.models.ChestInstance inst : plugin.getExtractionChestApi().getChestInstanceManager().getAllInstances()) {
            if (inst.getWorld().equals(region.getWorld())) {
                org.bukkit.Location loc = inst.getLocation(org.bukkit.Bukkit.getWorld(inst.getWorld()));
                if (loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX() &&
                    loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ()) {
                    allInstances.add(inst);
                }
            }
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE + " - " + region.getId());
        
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
            
            ItemStack item = new ItemStack(Material.CHEST);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6Chest at " + inst.getX() + ", " + inst.getY() + ", " + inst.getZ());
                meta.setLore(Arrays.asList(
                    "§7Type: §f" + inst.getParentName(),
                    "§7World: §f" + inst.getWorld(),
                    "",
                    "§eClick to configure this",
                    "§etype of chest."
                ));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "chest-parent"), PersistentDataType.STRING, inst.getParentName());
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-chests-action"), PersistentDataType.STRING, "edit_chest");
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
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
        inv.setItem(49, back);

        // Pagination buttons
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            if (pm != null) {
                pm.setDisplayName("§cPrevious Page");
                pm.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-chests-action"), PersistentDataType.STRING, "prev_page");
                pm.getPersistentDataContainer().set(new NamespacedKey(plugin, "pagination-page"), PersistentDataType.INTEGER, page - 1);
                pm.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
                prev.setItemMeta(pm);
            }
            inv.setItem(45, prev);
        }

        if (endIndex < allInstances.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            if (nm != null) {
                nm.setDisplayName("§aNext Page");
                nm.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-chests-action"), PersistentDataType.STRING, "next_page");
                nm.getPersistentDataContainer().set(new NamespacedKey(plugin, "pagination-page"), PersistentDataType.INTEGER, page + 1);
                nm.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
                next.setItemMeta(nm);
            }
            inv.setItem(53, next);
        }

        player.openInventory(inv);
    }
}
