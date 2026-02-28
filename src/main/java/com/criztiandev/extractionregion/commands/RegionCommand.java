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

        // Check if it's the specific entry command which doesn't need admin
        boolean isEntryCommand = command.getName().equalsIgnoreCase("regionentry") && args.length > 0 && args[0].equalsIgnoreCase("entry");

        if (!isEntryCommand && !player.hasPermission("extractionchest.admin")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("regionchest")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("create")) {
                if (args.length > 1) {
                    player.chat("/lr create " + args[1] + " chest");
                } else {
                    plugin.getRegionManager().addCreatingPlayer(player.getUniqueId(), com.criztiandev.extractionregion.models.RegionType.CHEST_REPLENISH);
                    player.sendMessage("§aPlease type the name of your new region in chat (or type 'cancel' to abort).");
                }
            } else {
                new com.criztiandev.extractionregion.gui.RegionSubMenuGUI(plugin).openMenu(player, com.criztiandev.extractionregion.models.RegionType.CHEST_REPLENISH);
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("regionentry")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("entry")) {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /lre entry <region>");
                    return true;
                }
                String regionId = args[1];
                SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                if (region == null || region.getType() != com.criztiandev.extractionregion.models.RegionType.ENTRY_REGION) {
                    player.sendMessage("§cValid Entry Region not found.");
                    return true;
                }

                if (region.isOnEntryCooldown(player.getUniqueId())) {
                    long remaining = region.getRemainingEntryCooldownTime(player.getUniqueId());
                    player.sendMessage("§cYou are on cooldown! Please wait " + com.criztiandev.extractionregion.utils.TimeUtil.formatDuration(remaining) + " before entering again.");
                    return true;
                }

                org.bukkit.World world = org.bukkit.Bukkit.getWorld(region.getWorld());
                if (world == null) {
                    player.sendMessage("§cRegion world is not loaded!");
                    return true;
                }

                // Calculate random drop coordinates within the region boundaries
                int minX = Math.min(region.getMinX(), region.getMaxX());
                int maxX = Math.max(region.getMinX(), region.getMaxX());
                int minZ = Math.min(region.getMinZ(), region.getMaxZ());
                int maxZ = Math.max(region.getMinZ(), region.getMaxZ());

                int targetX = minX == maxX ? minX : java.util.concurrent.ThreadLocalRandom.current().nextInt(minX, maxX + 1);
                int targetZ = minZ == maxZ ? minZ : java.util.concurrent.ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);
                int targetY = 300; // Fixed high coordinate

                org.bukkit.Location targetLoc = new org.bukkit.Location(world, targetX + 0.5, targetY, targetZ + 0.5, player.getLocation().getYaw(), 0);
                
                player.teleport(targetLoc);
                player.sendMessage("§cWelcome to the warzone, enjoy");
                region.setPlayerEntryCooldown(player.getUniqueId());

                if (region.getSlowFallingSeconds() > 0) {
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING, region.getSlowFallingSeconds() * 20, 0, false, false, true));
                }
                if (region.getBlindnessSeconds() > 0) {
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, region.getBlindnessSeconds() * 20, 0, false, false, true));
                }
                
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("create")) {
                if (args.length > 1) {
                    player.chat("/lr create " + args[1] + " entry");
                } else {
                    plugin.getRegionManager().addCreatingPlayer(player.getUniqueId(), com.criztiandev.extractionregion.models.RegionType.ENTRY_REGION);
                    player.sendMessage("§aPlease type the name of your new region in chat (or type 'cancel' to abort).");
                }
            } else {
                new com.criztiandev.extractionregion.gui.RegionSubMenuGUI(plugin).openMenu(player, com.criztiandev.extractionregion.models.RegionType.ENTRY_REGION);
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("regionexit")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("spawn")) {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /lrex spawn <region>");
                    return true;
                }
                String regionId = args[1];
                SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                if (region == null) {
                    player.sendMessage("§cRegion not found.");
                    return true;
                }
                
                org.bukkit.Location loc = player.getLocation();
                region.setExtractionSpawnLocation(loc);
                plugin.getRegionManager().saveRegion(region);

                player.sendMessage("§aExtraction spawn point for region §e" + regionId + " §aset to your current location!");
                return true;
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("create")) {
                if (args.length > 1) {
                    player.chat("/lr create " + args[1] + " extraction");
                } else {
                    plugin.getRegionManager().addCreatingPlayer(player.getUniqueId(), com.criztiandev.extractionregion.models.RegionType.EXTRACTION);
                    player.sendMessage("§aPlease type the name of your new region in chat (or type 'cancel' to abort).");
                }
            } else {
                new com.criztiandev.extractionregion.gui.RegionSubMenuGUI(plugin).openMenu(player, com.criztiandev.extractionregion.models.RegionType.EXTRACTION);
            }
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
            player.sendMessage("§7/lr spawn <id> §f- Set extraction spawn for a region.");
            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /lr spawn <region>");
                return true;
            }
            String regionId = args[1];
            SavedRegion region = plugin.getRegionManager().getRegion(regionId);
            if (region == null) {
                player.sendMessage("§cRegion not found.");
                return true;
            }
            
            org.bukkit.Location loc = player.getLocation();
            region.setExtractionSpawnLocation(loc);
            plugin.getRegionManager().saveRegion(region);

            player.sendMessage("§aExtraction spawn point for region §e" + regionId + " §aset to your current location!");
            return true;
        }

        if (args[0].equalsIgnoreCase("wand")) {
            new com.criztiandev.extractionregion.gui.WandMenuGUI(plugin).openMenu(player);
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

        if (args[0].equalsIgnoreCase("profile")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /lr profile <region>");
                return true;
            }
            String id = args[1];
            SavedRegion region = plugin.getRegionManager().getRegion(id);
            if (region == null) {
                player.sendMessage("§cRegion not found.");
                return true;
            }
            
            player.sendMessage("§8[§bExtractionChest§8] §aRegion Profile: §e" + region.getId());
            player.sendMessage("§7Type: §f" + region.getType().name());
            player.sendMessage("§7World: §f" + region.getWorld());
            player.sendMessage("§7Coords: §fX: " + region.getMinX() + " to " + region.getMaxX() + ", Z: " + region.getMinZ() + " to " + region.getMaxZ());
            
            if (region.getType() == com.criztiandev.extractionregion.models.RegionType.CHEST_REPLENISH) {
                long nextReset = region.getNextResetTime();
                if (nextReset > 0) {
                    long remaining = nextReset - System.currentTimeMillis();
                    if (remaining > 0) {
                        player.sendMessage("§7Next Reset: §f" + com.criztiandev.extractionregion.utils.TimeUtil.formatDuration(remaining));
                    } else {
                        player.sendMessage("§7Next Reset: §cPending/Processing...");
                    }
                } else {
                    player.sendMessage("§7Next Reset: §fConfigured Every " + region.getResetIntervalMinutes() + "m");
                }
            } else if (region.getType() == com.criztiandev.extractionregion.models.RegionType.EXTRACTION) {
                java.util.List<Integer> seq = region.getCooldownSequence();
                StringBuilder seqStr = new StringBuilder();
                for (int i = 0; i < seq.size(); i++) {
                    seqStr.append(seq.get(i));
                    if (i < seq.size() - 1) seqStr.append(",");
                }
                player.sendMessage("§7Cooldown Sequence: §f" + seqStr.toString() + "m, Capacity: §f" + region.getMinCapacity() + "-" + region.getMaxCapacity());
            } else if (region.getType() == com.criztiandev.extractionregion.models.RegionType.ENTRY_REGION) {
                player.sendMessage("§7SlowFalling: §f" + region.getSlowFallingSeconds() + "s, Blindness: §f" + region.getBlindnessSeconds() + "s");
            }
            return true;
        }

        player.sendMessage("§cUnknown subcommand. Type /lr help for commands.");
        return true;
    }
}
