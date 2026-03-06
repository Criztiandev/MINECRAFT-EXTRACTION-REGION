package com.criztiandev.extractionregion.managers;

import com.criztiandev.extractionchest.ExtractionChestPlugin;
import com.criztiandev.extractionchest.managers.ChestInstanceManager;
import com.criztiandev.extractionchest.managers.HologramManager;
import com.criztiandev.extractionchest.models.ChestInstance;
import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.SavedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.criztiandev.extractionregion.storage.RegionStorageProvider;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegionManagerTest {

    @Mock
    private ExtractionRegionPlugin pluginMock;

    @Mock
    private ExtractionChestPlugin extractionChestApiMock;

    @Mock
    private ChestInstanceManager chestInstanceManagerMock;

    @Mock
    private World worldMock;

    @Mock
    private BukkitScheduler schedulerMock;

    @Mock
    private BukkitTask bukkitTaskMock;

    private RegionStorageProvider storageProviderMock;
    private org.bukkit.configuration.file.FileConfiguration configMock;
    private com.criztiandev.extractionchest.managers.HologramManager hologramManagerMock;
    private java.util.logging.Logger loggerMock;

    private RegionManager regionManager;
    private SavedRegion testRegion;

    @BeforeEach
    public void setup() {
        // Explicitly mock these fields
        pluginMock = mock(ExtractionRegionPlugin.class);
        extractionChestApiMock = mock(ExtractionChestPlugin.class);
        chestInstanceManagerMock = mock(ChestInstanceManager.class);
        hologramManagerMock = mock(com.criztiandev.extractionchest.managers.HologramManager.class);
        storageProviderMock = mock(RegionStorageProvider.class);
        configMock = mock(org.bukkit.configuration.file.FileConfiguration.class);
        loggerMock = java.util.logging.Logger.getLogger("TestLogger");

        regionManager = new RegionManager(pluginMock);

        // Region from X: 0 to 100, Z: 0 to 100
        testRegion = new SavedRegion("testRegion", "world", 0, 100, 0, 100);
        testRegion.setResetIntervalMinutes(60);

        when(pluginMock.getExtractionChestApi()).thenReturn(extractionChestApiMock);
        when(pluginMock.getConfig()).thenReturn(configMock);
        lenient().when(pluginMock.getLogger()).thenReturn(loggerMock);
        when(extractionChestApiMock.getChestInstanceManager()).thenReturn(chestInstanceManagerMock);
        lenient().when(extractionChestApiMock.getHologramManager()).thenReturn(hologramManagerMock);
        lenient().when(pluginMock.getStorageProvider()).thenReturn(storageProviderMock);
        lenient().when(pluginMock.getConfig()).thenReturn(configMock);
        lenient().when(configMock.getInt("region.replenish-batch-size", 5)).thenReturn(5);
    }

    @Test
    public void testForceReplenish_BoundsFiltering() {
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(worldMock);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(schedulerMock);

            when(schedulerMock.runTaskTimer(any(ExtractionRegionPlugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(bukkitTaskMock);

            List<ChestInstance> mockInstances = new ArrayList<>();

            // 1. Inject 50 chests INSIDE the bounds
            for (int i = 0; i < 50; i++) {
                ChestInstance inst = mock(ChestInstance.class);
                lenient().when(inst.getWorld()).thenReturn("world");
                lenient().when(inst.getX()).thenReturn(50);
                lenient().when(inst.getY()).thenReturn(64);
                lenient().when(inst.getZ()).thenReturn(50);
                lenient().when(inst.getId()).thenReturn("inside-" + i);
                lenient().when(inst.isStationary()).thenReturn(false);
                lenient().when(inst.getBaseParentName()).thenReturn("COMMON");
                lenient().when(inst.getFallbackParentName()).thenReturn("");
                lenient().when(inst.getSpawnChance()).thenReturn(100);
                mockInstances.add(inst);

                org.bukkit.block.Block blockMock = mock(org.bukkit.block.Block.class);
                lenient().when(worldMock.getBlockAt(50, 64, 50)).thenReturn(blockMock);
            }

            // 2. Inject 950 chests OUTSIDE the bounds
            for (int i = 0; i < 950; i++) {
                ChestInstance inst = mock(ChestInstance.class);
                lenient().when(inst.getWorld()).thenReturn("world");
                lenient().when(inst.getX()).thenReturn(200);
                lenient().when(inst.getZ()).thenReturn(200);
                lenient().when(inst.getId()).thenReturn("outside-" + i);
                lenient().when(inst.isStationary()).thenReturn(false);
                mockInstances.add(inst);
            }

            // Total: 1000 injected instances
            when(chestInstanceManagerMock.getAllInstances()).thenReturn(mockInstances);

            // Execute
            int replenishedCount = regionManager.forceReplenish(testRegion);

            // Verify count mathematically proves boundaries filtered all 950 chests
            assertEquals(50, replenishedCount, "Exactly 50 chests should be captured perfectly within the geometry.");
        }
    }

    @Test
    public void testForceReplenish_AppliesSavedRegionOverrides() {
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(worldMock);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(schedulerMock);

            when(schedulerMock.runTaskTimer(any(ExtractionRegionPlugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(bukkitTaskMock);

            List<ChestInstance> mockInstances = new ArrayList<>();
            ChestInstance inst1 = mock(ChestInstance.class);
            lenient().when(inst1.getWorld()).thenReturn("world");
            lenient().when(inst1.getX()).thenReturn(50);
            lenient().when(inst1.getY()).thenReturn(64);
            lenient().when(inst1.getZ()).thenReturn(50);
            lenient().when(inst1.getId()).thenReturn("inside-1");
            lenient().when(inst1.getBaseParentName()).thenReturn("COMMON");
            mockInstances.add(inst1);

            ChestInstance inst2 = mock(ChestInstance.class);
            lenient().when(inst2.getWorld()).thenReturn("world");
            lenient().when(inst2.getX()).thenReturn(60);
            lenient().when(inst2.getY()).thenReturn(64);
            lenient().when(inst2.getZ()).thenReturn(60);
            lenient().when(inst2.getId()).thenReturn("inside-2");
            lenient().when(inst2.getBaseParentName()).thenReturn("MYTHIC");
            mockInstances.add(inst2);

            when(chestInstanceManagerMock.getAllInstances()).thenReturn(mockInstances);

            // Configure SavedRegion overrides
            testRegion.getChestStationaryOverrides().put("50,64,50", true);
            testRegion.getChestChanceOverrides().put("50,64,50", 15);
            testRegion.getChestFallbackOverrides().put("50,64,50", "FALLBACK_DEF");

            org.bukkit.block.Block blockMock = mock(org.bukkit.block.Block.class);
            lenient().when(worldMock.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(blockMock);

            int replenishedCount = regionManager.forceReplenish(testRegion);

            assertEquals(2, replenishedCount);
            verify(inst1).setStationary(true);
            verify(inst1).setSpawnChance(15);
            verify(inst1).setFallbackParentName("FALLBACK_DEF");

            // For inst2, it had no overrides so it should default to false (shufflable) regardless of its tier
            verify(inst2).setStationary(false); 
            assertEquals(false, testRegion.getChestStationaryOverrides().get("60,64,60"));
        }
    }
}
