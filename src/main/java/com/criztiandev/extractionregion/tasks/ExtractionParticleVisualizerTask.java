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

                if (maxX - minX > 200 || maxZ - minZ > 200) {
                    continue;
                }

                int floorY = region.getMinY();
                int closestX = Math.max(minX, Math.min(pLoc.getBlockX(), maxX));
                int closestZ = Math.max(minZ, Math.min(pLoc.getBlockZ(), maxZ));

                if (Math.abs(pLoc.getBlockX() - closestX) > 45 || Math.abs(pLoc.getBlockZ() - closestZ) > 45) {
                    continue;
                }

                int pBlockX = pLoc.getBlockX();
                int pBlockZ = pLoc.getBlockZ();
                int viewRadius = 40;

                int startX = Math.max(minX, pBlockX - viewRadius);
                int endX   = Math.min(maxX, pBlockX + viewRadius);
                int startZ = Math.max(minZ, pBlockZ - viewRadius);
                int endZ   = Math.min(maxZ, pBlockZ + viewRadius);

                for (int x = startX; x <= endX; x++) {
                    if (Math.abs(minZ - pBlockZ) <= viewRadius) {
                        newFakeBlocks.add(new Location(world, x, floorY, minZ));
                    }
                    if (Math.abs(maxZ - pBlockZ) <= viewRadius) {
                        newFakeBlocks.add(new Location(world, x, floorY, maxZ));
                    }
                }

                for (int z = Math.max(minZ + 1, startZ); z < Math.min(maxZ, endZ + 1); z++) {
                    if (Math.abs(minX - pBlockX) <= viewRadius) {
                        newFakeBlocks.add(new Location(world, minX, floorY, z));
                    }
                    if (Math.abs(maxX - pBlockX) <= viewRadius) {
                        newFakeBlocks.add(new Location(world, maxX, floorY, z));
                    }
                }
                
                if (region.getConduitLocation() != null) {
                    Location cLoc = region.getConduitLocation();
                    if (cLoc.distanceSquared(pLoc) < 2500 && world.isChunkLoaded(cLoc.getBlockX() >> 4, cLoc.getBlockZ() >> 4)) {
                        player.spawnParticle(Particle.END_ROD, cLoc.clone().add(0.5, 0.5, 0.5), 1, 0.3, 0.3, 0.3, 0.01);
                    }
                }
            }

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
