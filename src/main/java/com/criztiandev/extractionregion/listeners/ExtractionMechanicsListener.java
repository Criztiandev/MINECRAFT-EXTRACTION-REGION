package com.criztiandev.extractionregion.listeners;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionType;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.Location;
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
        if (event.getPlayer().hasPermission("extractionchest.admin")) return;

        Location loc = event.getBlock().getLocation();
        for (SavedRegion region : plugin.getRegionManager().getRegions()) {
            if (region.getType() == RegionType.EXTRACTION && region.getWorld().equals(loc.getWorld().getName())) {
                if (loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX() &&
                    loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ()) {
                    
                    event.setCancelled(true);
                    event.getPlayer().sendMessage("§cYou cannot break blocks in an extraction zone!");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getPlayer().hasPermission("extractionchest.admin")) return;

        Location loc = event.getBlock().getLocation();
        for (SavedRegion region : plugin.getRegionManager().getRegions()) {
            if (region.getType() == RegionType.EXTRACTION && region.getWorld().equals(loc.getWorld().getName())) {
                if (loc.getBlockX() >= region.getMinX() && loc.getBlockX() <= region.getMaxX() &&
                    loc.getBlockZ() >= region.getMinZ() && loc.getBlockZ() <= region.getMaxZ()) {
                    
                    event.setCancelled(true);
                    event.getPlayer().sendMessage("§cYou cannot place blocks in an extraction zone!");
                    return;
                }
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

    // 3. Button Press: Start extraction process
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        org.bukkit.block.Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        
        // Check if the clicked block is a button (or just the conduit itself based on setup)
        // We will just check if this block's location matches any region's conduit location.
        // It might be the button ON the conduit, or the conduit itself. 
        // For simplicity, we check if the clicked block is within 1 block of the conduit location.
        
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
