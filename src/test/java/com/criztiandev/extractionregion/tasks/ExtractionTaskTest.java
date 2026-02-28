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

    private ExtractionTask task;
    private SavedRegion region;
    private MockedStatic<Bukkit> mockedBukkit;
    private Location playerLoc;

    @BeforeEach
    public void setup() {
        task = new ExtractionTask(plugin);
        
        region = new SavedRegion("test_extract", "world", 0, 10, 0, 10);
        region.setType(RegionType.EXTRACTION);
        
        playerLoc = new Location(world, 5, 64, 5);
        lenient().when(player.getLocation()).thenReturn(playerLoc);
        lenient().when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().when(player.getName()).thenReturn("Tester");
        lenient().when(world.getName()).thenReturn("world");
        
        Player.Spigot spigotMock = mock(Player.Spigot.class);
        lenient().when(player.spigot()).thenReturn(spigotMock);
        
        region.setConduitLocation(new Location(world, 5, 64, 5));

        lenient().when(plugin.getConfig()).thenReturn(config);
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(config.getInt(anyString(), anyInt())).thenAnswer(invocation -> invocation.getArgument(1));

        mockedBukkit = mockStatic(Bukkit.class);
        mockedBukkit.when(Bukkit::getOnlinePlayers).thenReturn(Collections.singletonList(player));
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
}
