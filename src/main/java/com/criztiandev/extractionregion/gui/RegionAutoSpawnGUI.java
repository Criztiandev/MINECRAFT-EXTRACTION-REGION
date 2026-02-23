package com.criztiandev.extractionregion.gui;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionchest.models.ParentChestDefinition;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class RegionAutoSpawnGUI {

    private final ExtractionRegionPlugin plugin;

    public RegionAutoSpawnGUI(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, SavedRegion region) {
        FileConfiguration config = plugin.getConfig();
        String title = config.getString("region-gui.title", "§8â–¶ §dConfigure Auto Spawns").replace("&", "§");
        int size = config.getInt("region-gui.size", 54);
        Inventory inv = Bukkit.createInventory(null, size, title);

        Material fillerMat = Material.matchMaterial(config.getString("region-gui.filler-material", "BLACK_STAINED_GLASS_PANE"));
        if (fillerMat == null) fillerMat = Material.BLACK_STAINED_GLASS_PANE;
        
        ItemStack border = createItem(fillerMat, "§8");
        for (int i = 0; i < size; i++) inv.setItem(i, border);

        List<ParentChestDefinition> templates = plugin.getExtractionChestApi().getLootTableManager().getAllDefinitions();
        int slot = 10;
        for (ParentChestDefinition temp : templates) {
            if (slot > size - 10 && slot % 9 == 8) {
                continue;
            }
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
                continue;
            }

            int amount = region.getAutoSpawnAmount(temp.getName());
            Material mat = Material.matchMaterial(config.getString("tiers." + temp.getTier().name().toLowerCase() + ".nexo-block-id", "CHEST"));
            if (mat == null) mat = Material.CHEST; // Fallback if nexo block IDs are not real materials

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6" + temp.getName() + " §8[" + temp.getTier().name() + "]");
                
                List<String> rawLore = config.getStringList("region-gui.item-lore");
                List<String> lore = new ArrayList<>();
                if (!rawLore.isEmpty()) {
                    for (String l : rawLore) {
                        lore.add(l.replace("&", "§").replace("%amount%", String.valueOf(amount)));
                    }
                } else {
                    lore.add("§7Current Spawns: §f" + amount);
                    lore.add("§aLeft-Click to increase (+1)");
                    lore.add("§cRight-Click to decrease (-1)");
                }
                meta.setLore(lore);
                
                // Save meta data to know which definition this represents and which region
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-gui-type"), PersistentDataType.STRING, temp.getName());
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-gui-region"), PersistentDataType.STRING, region.getId());

                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}

