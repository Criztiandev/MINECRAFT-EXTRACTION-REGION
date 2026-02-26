package com.criztiandev.extractionregion.gui;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

public class RegionChestDropGUI {

    public static final String TITLE_PREFIX = "§8▶ §dDrop Chests: §7";
    private final ExtractionRegionPlugin plugin;

    public RegionChestDropGUI(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, SavedRegion region) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX + region.getId());

        // Pre-fill the inventory with chests if they already have some configured in autoSpawns
        for (Map.Entry<String, Integer> entry : region.getAutoSpawns().entrySet()) {
            String defName = entry.getKey();
            int amount = entry.getValue();

            // Find the definition to get the proper item icon
            com.criztiandev.extractionchest.models.ParentChestDefinition def = plugin.getExtractionChestApi().getLootTableManager().getDefinition(defName);
            if (def == null) continue;

            // Generate the extraction chest item directly
            ItemStack chestItem = new ItemStack(org.bukkit.Material.CHEST);
            ItemMeta meta = chestItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6" + def.getName() + " §7(Placeable Chest)");
                meta.setLore(java.util.Arrays.asList(
                    "§7Tier: §f" + def.getTier().name(),
                    "",
                    "§ePlace this block to spawn",
                    "§ean Extraction Chest."
                ));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin.getExtractionChestApi(), "extractionchest-type"), PersistentDataType.STRING, def.getName());
                // Set the tier key as expected by the InventoryCloseEvent scanner
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin.getExtractionChestApi(), "extraction-chest-tier"), PersistentDataType.STRING, def.getName());
                chestItem.setItemMeta(meta);
            }

            // Give it the proper amount
            while (amount > 0) {
                ItemStack stack = chestItem.clone();
                int stackSize = Math.min(amount, stack.getMaxStackSize());
                stack.setAmount(stackSize);
                inv.addItem(stack);
                amount -= stackSize;
            }
        }

        player.openInventory(inv);
    }
}
