package com.criztiandev.extractionregion.managers;

import com.criztiandev.extractionchest.ExtractionChestPlugin;
import com.criztiandev.extractionchest.managers.ChestInstanceManager;
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

    @Mock
    private RegionStorageProvider storageProviderMock;

    @Mock
    private FileConfiguration configMock;

    private RegionManager regionManager;
    private SavedRegion testRegion;

    @BeforeEach
    public void setup() {
        regionManager = new RegionManager(pluginMock);

        // Region from X: 0 to 100, Z: 0 to 100
        testRegion = new SavedRegion("testRegion", "world", 0, 100, 0, 100);
        testRegion.setResetIntervalMinutes(60);

        when(pluginMock.getExtractionChestApi()).thenReturn(extractionChestApiMock);
        when(extractionChestApiMock.getChestInstanceManager()).thenReturn(chestInstanceManagerMock);
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
                when(inst.getWorld()).thenReturn("world");
                when(inst.getX()).thenReturn(50);
                when(inst.getZ()).thenReturn(50);
                mockInstances.add(inst);
            }

            // 2. Inject 950 chests OUTSIDE the bounds
            for (int i = 0; i < 950; i++) {
                ChestInstance inst = mock(ChestInstance.class);
                when(inst.getWorld()).thenReturn("world");
                when(inst.getX()).thenReturn(200);
                when(inst.getZ()).thenReturn(200);
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
}
