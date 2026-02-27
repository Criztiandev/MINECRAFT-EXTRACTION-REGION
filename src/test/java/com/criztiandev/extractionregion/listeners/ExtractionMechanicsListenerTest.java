package com.criztiandev.extractionregion.listeners;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.managers.RegionManager;
import com.criztiandev.extractionregion.models.RegionType;
import com.criztiandev.extractionregion.models.SavedRegion;
import com.criztiandev.extractionregion.tasks.ExtractionTask;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExtractionMechanicsListenerTest {

    @Mock private ExtractionRegionPlugin plugin;
    @Mock private RegionManager regionManager;
    @Mock private ExtractionTask extractionTask;
    @Mock private Player player;
    @Mock private World world;
    @Mock private Block block;

    private ExtractionMechanicsListener listener;
    private SavedRegion extractionRegion;
    private Location regionLocation;

    @BeforeEach
    public void setup() {
        listener = new ExtractionMechanicsListener(plugin);
        
        extractionRegion = new SavedRegion("test_extract", "world", 0, 10, 0, 10);
        extractionRegion.setType(RegionType.EXTRACTION);
        regionLocation = new Location(world, 5, 64, 5);
        extractionRegion.setConduitLocation(regionLocation);
        
        lenient().when(plugin.getRegionManager()).thenReturn(regionManager);
        lenient().when(world.getName()).thenReturn("world");
        lenient().when(block.getWorld()).thenReturn(world);
        lenient().when(block.getLocation()).thenReturn(regionLocation);
        lenient().when(player.hasPermission(anyString())).thenReturn(false);
    }

    @Test
    public void testBlockBreak_InsideRegion_Cancelled() {
        when(regionManager.getRegions()).thenReturn(Collections.singletonList(extractionRegion));
        
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);
        
        assert(event.isCancelled());
        verify(player).sendMessage(contains("cannot break"));
    }

    @Test
    public void testBlockPlace_InsideRegion_Cancelled() {
        when(regionManager.getRegions()).thenReturn(Collections.singletonList(extractionRegion));
        
        BlockPlaceEvent event = mock(BlockPlaceEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getBlock()).thenReturn(block);
        
        listener.onBlockPlace(event);
        
        verify(event).setCancelled(true);
        verify(player).sendMessage(contains("cannot place"));
    }

    @Test
    public void testPlayerDamage_CancelsExtraction() {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(plugin.getExtractionTask()).thenReturn(extractionTask);

        listener.onPlayerDamage(event);

        verify(extractionTask).cancelExtractionByDamage(player);
    }

    @Test
    public void testPlayerInteract_RightClickButton_StartsExtraction() {
        when(regionManager.getRegions()).thenReturn(Collections.singletonList(extractionRegion));
        when(plugin.getExtractionTask()).thenReturn(extractionTask);

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, null);
        
        listener.onPlayerInteract(event);

        verify(extractionTask).handleButtonPress(player, extractionRegion);
    }

    @Test
    public void testPlayerInteract_LeftClick_Ignored() {
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, null);
        
        listener.onPlayerInteract(event);

        verify(plugin, never()).getExtractionTask();
    }
}
