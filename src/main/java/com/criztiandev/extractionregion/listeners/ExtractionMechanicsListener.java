package com.criztiandev.extractionregion.listeners;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionType;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

public class ExtractionMechanicsListener implements Listener {

    private final ExtractionRegionPlugin plugin;

    public ExtractionMechanicsListener(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    // 1. Block Protection: Prevent placing/breaking inside EXTRACTION regions
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        boolean hasBypass = player.hasPermission("extractionchest.admin") && player.isSneaking();

        Location loc = event.getBlock().getLocation();
        for (SavedRegion region : plugin.getRegionManager().getRegions()) {
            if (region.getType() == RegionType.EXTRACTION && region.getWorld().equals(loc.getWorld().getName())) {
                if (loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX() &&
                    loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ()) {
                    
                    if (!hasBypass) {
                        event.setCancelled(true);
                        player.sendMessage("§cYou cannot break blocks in an extraction zone!");
                    } else {
                        player.sendMessage("§a[ExtractionRegion] §eBypassed region protection.");
                    }
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        boolean hasBypass = player.hasPermission("extractionchest.admin") && player.isSneaking();

        Location loc = event.getBlock().getLocation();
        for (SavedRegion region : plugin.getRegionManager().getRegions()) {
            if (region.getType() == RegionType.EXTRACTION && region.getWorld().equals(loc.getWorld().getName())) {
                if (loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX() &&
                    loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ()) {
                    
                    if (!hasBypass) {
                        event.setCancelled(true);
                        player.sendMessage("§cYou cannot place blocks in an extraction zone!");
                    } else {
                        player.sendMessage("§a[ExtractionRegion] §eBypassed region protection.");
                    }
                    return;
                }
            }
        }
    }

    private boolean isInsideExtractionRegion(Location loc) {
        for (SavedRegion region : plugin.getRegionManager().getRegions()) {
            if (region.getType() == RegionType.EXTRACTION && region.getWorld().equals(loc.getWorld().getName())) {
                if (loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX() &&
                    loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onBlockPhysics(org.bukkit.event.block.BlockPhysicsEvent event) {
        if (isInsideExtractionRegion(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        event.blockList().removeIf(block -> isInsideExtractionRegion(block.getLocation()));
    }

    @EventHandler
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        event.blockList().removeIf(block -> isInsideExtractionRegion(block.getLocation()));
    }

    @EventHandler
    public void onPistonExtend(org.bukkit.event.block.BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (isInsideExtractionRegion(block.getLocation()) || isInsideExtractionRegion(block.getLocation().add(event.getDirection().getDirection()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(org.bukkit.event.block.BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (isInsideExtractionRegion(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // 2. Combat Tag Cancellation: Taking damage from an entity cancels extraction
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            // Tell the task to cancel the extraction session if it exists
            if (plugin.getExtractionTask() != null) {
                plugin.getExtractionTask().cancelExtractionByDamage(victim);
            }
        }
    }

    // 2.5 Drop Zone Landing logic: Strip Slow Falling when they touch the ground
    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.getRegionManager().isActiveDropZonePlayer(player.getUniqueId())) {
            // Check if player has landed
            org.bukkit.Location to = event.getTo();
            if (to != null && ((org.bukkit.entity.Entity) player).isOnGround()) {
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING);
                plugin.getRegionManager().removeActiveDropZonePlayer(player.getUniqueId());
            }
        }
    }

    // 3. Button Press: Start extraction process
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Action action = event.getAction();
        
        if (block == null) return;
        
        // Handle Conduit Selection Mode First
        if (plugin.getRegionManager().isConduitSelectingPlayer(player.getUniqueId())) {
            event.setCancelled(true);
            
            if (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK) {
                String regionId = plugin.getRegionManager().getConduitSelectingRegionId(player.getUniqueId());
                if (regionId == null) {
                    plugin.getRegionManager().removeConduitSelectingPlayer(player.getUniqueId());
                    return;
                }
                
                SavedRegion region = plugin.getRegionManager().getRegion(regionId);
                if (region == null) {
                    player.sendMessage("§cRegion not found.");
                    plugin.getRegionManager().removeConduitSelectingPlayer(player.getUniqueId());
                    return;
                }

                if (block.getX() >= region.getMinX() && block.getX() <= region.getMaxX() &&
                    block.getZ() >= region.getMinZ() && block.getZ() <= region.getMaxZ() &&
                    block.getWorld().getName().equals(region.getWorld())) {
                    
                    region.setConduitLocation(block.getLocation());
                    plugin.getRegionManager().saveRegion(region);
                    player.sendMessage("§aConduit extraction point set successfully for region §e" + regionId + "§a!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                } else {
                    player.sendMessage("§cThe conduit block must be inside the region boundaries!");
                }
                
                plugin.getRegionManager().removeConduitSelectingPlayer(player.getUniqueId());
            }
            return;
        }

        if (action != Action.RIGHT_CLICK_BLOCK) return;
        
        org.bukkit.block.Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        
        Location clickLoc = clickedBlock.getLocation();
        for (SavedRegion region : plugin.getRegionManager().getRegions()) {
            if (region.getType() == RegionType.EXTRACTION && region.getConduitLocation() != null) {
                Location conduitLoc = region.getConduitLocation();
                if (conduitLoc.getWorld().equals(clickLoc.getWorld()) && 
                    clickLoc.distanceSquared(conduitLoc) <= 2.0) { // Catch button attached to it
                    
                    // Found an interaction on the extraction point!
                    if (plugin.getExtractionTask() != null) {
                        plugin.getExtractionTask().handleButtonPress(event.getPlayer(), region);
                    }
                    return;
                }
            }
        }
    }
}
