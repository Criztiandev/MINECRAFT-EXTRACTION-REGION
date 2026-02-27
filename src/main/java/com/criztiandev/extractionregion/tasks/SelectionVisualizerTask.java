package com.criztiandev.extractionregion.tasks;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionSelection;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SelectionVisualizerTask extends BukkitRunnable {

    private final ExtractionRegionPlugin plugin;
    private final Map<UUID, Set<Location>> activeVisuals = new HashMap<>();
    private final BlockData redWool;

    public SelectionVisualizerTask(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
        this.redWool = Bukkit.createBlockData(Material.RED_WOOL);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean shouldShow = false;
            RegionSelection selection = plugin.getRegionManager().getSelection(player.getUniqueId());
            SavedRegion targetRegion = null;
            
            ItemStack item = player.getInventory().getItemInMainHand();
            if (com.criztiandev.extractionregion.utils.WandUtil.isWand(plugin, item)) {
                ItemMeta meta = item.getItemMeta();
                if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING)) {
                    String regionId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "region-id"), PersistentDataType.STRING);
                    targetRegion = plugin.getRegionManager().getRegion(regionId);
                    if (targetRegion != null) {
                        shouldShow = true;
                    }
                } else if (selection != null && (selection.getPos1() != null || selection.getPos2() != null)) {
                    shouldShow = true;
                } else {
                    targetRegion = plugin.getRegionManager().getRegionAt(player.getLocation());
                    if (targetRegion != null) {
                        shouldShow = true;
                    }
                }
            } else if (player.hasPermission("extractionchest.admin") && plugin.getConfig().getBoolean("region.always-show-regions", false)) {
                targetRegion = plugin.getRegionManager().getRegionAt(player.getLocation());
                if (targetRegion != null) {
                    shouldShow = true;
                }
            }

            Set<Location> currentVisuals = activeVisuals.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());

            if (shouldShow) {
                Set<Location> newVisuals = new HashSet<>();
                World world = null;
                
                if (targetRegion != null) {
                    world = Bukkit.getWorld(targetRegion.getWorld());
                    if (world != null) {
                        int minX = targetRegion.getMinX();
                        int maxX = targetRegion.getMaxX();
                        int minZ = targetRegion.getMinZ();
                        int maxZ = targetRegion.getMaxZ();
                        
                        if (maxX - minX < 200 && maxZ - minZ < 200) {
                            for (int x = minX; x <= maxX; x++) {
                                int y1 = world.getHighestBlockYAt(x, minZ);
                                newVisuals.add(new Location(world, x, y1, minZ));
                                
                                int y2 = world.getHighestBlockYAt(x, maxZ);
                                newVisuals.add(new Location(world, x, y2, maxZ));
                            }
                            for (int z = minZ + 1; z < maxZ; z++) {
                                int y1 = world.getHighestBlockYAt(minX, z);
                                newVisuals.add(new Location(world, minX, y1, z));
                                
                                int y2 = world.getHighestBlockYAt(maxX, z);
                                newVisuals.add(new Location(world, maxX, y2, z));
                            }
                        }
                    }
                } else if (selection.getPos1() != null && selection.getPos2() == null) {
                    world = selection.getPos1().getWorld();
                    int x = selection.getPos1().getBlockX();
                    int z = selection.getPos1().getBlockZ();
                    int y = world.getHighestBlockYAt(x, z);
                    newVisuals.add(new Location(world, x, y, z));
                } else if (selection.getPos1() == null && selection.getPos2() != null) {
                    world = selection.getPos2().getWorld();
                    int x = selection.getPos2().getBlockX();
                    int z = selection.getPos2().getBlockZ();
                    int y = world.getHighestBlockYAt(x, z);
                    newVisuals.add(new Location(world, x, y, z));
                } else if (selection.getPos1() != null && selection.getPos2() != null) {
                    if (selection.getPos1().getWorld().equals(selection.getPos2().getWorld())) {
                        world = selection.getPos1().getWorld();
                        int minX = selection.getMinX();
                        int maxX = selection.getMaxX();
                        int minZ = selection.getMinZ();
                        int maxZ = selection.getMaxZ();
                        
                        if (maxX - minX < 200 && maxZ - minZ < 200) {
                            for (int x = minX; x <= maxX; x++) {
                                int y1 = world.getHighestBlockYAt(x, minZ);
                                newVisuals.add(new Location(world, x, y1, minZ));
                                
                                int y2 = world.getHighestBlockYAt(x, maxZ);
                                newVisuals.add(new Location(world, x, y2, maxZ));
                            }
                            for (int z = minZ + 1; z < maxZ; z++) {
                                int y1 = world.getHighestBlockYAt(minX, z);
                                newVisuals.add(new Location(world, minX, y1, z));
                                
                                int y2 = world.getHighestBlockYAt(maxX, z);
                                newVisuals.add(new Location(world, maxX, y2, z));
                            }
                        }
                    }
                }

                for (Location loc : currentVisuals) {
                    if (!newVisuals.contains(loc) && loc.getWorld().equals(player.getWorld())) {
                        player.sendBlockChange(loc, loc.getBlock().getBlockData());
                    }
                }
                
                for (Location loc : newVisuals) {
                    if (!currentVisuals.contains(loc) && loc.getWorld().equals(player.getWorld())) {
                        player.sendBlockChange(loc, redWool);
                    }
                }
                
                activeVisuals.put(player.getUniqueId(), newVisuals);
                
            } else {
                if (selection != null) {
                    plugin.getRegionManager().removeSelection(player.getUniqueId());
                    player.sendMessage("§cSelection cancelled.");
                }

                if (!currentVisuals.isEmpty()) {
                    for (Location loc : currentVisuals) {
                        if (loc.getWorld().equals(player.getWorld())) {
                            player.sendBlockChange(loc, loc.getBlock().getBlockData());
                        }
                    }
                    currentVisuals.clear();
                }
            }
        }
    }
    
    public void clearAll() {
        for (Map.Entry<UUID, Set<Location>> entry : activeVisuals.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                for (Location loc : entry.getValue()) {
                    if (loc.getWorld().equals(player.getWorld())) {
                        player.sendBlockChange(loc, loc.getBlock().getBlockData());
                    }
                }
            }
        }
        activeVisuals.clear();
    }
}
