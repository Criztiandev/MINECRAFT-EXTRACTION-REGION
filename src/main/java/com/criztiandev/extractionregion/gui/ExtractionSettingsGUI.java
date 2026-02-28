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

public class ExtractionSettingsGUI {

    private final ExtractionRegionPlugin plugin;
    public static final String TITLE = "§8Extraction Settings";

    public ExtractionSettingsGUI(ExtractionRegionPlugin plugin) {
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

        // Slot 11: Cooldown
        ItemStack cooldownItem = new ItemStack(Material.CLOCK);
        ItemMeta cooldownMeta = cooldownItem.getItemMeta();
        if (cooldownMeta != null) {
            cooldownMeta.setDisplayName("§e§lCooldown Sequence");
            
            java.util.List<Integer> seq = region.getCooldownSequence();
            StringBuilder seqBuilder = new StringBuilder();
            for (int i = 0; i < seq.size(); i++) {
                seqBuilder.append(seq.get(i));
                if (i < seq.size() - 1) seqBuilder.append(",");
            }
            
            cooldownMeta.setLore(Arrays.asList(
                "§7Current: §f" + seqBuilder.toString() + " Minutes",
                "§7(Progresses through list on each pop)",
                "",
                "§eLeft-Click: §fCycle Preset Sequences",
                "§cRight-Click: §fCycle Preset Sequences",
                "§dShift-Click: §fSet exact sequence in chat"
            ));
            cooldownMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING, "cooldown");
            cooldownMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            cooldownItem.setItemMeta(cooldownMeta);
        }
        inv.setItem(11, cooldownItem);

        // Slot 12: Min Capacity
        ItemStack minCapItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta minCapMeta = minCapItem.getItemMeta();
        if (minCapMeta != null) {
            minCapMeta.setDisplayName("§b§lMinimum Capacity");
            minCapMeta.setLore(Arrays.asList(
                "§7Current: §f" + region.getMinCapacity() + " Players",
                "",
                "§eLeft-Click: §f+1",
                "§cRight-Click: §f-1",
                "§dShift-Click: §fSet exact amount in chat"
            ));
            minCapMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING, "min_cap");
            minCapMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            minCapItem.setItemMeta(minCapMeta);
        }
        inv.setItem(12, minCapItem);

        // Slot 13: Max Capacity
        ItemStack maxCapItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta maxCapMeta = maxCapItem.getItemMeta();
        if (maxCapMeta != null) {
            maxCapMeta.setDisplayName("§b§lMaximum Capacity");
            maxCapMeta.setLore(Arrays.asList(
                "§7Current: §f" + region.getMaxCapacity() + " Players",
                "",
                "§eLeft-Click: §f+1",
                "§cRight-Click: §f-1",
                "§dShift-Click: §fSet exact amount in chat"
            ));
            maxCapMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING, "max_cap");
            maxCapMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            maxCapItem.setItemMeta(maxCapMeta);
        }
        inv.setItem(13, maxCapItem);
        
        // Slot 14: Extraction Duration
        ItemStack durationItem = new ItemStack(Material.COMPASS);
        ItemMeta durationMeta = durationItem.getItemMeta();
        if (durationMeta != null) {
            durationMeta.setDisplayName("§6§lExtraction Duration(s)");
            
            java.util.List<Integer> dSeq = region.getPossibleDurations();
            StringBuilder dSeqBuilder = new StringBuilder();
            for (int i = 0; i < dSeq.size(); i++) {
                dSeqBuilder.append(dSeq.get(i));
                if (i < dSeq.size() - 1) dSeqBuilder.append(",");
            }
            
            durationMeta.setLore(Arrays.asList(
                "§7Time required to wait in zone.",
                "§7Current: §f" + dSeqBuilder.toString() + " Seconds",
                "§7(Picks a random duration from the list)",
                "",
                "§eLeft-Click: §fCycle Presets",
                "§cRight-Click: §fCycle Presets",
                "§dShift-Click: §fSet exact sequence in chat"
            ));
            durationMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING, "duration");
            durationMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            durationItem.setItemMeta(durationMeta);
        }
        inv.setItem(14, durationItem);

        // Slot 15: Mimic Toggle
        boolean mimics = region.isMimicEnabled();
        ItemStack mimicItem = new ItemStack(mimics ? Material.REDSTONE_TORCH : Material.LEVER);
        ItemMeta mimicMeta = mimicItem.getItemMeta();
        if (mimicMeta != null) {
            mimicMeta.setDisplayName("§c§lMimic Conduit Trap");
            mimicMeta.setLore(Arrays.asList(
                "§7Enabled: " + (mimics ? "§aYes" : "§cNo"),
                "§7Chance: §f" + region.getMimicChance() + "%",
                "§7If triggered, a trap guards the",
                "§7extraction! (Detonates + Guards)",
                "",
                "§eLeft-Click: §fToggle On/Off",
                "§cRight-Click: §fChange Chance (+5%)",
                "§dShift-Click: §fSet exact chance in chat"
            ));
            mimicMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING, "mimic");
            mimicMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            mimicItem.setItemMeta(mimicMeta);
        }
        inv.setItem(15, mimicItem);

        // Slot 16: Announcement Radius
        ItemStack radiusItem = new ItemStack(Material.BELL);
        ItemMeta radiusMeta = radiusItem.getItemMeta();
        if (radiusMeta != null) {
            radiusMeta.setDisplayName("§a§lAnnouncement Radius");
            int r = region.getAnnouncementRadius();
            radiusMeta.setLore(Arrays.asList(
                "§7Current: §f" + (r <= 0 ? "Infinite/Global" : r + " Blocks"),
                "§7Radius for action bars & chat alerts",
                "§7when extraction starts/finishes.",
                "",
                "§eLeft-Click: §f+10",
                "§cRight-Click: §f-10",
                "§dShift-Click: §fSet exact radius in chat"
            ));
            radiusMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING, "radius");
            radiusMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            radiusItem.setItemMeta(radiusMeta);
        }
        inv.setItem(16, radiusItem);

        // Slot 24: Beam Color
        ItemStack beamItem = new ItemStack(Material.RED_DYE);
        ItemMeta beamMeta = beamItem.getItemMeta();
        if (beamMeta != null) {
            beamMeta.setDisplayName("§4§lBeam Color");
            beamMeta.setLore(Arrays.asList(
                "§7Current: §f" + region.getBeamColor(),
                "§7The hex color of the extraction beam.",
                "",
                "§eLeft-Click: §fSet color in chat"
            ));
            beamMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING, "beam");
            beamMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            beamItem.setItemMeta(beamMeta);
        }
        inv.setItem(24, beamItem);

        // Slot 25: Alarm Sound
        ItemStack alarmItem = new ItemStack(Material.NOTE_BLOCK);
        ItemMeta alarmMeta = alarmItem.getItemMeta();
        if (alarmMeta != null) {
            alarmMeta.setDisplayName("§9§lAlarm Sound");
            alarmMeta.setLore(Arrays.asList(
                "§7Current: §f" + region.getAlarmSound(),
                "§7The sound played during countdown.",
                "",
                "§eLeft-Click: §fSet sound in chat"
            ));
            alarmMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING, "alarm");
            alarmMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            alarmItem.setItemMeta(alarmMeta);
        }
        inv.setItem(25, alarmItem);

        // Back Button (Slot 18)
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cGo Back to Actions");
            backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "extrc-action"), PersistentDataType.STRING, "back");
            backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING, region.getId());
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(18, backItem);

        player.openInventory(inv);
    }
}
