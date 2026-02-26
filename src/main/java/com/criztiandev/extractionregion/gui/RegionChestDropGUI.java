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

            // Generate the extraction chest item
            ItemStack chestItem = plugin.getExtractionChestApi().getItemManager().getExtractionChestItem(def);
            if (chestItem == null) continue;

            // Give it the proper amount (max stack size visually limits this to 64 per slot, 
            // but for a 54 slot inventory we can just spread them or set the stack amount)
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
