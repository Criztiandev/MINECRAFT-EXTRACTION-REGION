package com.criztiandev.extractionregion.listeners;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class RegionChatPromptListener implements Listener {

    private final ExtractionRegionPlugin plugin;

    public RegionChatPromptListener(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (plugin.getRegionManager().isCreatingPlayer(player.getUniqueId())) {
            event.setCancelled(true);
            String input = event.getMessage().trim();

            if (input.equalsIgnoreCase("cancel")) {
                plugin.getRegionManager().removeCreatingPlayer(player.getUniqueId());
                player.sendMessage("§cRegion creation cancelled.");
                return;
            }

            if (input.contains(" ")) {
                player.sendMessage("§cRegion IDs cannot contain spaces. Try again, or type 'cancel'.");
                return;
            }

            // Retrieve intended type
            com.criztiandev.extractionregion.models.RegionType type = plugin.getRegionManager().getCreatingPlayerType(player.getUniqueId());
            String typeStr = "chest";
            if (type == com.criztiandev.extractionregion.models.RegionType.EXTRACTION) typeStr = "extraction";
            else if (type == com.criztiandev.extractionregion.models.RegionType.ENTRY_REGION) typeStr = "entry";

            // Execute creation command sync
            String finalTypeStr = typeStr;
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getRegionManager().removeCreatingPlayer(player.getUniqueId());
                player.chat("/lr create " + input + " " + finalTypeStr);
            });
            return;
        }

        if (plugin.getRegionManager().isTimerConfiguringPlayer(player.getUniqueId())) {
            event.setCancelled(true);
            String input = event.getMessage().trim();

            if (input.equalsIgnoreCase("cancel")) {
                plugin.getRegionManager().removeTimerConfiguringPlayer(player.getUniqueId());
                player.sendMessage("§cTimer configuration cancelled.");
                return;
            }

            try {
                int minutes = Integer.parseInt(input);
                if (minutes <= 0 || minutes > 1440) {
                    player.sendMessage("§cPlease enter a number between 1 and 1440 minutes (24 hours).");
                    return;
                }

                String regionId = plugin.getRegionManager().getTimerConfiguringRegionId(player.getUniqueId());
                plugin.getRegionManager().removeTimerConfiguringPlayer(player.getUniqueId());

                com.criztiandev.extractionregion.models.SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                if (region != null) {
                    region.setResetIntervalMinutes(minutes);
                    plugin.getRegionManager().saveRegion(region);
                    player.sendMessage("§aTimer interval set to " + minutes + " minutes.");
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        new com.criztiandev.extractionregion.gui.RegionActionGUI(plugin).openMenu(player, region);
                    });
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number. Please enter a valid integer (e.g., 60), or type 'cancel'.");
            }
        }
        
        if (plugin.getRegionManager().hasPromptState(player.getUniqueId())) {
            event.setCancelled(true);
            String input = event.getMessage().trim();
            String state = plugin.getRegionManager().getPromptState(player.getUniqueId());

            if (input.equalsIgnoreCase("cancel")) {
                plugin.getRegionManager().removePromptState(player.getUniqueId());
                player.sendMessage("§cInput cancelled.");
                return;
            }

            try {
                String[] parts = state.split("_", 3);
                if (parts.length < 3) return; // Malformed state

                String prefix = "";
                String rId = "";
                
                if (state.startsWith("chest_chance_") || state.startsWith("chest_fallback_")) {
                    plugin.getRegionManager().removePromptState(player.getUniqueId());
                    String[] chestParts = state.split("_", 4);
                    if (chestParts.length < 4) return;
                    String chestRegionId = chestParts[2];
                    String chestInstanceId = chestParts[3];
                    
                    com.criztiandev.extractionregion.models.SavedRegion cRegion = plugin.getRegionManager().getRegion(chestRegionId);
                    com.criztiandev.extractionchest.models.ChestInstance cInst = plugin.getExtractionChestApi().getChestInstanceManager().getInstanceById(chestInstanceId);
                    if (cRegion == null || cInst == null) {
                        player.sendMessage("§cRegion or Chest Instance no longer exists.");
                        return;
                    }

                    if (state.startsWith("chest_chance_")) {
                        try {
                            int chance = Integer.parseInt(input);
                            if (chance < 1 || chance > 100) {
                                player.sendMessage("§cSpawn chance must be between 1 and 100.");
                                return;
                            }
                            cInst.setSpawnChance(chance);
                            player.sendMessage("§aSpawn chance set to " + chance + "%.");
                        } catch (NumberFormatException e) {
                            player.sendMessage("§cInvalid number. Please enter a valid percentage (1-100).");
                            return;
                        }
                    } else if (state.startsWith("chest_fallback_")) {
                        if (input.equalsIgnoreCase("none")) {
                            cInst.setFallbackParentName(null);
                            player.sendMessage("§aFallback tier removed. Chest will physically despawn if the chance roll fails.");
                        } else {
                            com.criztiandev.extractionchest.models.ParentChestDefinition def = plugin.getExtractionChestApi().getLootTableManager().getDefinition(input);
                            if (def == null) {
                                player.sendMessage("§cWarning: The loot table '" + input + "' doesn't seem to exist. It will be saved anyway, but you should create it soon!");
                            }
                            cInst.setFallbackParentName(input);
                            player.sendMessage("§aFallback tier set to " + input + ".");
                        }
                    }

                    plugin.getExtractionChestApi().getStorageProvider().saveInstance(cInst);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        new com.criztiandev.extractionregion.gui.ChestConfigGUI(plugin).openMenu(player, cRegion, chestInstanceId);
                    });
                    return;
                }

                // Handle bulk chance prompt: "bulk_chance_<regionId>_<tierName>"
                if (state.startsWith("bulk_chance_")) {
                    plugin.getRegionManager().removePromptState(player.getUniqueId());
                    String suffix = state.substring("bulk_chance_".length()); // "<regionId>_<tierName>"
                    int lastUnderscore = suffix.lastIndexOf("_");
                    if (lastUnderscore < 0) return;
                    String bulkRegionId = suffix.substring(0, lastUnderscore);
                    String bulkTierName = suffix.substring(lastUnderscore + 1);

                    com.criztiandev.extractionregion.models.SavedRegion bRegion = plugin.getRegionManager().getRegion(bulkRegionId);
                    if (bRegion == null) { player.sendMessage("§cRegion no longer exists."); return; }

                    try {
                        int chance = Integer.parseInt(input);
                        if (chance < 1 || chance > 100) {
                            player.sendMessage("§cSpawn chance must be between 1 and 100.");
                            return;
                        }
                        com.criztiandev.extractionchest.models.ChestTier bulkTier = com.criztiandev.extractionchest.models.ChestTier.valueOf(bulkTierName);
                        int updated = 0;
                        for (com.criztiandev.extractionchest.models.ChestInstance bi : plugin.getExtractionChestApi().getChestInstanceManager().getAllInstances()) {
                            if (!bi.getWorld().equals(bRegion.getWorld())) continue;
                            org.bukkit.Location bLoc = bi.getLocation(org.bukkit.Bukkit.getWorld(bi.getWorld()));
                            if (bLoc.getBlockX() < bRegion.getMinX() || bLoc.getBlockX() > bRegion.getMaxX()) continue;
                            if (bLoc.getBlockZ() < bRegion.getMinZ() || bLoc.getBlockZ() > bRegion.getMaxZ()) continue;
                            com.criztiandev.extractionchest.models.ParentChestDefinition bDef = plugin.getExtractionChestApi().getLootTableManager().getDefinition(bi.getParentName());
                            if (bDef == null || bDef.getTier() != bulkTier) continue;
                            bi.setSpawnChance(chance);
                            plugin.getExtractionChestApi().getStorageProvider().saveInstance(bi);
                            updated++;
                        }
                        final com.criztiandev.extractionchest.models.ChestTier finalTier = bulkTier;
                        final com.criztiandev.extractionregion.models.SavedRegion fRegion = bRegion;
                        player.sendMessage("§b[Bulk Configure] §aSet spawn chance to §e" + chance + "% §afor §e" + updated + " §a" + bulkTierName + " chests in §b" + bulkRegionId + "§a.");
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            new com.criztiandev.extractionregion.gui.RegionChestsCategoryGUI(plugin).openMenu(player, fRegion, finalTier, 0);
                        });
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cInvalid number. Please enter a valid percentage (1-100) or type 'cancel'.");
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid tier name. Something went wrong.");
                    }
                    return;
                }
                
                if (state.startsWith("extrc_cooldown_")) { prefix = "extrc_cooldown_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("extrc_min_cap_")) { prefix = "extrc_min_cap_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("extrc_max_cap_")) { prefix = "extrc_max_cap_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("extrc_duration_")) { prefix = "extrc_duration_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("extrc_mimic_")) { prefix = "extrc_mimic_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("extrc_radius_")) { prefix = "extrc_radius_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("extrc_beam_")) { prefix = "extrc_beam_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("extrc_cool_cmd_")) { prefix = "extrc_cool_cmd_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("extrc_alarm_")) { prefix = "extrc_alarm_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("extrc_dest_cmd_")) { prefix = "extrc_dest_cmd_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("extrc_dest_loc_")) { prefix = "extrc_dest_loc_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("holo_offset_x_")) { prefix = "holo_offset_x_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("holo_offset_y_")) { prefix = "holo_offset_y_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("holo_offset_z_")) { prefix = "holo_offset_z_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("holo_scale_")) { prefix = "holo_scale_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("entry_slowfall_")) { prefix = "entry_slowfall_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("entry_blindness_")) { prefix = "entry_blindness_"; rId = state.substring(prefix.length()); }
                else if (state.startsWith("entry_cooldown_")) { prefix = "entry_cooldown_"; rId = state.substring(prefix.length()); }

                plugin.getRegionManager().removePromptState(player.getUniqueId());

                com.criztiandev.extractionregion.models.SavedRegion region = plugin.getRegionManager().getRegion(rId);
                if (region != null) {
                    if (prefix.equals("extrc_cooldown_") || prefix.equals("extrc_duration_")) {
                        java.util.List<Integer> seq = new java.util.ArrayList<>();
                        for (String part : input.split(",")) {
                            try {
                                int val = Integer.parseInt(part.trim());
                                if (val >= 0) seq.add(val);
                            } catch (NumberFormatException ignored) {}
                        }
                        if (seq.isEmpty()) {
                            player.sendMessage("§cInvalid sequence. Please enter valid numbers (e.g., 15,25,30).");
                            return;
                        }
                        
                        if (prefix.equals("extrc_cooldown_")) {
                            region.setCooldownSequence(seq);
                            region.setCooldownIndex(0);
                            player.sendMessage("§aCooldown sequence updated to " + input + ".");
                        } else {
                            region.setPossibleDurations(seq);
                            player.sendMessage("§aExtraction duration sequence updated to " + input + ".");
                        }
                    } else if (prefix.equals("extrc_beam_") || prefix.equals("extrc_alarm_") || prefix.equals("extrc_dest_cmd_") || prefix.equals("extrc_dest_loc_") || prefix.equals("extrc_cool_cmd_") || prefix.equals("entry_fallback_")) {
                        if (prefix.equals("extrc_dest_cmd_")) {
                            region.setExtractionCommand(input);
                            player.sendMessage("§aExtraction command updated to " + input + ".");
                        } else if (prefix.equals("extrc_dest_loc_")) {
                            String[] coords = input.split(",");
                            if (coords.length >= 4) {
                                try {
                                    region.setExtractionSpawnWorld(coords[0].trim());
                                    region.setExtractionSpawnX(Double.parseDouble(coords[1].trim()));
                                    region.setExtractionSpawnY(Double.parseDouble(coords[2].trim()));
                                    region.setExtractionSpawnZ(Double.parseDouble(coords[3].trim()));
                                    region.setExtractionSpawnYaw(0f);
                                    region.setExtractionSpawnPitch(0f);
                                    player.sendMessage("§aExtraction spawn location updated to " + input + ".");
                                    
                                    if (Bukkit.getWorld(coords[0].trim()) == null) {
                                        player.sendMessage("§cWARNING: The world '" + coords[0].trim() + "' does not seem to exist! Teleport will fail.");
                                        player.sendMessage("§e(The default overworld is usually named 'world'.)");
                                    }
                                } catch (NumberFormatException e) {
                                    player.sendMessage("§cInvalid coordinates format. Please use numbers for X,Y,Z.");
                                    return;
                                }
                            } else {
                                player.sendMessage("§cInvalid format. Please use 'world,x,y,z'.");
                                return;
                            }
                        } else if (prefix.equals("extrc_beam_")) {
                            region.setBeamColor(input);
                            player.sendMessage("§aBeam color updated to " + input + ".");
                        } else if (prefix.equals("extrc_cool_cmd_")) {
                            region.setCooldownCommand(input);
                            region.setUseCooldownCommand(true);
                            player.sendMessage("§aCooldown command updated to: " + input);
                        } else if (prefix.equals("entry_fallback_")) {
                            region.setEntryFallbackCommand(input);
                            player.sendMessage("§aFallback command updated to: " + input);
                        } else {
                            try {
                                org.bukkit.Sound.valueOf(input.toUpperCase());
                                region.setAlarmSound(input.toUpperCase());
                                player.sendMessage("§aAlarm sound updated to " + input.toUpperCase() + ".");
                            } catch (Exception e) {
                                player.sendMessage("§cInvalid sound. Please ensure you type a valid Spigot/Paper Sound enum.");
                                return;
                            }
                        }
                    } else if (prefix.startsWith("holo_")) {
                        try {
                            double value = Double.parseDouble(input);
                            if (prefix.equals("holo_scale_") && value <= 0) {
                                player.sendMessage("§cScale must be greater than 0.");
                                return;
                            }
                            
                            if (prefix.equals("holo_offset_x_")) region.setHologramOffsetX(value);
                            else if (prefix.equals("holo_offset_y_")) region.setHologramOffsetY(value);
                            else if (prefix.equals("holo_offset_z_")) region.setHologramOffsetZ(value);
                            else if (prefix.equals("holo_scale_")) region.setHologramScale(value);
                            
                            player.sendMessage("§aHologram setting updated to " + value + ".");
                            
                            plugin.getRegionManager().saveRegion(region);
                            
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                new com.criztiandev.extractionregion.gui.HologramSettingsGUI(plugin).openMenu(player, region);
                            });
                            return; // Early return to avoid ExtractionSettingsGUI
                        } catch (NumberFormatException e) {
                            player.sendMessage("§cInvalid number. Please check your input or type 'cancel'.");
                            return;
                        }
                    } else {
                        try {
                            int value = Integer.parseInt(input);
                            if (value < 0) {
                                player.sendMessage("§cPlease enter a positive number.");
                                return;
                            }
                            
                            if (prefix.equals("extrc_min_cap_")) region.setMinCapacity(value);
                            else if (prefix.equals("extrc_max_cap_")) region.setMaxCapacity(value);
                            else if (prefix.equals("extrc_mimic_")) {
                                if (value > 100) value = 100;
                                region.setMimicChance(value);
                            }
                            else if (prefix.equals("extrc_radius_")) region.setAnnouncementRadius(value);
                            else if (prefix.equals("entry_slowfall_")) region.setSlowFallingSeconds(value);
                            else if (prefix.equals("entry_blindness_")) region.setBlindnessSeconds(value);
                            else if (prefix.equals("entry_cooldown_")) region.setEntryCooldownMinutes(value);
                            
                            player.sendMessage("§aSetting updated to " + value + ".");
                        } catch (NumberFormatException e) {
                            player.sendMessage("§cInvalid input. Please check your numbers or type 'cancel'.");
                            return;
                        }
                    }
                    
                    plugin.getRegionManager().saveRegion(region);
                    
                    final String finalPrefix = prefix;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (finalPrefix.startsWith("entry_")) {
                            new com.criztiandev.extractionregion.gui.EntrySettingsGUI(plugin).openMenu(player, region);
                        } else {
                            new com.criztiandev.extractionregion.gui.ExtractionSettingsGUI(plugin).openMenu(player, region);
                        }
                    });
                }
            } catch (Exception e) {
                player.sendMessage("§cAn error occurred processing your input. type 'cancel' if stuck.");
            }
        }
    }
}
