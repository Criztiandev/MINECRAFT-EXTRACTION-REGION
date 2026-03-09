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
    private final Map<String, Set<Location>> cachedPerimeters = new HashMap<>();
    private final BlockData redWool;

    public SelectionVisualizerTask(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
        this.redWool = Bukkit.createBlockData(Material.RED_WOOL);
    }

    public void invalidateCache(String regionId) {
        cachedPerimeters.remove(regionId.toLowerCase());
    }

    // Expose active visuals to prevent Z-fighting with other block visualizer tasks
    public boolean isVisualBlockActive(Player player, Location loc) {
        Set<Location> visuals = activeVisuals.get(player.getUniqueId());
        return visuals != null && visuals.contains(loc);
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
                        String regionId = targetRegion.getId().toLowerCase();
                        
                        Set<Location> cached = cachedPerimeters.get(regionId);
                        if (cached != null) {
                            newVisuals.addAll(cached);
                        } else {
                            int minX = targetRegion.getMinX();
                            int maxX = targetRegion.getMaxX();
                            int minZ = targetRegion.getMinZ();
                            int maxZ = targetRegion.getMaxZ();
                            int floorY = targetRegion.getMinY();

                            if (maxX - minX < 200 && maxZ - minZ < 200) {
                                boolean canCache = true;
                                if (!world.isChunkLoaded(minX >> 4, minZ >> 4)) canCache = false;
                                if (!world.isChunkLoaded(maxX >> 4, maxZ >> 4)) canCache = false;

                                if (canCache) {
                                    Set<Location> perimeter = new HashSet<>();
                                    for (int x = minX; x <= maxX; x++) {
                                        perimeter.add(new Location(world, x, floorY, minZ));
                                        perimeter.add(new Location(world, x, floorY, maxZ));
                                    }
                                    for (int z = minZ + 1; z < maxZ; z++) {
                                        perimeter.add(new Location(world, minX, floorY, z));
                                        perimeter.add(new Location(world, maxX, floorY, z));
                                    }
                                    cachedPerimeters.put(regionId, perimeter);
                                    newVisuals.addAll(perimeter);
                                }
                            }
                        }
                    }
                } else if (selection.getPos1() != null && selection.getPos2() == null) {
                    world = selection.getPos1().getWorld();
                    Location pos1 = selection.getPos1();
                    // Use the exact Y the player clicked — getHighestBlockYAt returns canopy/tree tops.
                    newVisuals.add(new Location(world, pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ()));
                } else if (selection.getPos1() == null && selection.getPos2() != null) {
                    world = selection.getPos2().getWorld();
                    Location pos2 = selection.getPos2();
                    newVisuals.add(new Location(world, pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ()));
                } else if (selection.getPos1() != null && selection.getPos2() != null) {
                    if (selection.getPos1().getWorld().equals(selection.getPos2().getWorld())) {
                        world = selection.getPos1().getWorld();
                        int minX = selection.getMinX();
                        int maxX = selection.getMaxX();
                        int minZ = selection.getMinZ();
                        int maxZ = selection.getMaxZ();
                        // Use the lower of the two clicked Y positions as the outline floor.
                        // getHighestBlockYAt places wool on leaf canopies above the selection.
                        int floorY = Math.min(selection.getPos1().getBlockY(), selection.getPos2().getBlockY());

                        if (maxX - minX < 200 && maxZ - minZ < 200) {
                            for (int x = minX; x <= maxX; x++) {
                                newVisuals.add(new Location(world, x, floorY, minZ));
                                newVisuals.add(new Location(world, x, floorY, maxZ));
                            }
                            for (int z = minZ + 1; z < maxZ; z++) {
                                newVisuals.add(new Location(world, minX, floorY, z));
                                newVisuals.add(new Location(world, maxX, floorY, z));
                            }
                        }
                    }
                }

                for (Location loc : currentVisuals) {
                    if (!newVisuals.contains(loc) && loc.getWorld().equals(player.getWorld())) {
                        boolean isExtractionBlock = false;
                        if (plugin.getParticleVisualizerTask() != null) {
                            isExtractionBlock = plugin.getParticleVisualizerTask().isFakeBlockActive(player, loc);
                        }
                        if (!isExtractionBlock) {
                            player.sendBlockChange(loc, loc.getBlock().getBlockData());
                        }
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
                            boolean isExtractionBlock = false;
                            if (plugin.getParticleVisualizerTask() != null) {
                                isExtractionBlock = plugin.getParticleVisualizerTask().isFakeBlockActive(player, loc);
                            }
                            if (!isExtractionBlock) {
                                player.sendBlockChange(loc, loc.getBlock().getBlockData());
                            }
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
