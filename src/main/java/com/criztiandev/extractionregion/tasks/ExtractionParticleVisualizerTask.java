package com.criztiandev.extractionregion.tasks;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionType;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ExtractionParticleVisualizerTask extends BukkitRunnable {

    private final ExtractionRegionPlugin plugin;
    private final BlockData redWool;
    
    // Store which players currently see which fake blocks so we can remove them
    private final Map<UUID, Set<Location>> activeFakeBlocks = new HashMap<>();

    public ExtractionParticleVisualizerTask(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
        this.redWool = Bukkit.createBlockData(Material.RED_WOOL);
    }

    // Expose active fake blocks to prevent Z-fighting with Wand Selector visualizer
    public boolean isFakeBlockActive(Player player, Location loc) {
        Set<Location> blocks = activeFakeBlocks.get(player.getUniqueId());
        return blocks != null && blocks.contains(loc);
    }

    @Override
    public void run() {
        if (plugin.getRegionManager() == null || plugin.getRegionManager().getRegions() == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Location pLoc = player.getLocation();
            World world = pLoc.getWorld();
            if (world == null) continue;

            Set<Location> newFakeBlocks = new HashSet<>();
            Set<Location> currentFakeBlocks = activeFakeBlocks.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());

            for (SavedRegion region : plugin.getRegionManager().getRegionsInWorld(world.getName())) {
                if (region.getType() != RegionType.EXTRACTION) continue;

                int minX = region.getMinX();
                int maxX = region.getMaxX();
                int minZ = region.getMinZ();
                int maxZ = region.getMaxZ();

                // Safety check: skip massive regions to prevent lag
                if (maxX - minX > 200 || maxZ - minZ > 200) continue;

                // Optimization: only process chunks/blocks if player is reasonably close to the region box
                // Check distance from player to the closest point on the region bounding box
                int closestX = Math.max(minX, Math.min(pLoc.getBlockX(), maxX));
                int closestZ = Math.max(minZ, Math.min(pLoc.getBlockZ(), maxZ));
                
                // If player is further than 45 blocks from the nearest edge, don't show the outline to them
                if (Math.abs(pLoc.getBlockX() - closestX) > 45 || Math.abs(pLoc.getBlockZ() - closestZ) > 45) {
                    continue;
                }

                int pBlockX = pLoc.getBlockX();
                int pBlockZ = pLoc.getBlockZ();
                int viewRadius = 40; // Only render edges within 40 blocks of the player
                
                int startX = Math.max(minX, pBlockX - viewRadius);
                int endX = Math.min(maxX, pBlockX + viewRadius);
                int startZ = Math.max(minZ, pBlockZ - viewRadius);
                int endZ = Math.min(maxZ, pBlockZ + viewRadius);

                // X edges (minZ and maxZ)
                for (int x = startX; x <= endX; x++) {
                    if (Math.abs(minZ - pBlockZ) <= viewRadius) {
                        int y1 = world.getHighestBlockYAt(x, minZ);
                        newFakeBlocks.add(new Location(world, x, y1, minZ));
                    }
                    if (Math.abs(maxZ - pBlockZ) <= viewRadius) {
                        int y2 = world.getHighestBlockYAt(x, maxZ);
                        newFakeBlocks.add(new Location(world, x, y2, maxZ));
                    }
                }
                
                // Z edges (minX and maxX, skip corners)
                for (int z = Math.max(minZ + 1, startZ); z < Math.min(maxZ, endZ + 1); z++) {
                    if (Math.abs(minX - pBlockX) <= viewRadius) {
                        int y1 = world.getHighestBlockYAt(minX, z);
                        newFakeBlocks.add(new Location(world, minX, y1, z));
                    }
                    if (Math.abs(maxX - pBlockX) <= viewRadius) {
                        int y2 = world.getHighestBlockYAt(maxX, z);
                        newFakeBlocks.add(new Location(world, maxX, y2, z));
                    }
                }
                
                // Also spawn a prominent marker exactly at the conduit location if it exists
                if (region.getConduitLocation() != null) {
                    Location cLoc = region.getConduitLocation();
                    if (cLoc.distanceSquared(pLoc) < 2500 && world.isChunkLoaded(cLoc.getBlockX() >> 4, cLoc.getBlockZ() >> 4)) {
                        // Spawn a subtle beacon-like ring or floating spark above the conduit block
                        player.spawnParticle(Particle.END_ROD, cLoc.clone().add(0.5, 0.5, 0.5), 1, 0.3, 0.3, 0.3, 0.01);
                    }
                }
            }

            // Remove blocks that the player can no longer see
            for (Location loc : currentFakeBlocks) {
                if (!newFakeBlocks.contains(loc) && loc.getWorld().equals(player.getWorld())) {
                    boolean isWandBlock = false;
                    if (plugin.getVisualizerTask() != null) {
                        isWandBlock = plugin.getVisualizerTask().isVisualBlockActive(player, loc);
                    }
                    if (!isWandBlock) {
                        player.sendBlockChange(loc, loc.getBlock().getBlockData());
                    }
                }
            }
            
            // Send new blocks that the player just got in range of
            for (Location loc : newFakeBlocks) {
                if (!currentFakeBlocks.contains(loc) && loc.getWorld().equals(player.getWorld())) {
                    player.sendBlockChange(loc, redWool);
                }
            }
            
            activeFakeBlocks.put(player.getUniqueId(), newFakeBlocks);
        }
    }
    
    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        // Clean up all fake blocks when plugin disables
        for (Map.Entry<UUID, Set<Location>> entry : activeFakeBlocks.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                for (Location loc : entry.getValue()) {
                    if (loc.getWorld().equals(player.getWorld())) {
                        player.sendBlockChange(loc, loc.getBlock().getBlockData());
                    }
                }
            }
        }
        activeFakeBlocks.clear();
    }
}
