package com.criztiandev.extractionregion.tasks;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.RegionType;
import com.criztiandev.extractionregion.models.SavedRegion;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExtractionTaskTest {

    @Mock private ExtractionRegionPlugin plugin;
    @Mock private Player player;
    @Mock private World world;
    @Mock private FileConfiguration config;
    @Mock private com.criztiandev.extractionregion.managers.RegionManager regionManager;

    private ExtractionTask task;
    private SavedRegion region;
    private MockedStatic<Bukkit> mockedBukkit;
    private Location playerLoc;

    @BeforeEach
    public void setup() {
        task = new ExtractionTask(plugin);
        
        region = new SavedRegion("test_extract", "world", 0, 10, 0, 10);
        region.setType(RegionType.EXTRACTION);
        region.setMimicEnabled(false);
        
        playerLoc = new Location(world, 5, 64, 5);
        lenient().when(player.getLocation()).thenReturn(playerLoc);
        lenient().when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().when(player.getName()).thenReturn("Tester");
        lenient().when(world.getName()).thenReturn("world");
        
        Player.Spigot spigotMock = mock(Player.Spigot.class);
        lenient().when(player.spigot()).thenReturn(spigotMock);
        
        region.setConduitLocation(new Location(world, 5, 64, 5));

        lenient().when(plugin.getConfig()).thenReturn(config);
        lenient().when(plugin.getRegionManager()).thenReturn(regionManager);
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(config.getInt(anyString(), anyInt())).thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(config.getBoolean(anyString(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(config.getBoolean("extraction.effects.fireworks_enabled", true)).thenReturn(false);

        lenient().when(player.isOnline()).thenReturn(true);
        lenient().when(player.getWorld()).thenReturn(world);
        lenient().when(world.getSpawnLocation()).thenReturn(playerLoc);

        org.bukkit.entity.Firework fwMock = mock(org.bukkit.entity.Firework.class);
        org.bukkit.inventory.meta.FireworkMeta fwMetaMock = mock(org.bukkit.inventory.meta.FireworkMeta.class);
        lenient().when(world.spawnEntity(any(Location.class), eq(org.bukkit.entity.EntityType.FIREWORK_ROCKET))).thenReturn(fwMock);
        lenient().when(fwMock.getFireworkMeta()).thenReturn(fwMetaMock);

        mockedBukkit = mockStatic(Bukkit.class);
        mockedBukkit.when(Bukkit::getOnlinePlayers).thenReturn(Collections.singletonList(player));
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        mockedBukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(world);
        
        org.bukkit.plugin.PluginManager pluginManagerMock = mock(org.bukkit.plugin.PluginManager.class);
        mockedBukkit.when(Bukkit::getPluginManager).thenReturn(pluginManagerMock);
    }

    @AfterEach
    public void tearDown() {
        mockedBukkit.close();
    }

    @Test
    public void testHandleButtonPress_OnCooldown_Rejects() {
        // Set cooldown to be active (in the future)
        region.setCooldownEndTime(System.currentTimeMillis() + 10000);
        
        task.handleButtonPress(player, region);

        verify(player).sendMessage(org.mockito.ArgumentMatchers.contains("is on cooldown"));
        // We verify that no broadcast was done
        verify(player, never()).spigot();
    }

    @Test
    public void testHandleButtonPress_Success_StartsSession() {
        // Ensure no cooldown
        region.setCooldownEndTime(0);

        task.handleButtonPress(player, region);

        // Verify action bar was sent
        verify(player).sendMessage(org.mockito.ArgumentMatchers.contains("An extraction has been initiated"));
    }

    @Test
    public void testHandleButtonPress_BypassCooldown_SuccessMultipleTimes() {
        // Ensure cooldown is technically active
        region.setCooldownEndTime(System.currentTimeMillis() + 10000);
        
        // Enable bypass
        region.setBypassCooldown(true);

        task.handleButtonPress(player, region);

        // Verify it didn't reject
        verify(player, never()).sendMessage(org.mockito.ArgumentMatchers.contains("is on cooldown"));
        // Verify it successfully started the session
        verify(player).sendMessage(org.mockito.ArgumentMatchers.contains("An extraction has been initiated"));
    }

    @Test
    public void testNormalExtraction_FullFlow_AppliesCooldownAndCapacity() {
        // Ensure no cooldown
        region.setCooldownEndTime(0);
        region.setMinCapacity(10);
        region.setMaxCapacity(10);
        region.setCurrentCapacity(10);
        region.setCooldownSequence(java.util.Arrays.asList(15));
        region.setCooldownIndex(0);
        region.setBypassCooldown(false);

        // Start session
        task.handleButtonPress(player, region);

        // Simulate time passing (force the session to finish immediately)
        ExtractionTask.ExtractionSession session = task.getSessions().get(player.getUniqueId());
        try {
            java.lang.reflect.Field field = ExtractionTask.ExtractionSession.class.getDeclaredField("startTime");
            field.setAccessible(true);
            field.setLong(session, System.currentTimeMillis() - 500000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Mock block in front of player to prevent "looked away" cancellation
        org.bukkit.block.Block block = mock(org.bukkit.block.Block.class);
        lenient().when(player.getTargetBlockExact(5)).thenReturn(block);
        Location cLoc1 = region.getConduitLocation();
        lenient().when(block.getLocation()).thenReturn(cLoc1);

        // Process tick
        task.run();

        // Session should be removed
        assertEquals(0, task.getSessions().size());

        // Region should now have 9 capacity (10 - 1)
        assertEquals(9, region.getCurrentCapacity());

        // Region should be on cooldown
        org.junit.jupiter.api.Assertions.assertTrue(region.isOnCooldown());

        // It should have saved
        verify(regionManager).saveRegion(region);
        
        // Attempting to extract again should now fail because it is on cooldown
        task.handleButtonPress(player, region);
        verify(player).sendMessage(org.mockito.ArgumentMatchers.contains("is on cooldown"));
    }

    @Test
    public void testExtraction_CancelOnMove() {
        region.setCooldownEndTime(0);
        task.handleButtonPress(player, region);
        
        // Mock player moving far away
        Location newLoc = new Location(world, 10, 64, 10); // Far from 5,64,5
        lenient().when(player.getLocation()).thenReturn(newLoc);
        
        // Mock block in front of player
        org.bukkit.block.Block block = mock(org.bukkit.block.Block.class);
        lenient().when(player.getTargetBlockExact(5)).thenReturn(block);
        Location cLoc2 = region.getConduitLocation();
        lenient().when(block.getLocation()).thenReturn(cLoc2);

        task.run();

        // Session should be removed
        assertEquals(0, task.getSessions().size());
        
        // Verify action bar was sent for cancellation
        verify(player.spigot(), atLeastOnce()).sendMessage(eq(ChatMessageType.ACTION_BAR), any(TextComponent.class));
        verify(player).playSound(any(Location.class), eq(org.bukkit.Sound.BLOCK_GLASS_BREAK), anyFloat(), anyFloat());
    }

    @Test
    public void testExtraction_CancelOnLookAway() {
        region.setCooldownEndTime(0);
        task.handleButtonPress(player, region);
        
        // Mock player looking away (target block is too far from conduit)
        org.bukkit.block.Block block = mock(org.bukkit.block.Block.class);
        lenient().when(player.getTargetBlockExact(5)).thenReturn(block);
        lenient().when(block.getLocation()).thenReturn(new Location(world, 20, 64, 20));

        task.run();

        // Session should be removed
        assertEquals(0, task.getSessions().size());
        
        // Verify action bar was sent for cancellation
        verify(player.spigot(), atLeastOnce()).sendMessage(eq(ChatMessageType.ACTION_BAR), any(TextComponent.class));
        verify(player).playSound(any(Location.class), eq(org.bukkit.Sound.BLOCK_GLASS_BREAK), anyFloat(), anyFloat());
    }

    @Test
    public void testExtraction_CancelOnDamage() {
        region.setCooldownEndTime(0);
        task.handleButtonPress(player, region);
        
        assertEquals(1, task.getSessions().size());
        
        task.cancelExtractionByDamage(player);
        
        // Session should be removed
        assertEquals(0, task.getSessions().size());
        
        // Verify action bar was sent for cancellation
        verify(player.spigot(), atLeastOnce()).sendMessage(eq(ChatMessageType.ACTION_BAR), any(TextComponent.class));
        verify(player).playSound(any(Location.class), eq(org.bukkit.Sound.BLOCK_GLASS_BREAK), anyFloat(), anyFloat());
    }
    
    @Test
    public void testExtraction_ServerConsoleCommand() {
        // Setup a region to use a command rather than raw teleportation
        region.setCooldownEndTime(0);
        region.setExtractionUseCommand(true);
        region.setExtractionCommand("spawn %player%");
        
        // Ensure force OP config is effectively enabled
        lenient().when(config.getBoolean("extraction.force_op_commands", true)).thenReturn(true);
        
        org.bukkit.command.ConsoleCommandSender consoleMock = mock(org.bukkit.command.ConsoleCommandSender.class);
        mockedBukkit.when(Bukkit::getConsoleSender).thenReturn(consoleMock);
        
        task.handleButtonPress(player, region);
        
        // Simulate time passing to finish the extraction
        ExtractionTask.ExtractionSession session = task.getSessions().get(player.getUniqueId());
        try {
            java.lang.reflect.Field field = ExtractionTask.ExtractionSession.class.getDeclaredField("startTime");
            field.setAccessible(true);
            field.setLong(session, System.currentTimeMillis() - 500000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Mock block in front of player
        org.bukkit.block.Block block = mock(org.bukkit.block.Block.class);
        lenient().when(player.getTargetBlockExact(5)).thenReturn(block);
        Location cLoc3 = region.getConduitLocation();
        lenient().when(block.getLocation()).thenReturn(cLoc3);
        
        // Process tick (which completes the session and executes extraction)
        task.run();
        
        // Session should be removed
        assertEquals(0, task.getSessions().size());
        
        // Verify the server console dispatched the command replacing %player%
        mockedBukkit.verify(() -> Bukkit.dispatchCommand(consoleMock, "spawn Tester"));
        
        // Ensure player.performCommand is NOT called because the console proxies the request
        verify(player, never()).performCommand(anyString());
    }
}
