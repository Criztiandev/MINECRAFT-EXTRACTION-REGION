package com.criztiandev.extractionregion.commands;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.gui.RegionActionGUI;
import com.criztiandev.extractionregion.gui.RegionListGUI;
import com.criztiandev.extractionregion.gui.RegionMainGUI;
import com.criztiandev.extractionregion.models.RegionSelection;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class RegionCommand implements CommandExecutor {

    private final ExtractionRegionPlugin plugin;

    public RegionCommand(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("extractionchest.admin")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            new RegionMainGUI(plugin).openMenu(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage("§eExtractionRegionEditor Commands:");
            player.sendMessage("§7/lr wand §f- Get the region selection wand.");
            player.sendMessage("§7/lr create <id> [chest/extraction/entry] §f- Create a region.");
            player.sendMessage("§7/lr delete <id> §f- Delete an existing region.");
            player.sendMessage("§7/lr rename <old_id> <new_id> §f- Rename a region.");
            player.sendMessage("§7/lr list §f- Open the region management GUI.");
            player.sendMessage("§7/lr replenish <id> §f- Force replenish chests in a region.");
            return true;
        }

        if (args[0].equalsIgnoreCase("wand")) {
            ItemStack wand = new ItemStack(Material.STICK);
            ItemMeta meta = wand.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§dRegion Selection Wand");
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "region-wand"), PersistentDataType.BYTE, (byte) 1);
                wand.setItemMeta(meta);
            }
            player.getInventory().addItem(wand);
            player.sendMessage("§aYou have received the Region Selection Wand.");
            player.sendMessage("§7Left-Click a block to set Pos1. Right-Click a block to set Pos2.");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /lr create <id> [chest/extraction/entry]");
                return true;
            }

            String id = args[1];
            if (plugin.getRegionManager().getRegion(id) != null) {
                player.sendMessage("§cA region with that ID already exists.");
                return true;
            }

            com.criztiandev.extractionregion.models.RegionType type = com.criztiandev.extractionregion.models.RegionType.CHEST_REPLENISH;
            if (args.length >= 3) {
                if (args[2].equalsIgnoreCase("extraction")) {
                    type = com.criztiandev.extractionregion.models.RegionType.EXTRACTION;
                } else if (args[2].equalsIgnoreCase("entry")) {
                    type = com.criztiandev.extractionregion.models.RegionType.ENTRY_REGION;
                } else if (!args[2].equalsIgnoreCase("chest")) {
                    player.sendMessage("§cInvalid region type. Use 'chest', 'extraction', or 'entry'.");
                    return true;
                }
            }

            RegionSelection sel = plugin.getRegionManager().getOrCreateSelection(player.getUniqueId());
            if (sel.getPos1() == null || sel.getPos2() == null) {
                player.sendMessage("§cYou must select a region first using the wand.");
                return true;
            }

            if (!sel.getPos1().getWorld().equals(sel.getPos2().getWorld())) {
                player.sendMessage("§cSelections must be in the same world.");
                return true;
            }

            int minX = Math.min(sel.getPos1().getBlockX(), sel.getPos2().getBlockX());
            int maxX = Math.max(sel.getPos1().getBlockX(), sel.getPos2().getBlockX());
            int minZ = Math.min(sel.getPos1().getBlockZ(), sel.getPos2().getBlockZ());
            int maxZ = Math.max(sel.getPos1().getBlockZ(), sel.getPos2().getBlockZ());
            String world = sel.getPos1().getWorld().getName();

            SavedRegion region = new SavedRegion(id, world, minX, maxX, minZ, maxZ);
            region.setType(type);
            plugin.getRegionManager().saveRegion(region);

            player.sendMessage("§aRegion §e" + id + " §a(" + type.name() + ") created successfully.");
            plugin.getRegionManager().removeSelection(player.getUniqueId());
            new RegionActionGUI(plugin).openMenu(player, region);
            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /lr delete <id>");
                return true;
            }

            String id = args[1];
            if (plugin.getRegionManager().getRegion(id) == null) {
                player.sendMessage("§cRegion not found.");
                return true;
            }

            plugin.getRegionManager().deleteRegion(id);
            player.sendMessage("§aRegion §e" + id + " §adeleted.");
            return true;
        }

        if (args[0].equalsIgnoreCase("rename")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /lr rename <old_id> <new_id>");
                return true;
            }

            String oldId = args[1];
            String newId = args[2];

            if (plugin.getRegionManager().getRegion(oldId) == null) {
                player.sendMessage("§cRegion '" + oldId + "' not found.");
                return true;
            }

            if (plugin.getRegionManager().getRegion(newId) != null) {
                player.sendMessage("§cRegion '" + newId + "' already exists. Choose a different name.");
                return true;
            }

            player.sendMessage("§7Renaming region... Please wait.");
            plugin.getRegionManager().renameRegion(oldId, newId).thenAccept(success -> {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> { // Sync back to main thread
                    if (success) {
                        player.sendMessage("§aSuccessfully renamed region §e" + oldId + " §ato §e" + newId + "§a.");
                    } else {
                        player.sendMessage("§cFailed to rename region. Check the server console for errors.");
                    }
                });
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            new RegionListGUI(plugin).openMenu(player, 0, null);
            return true;
        }

        if (args[0].equalsIgnoreCase("replenish")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /lr replenish <id>");
                return true;
            }

            String id = args[1];
            SavedRegion region = plugin.getRegionManager().getRegion(id);
            if (region == null) {
                player.sendMessage("§cRegion not found.");
                return true;
            }

            int replenished = plugin.getRegionManager().forceReplenish(region);
            player.sendMessage("§aForced replenish for region §e" + id + "§a. Replenished §e" + replenished + " §achests.");
            return true;
        }

        player.sendMessage("§cUnknown subcommand. Type /lr help for commands.");
        return true;
    }
}
