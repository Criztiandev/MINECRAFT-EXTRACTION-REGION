package com.criztiandev.extractionregion.listeners;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.managers.RegionManager;
import com.criztiandev.extractionregion.models.RegionType;
import com.criztiandev.extractionregion.models.SavedRegion;
import com.criztiandev.extractionregion.tasks.ExtractionTask;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExtractionMechanicsTest {

    @Mock
    private ExtractionRegionPlugin pluginMock;
    
    @Mock
    private RegionManager regionManagerMock;
    
    @Mock
    private ExtractionTask extractionTaskMock;
    
    @Mock
    private Player playerMock;
    
    @Mock
    private World worldMock;
    
    @Mock
    private Block blockMock;

    private ExtractionMechanicsListener listener;
    private SavedRegion extractionRegion;

    @BeforeEach
    public void setup() {
        listener = new ExtractionMechanicsListener(pluginMock);
        
        extractionRegion = new SavedRegion("testRegion", "world", 10, 20, 10, 20);
        extractionRegion.setType(RegionType.EXTRACTION);
    }

    @Test
    public void testBlockBreak_InsideExtraction_Cancelled() {
        when(pluginMock.getRegionManager()).thenReturn(regionManagerMock);
        when(regionManagerMock.getRegions()).thenReturn(Collections.singletonList(extractionRegion));
        
        Location blockLoc = new Location(worldMock, 15, 60, 15);
        when(worldMock.getName()).thenReturn("world");
        when(blockMock.getLocation()).thenReturn(blockLoc);
        
        BlockBreakEvent event = new BlockBreakEvent(blockMock, playerMock);
        when(playerMock.hasPermission("extractionchest.admin")).thenReturn(false);
        
        listener.onBlockBreak(event);
        
        verify(playerMock).sendMessage("§cYou cannot break blocks in an extraction zone!");
        assertTrue(event.isCancelled());
    }

    @Test
    public void testBlockBreak_OutsideExtraction_NotCancelled() {
        when(pluginMock.getRegionManager()).thenReturn(regionManagerMock);
        when(regionManagerMock.getRegions()).thenReturn(Collections.singletonList(extractionRegion));
        
        Location blockLoc = new Location(worldMock, 0, 60, 0); // Outside bounded zone
        when(worldMock.getName()).thenReturn("world");
        when(blockMock.getLocation()).thenReturn(blockLoc);
        
        BlockBreakEvent event = new BlockBreakEvent(blockMock, playerMock);
        when(playerMock.hasPermission("extractionchest.admin")).thenReturn(false);
        
        listener.onBlockBreak(event);
        
        verify(playerMock, never()).sendMessage(anyString());
        assertFalse(event.isCancelled());
    }

    @Test
    public void testBlockPlace_InsideExtraction_Cancelled() {
        when(pluginMock.getRegionManager()).thenReturn(regionManagerMock);
        when(regionManagerMock.getRegions()).thenReturn(Collections.singletonList(extractionRegion));
        
        Location blockLoc = new Location(worldMock, 12, 60, 18);
        when(worldMock.getName()).thenReturn("world");
        when(blockMock.getLocation()).thenReturn(blockLoc);
        
        // Mock PlaceEvent constructor args lazily by instantiating minimal event
        @SuppressWarnings("deprecation")
        BlockPlaceEvent event = new BlockPlaceEvent(blockMock, null, null, null, playerMock, true, org.bukkit.inventory.EquipmentSlot.HAND);
        when(playerMock.hasPermission("extractionchest.admin")).thenReturn(false);
        
        listener.onBlockPlace(event);
        
        verify(playerMock).sendMessage("§cYou cannot place blocks in an extraction zone!");
        assertTrue(event.isCancelled());
    }

    @Test
    public void testPvpInterruption_CancelsExtraction() {
        when(pluginMock.getExtractionTask()).thenReturn(extractionTaskMock);
        
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(playerMock);
        
        listener.onPlayerDamage(event);
        
        verify(extractionTaskMock).cancelExtractionByDamage(playerMock);
    }
}
