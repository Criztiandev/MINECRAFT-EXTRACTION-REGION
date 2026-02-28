package com.criztiandev.extractionregion.models;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class SavedRegionTest {

    @Test
    public void testGetNextResetTime_NullInterval() {
        SavedRegion region = new SavedRegion("test", "world", 0, 0, 0, 0);
        region.setResetIntervalMinutes(0); // 0 means no reset
        assertEquals(0, region.getNextResetTime(), "Reset time should be 0 if interval is 0");
    }

    @Test
    public void testGetNextResetTime_DynamicInterval() {
        SavedRegion region = new SavedRegion("test", "world", 0, 0, 0, 0);
        region.setResetIntervalMinutes(15); // Every 15 minutes

        ZoneId phZoneId = ZoneId.of("Asia/Manila");
        ZonedDateTime now = ZonedDateTime.now(phZoneId);
        int currentMinuteOfDay = now.getHour() * 60 + now.getMinute();

        // The expected next reset minute is the next multiple of 15
        int expectedNextMinuteOfDay = ((currentMinuteOfDay / 15) + 1) * 15;
        
        // Calculate expected time
        ZonedDateTime expectedResetTime = now.withHour(0).withMinute(0).withSecond(0).withNano(0).plusMinutes(expectedNextMinuteOfDay);

        // If the expected reset time is exactly now (which shouldn't happen with the formula above, but just in case),
        // or if it jumped to the next day, it's fine. 
        // We evaluate strictly:
        long actualResTimeMs = region.getNextResetTime();

        assertTrue(Math.abs(expectedResetTime.toInstant().toEpochMilli() - actualResTimeMs) <= 1500, 
            "The dynamic timestamp should match the expected 15-minute chunk boundary within a ~1.5s tolerance.");
    }

    @Test
    public void testGetNextResetTime_TwoHourInterval() {
        SavedRegion region = new SavedRegion("test", "world", 0, 0, 0, 0);
        region.setResetIntervalMinutes(120); // Every 2 hours
        
        ZoneId phZoneId = ZoneId.of("Asia/Manila");
        ZonedDateTime now = ZonedDateTime.now(phZoneId);
        int currentMinuteOfDay = now.getHour() * 60 + now.getMinute();

        int expectedNextMinuteOfDay = ((currentMinuteOfDay / 120) + 1) * 120;
        ZonedDateTime expectedResetTime = now.withHour(0).withMinute(0).withSecond(0).withNano(0).plusMinutes(expectedNextMinuteOfDay);
        
        long actualResTimeMs = region.getNextResetTime();

        assertTrue(Math.abs(expectedResetTime.toInstant().toEpochMilli() - actualResTimeMs) <= 1500, 
            "The dynamic timestamp should match the expected 2-hour (120 min) chunk boundary within a ~1.5s tolerance.");
    }

    @Test
    public void testEntryCooldown_DefaultNoCooldown() {
        SavedRegion region = new SavedRegion("test", "world", 0, 0, 0, 0);
        java.util.UUID playerId = java.util.UUID.randomUUID();
        
        assertFalse(region.isOnEntryCooldown(playerId), "Should not be on cooldown initially");
        assertEquals(0, region.getRemainingEntryCooldownTime(playerId), "Remaining time should be 0");
    }

    @Test
    public void testEntryCooldown_SetAndCheck() {
        SavedRegion region = new SavedRegion("test", "world", 0, 0, 0, 0);
        region.setEntryCooldownMinutes(5); // 5 minutes
        java.util.UUID playerId = java.util.UUID.randomUUID();
        
        region.setPlayerEntryCooldown(playerId);
        
        assertTrue(region.isOnEntryCooldown(playerId), "Player should be on cooldown after it is set");
        assertTrue(region.getRemainingEntryCooldownTime(playerId) > 0, "Remaining time should be greater than 0");
    }

    @Test
    public void testEntryCooldown_MultiplayerIsolation() {
        SavedRegion region = new SavedRegion("test", "world", 0, 0, 0, 0);
        region.setEntryCooldownMinutes(10);
        
        java.util.UUID playerA = java.util.UUID.randomUUID();
        java.util.UUID playerB = java.util.UUID.randomUUID();
        
        region.setPlayerEntryCooldown(playerA);
        
        assertTrue(region.isOnEntryCooldown(playerA), "Player A should be on cooldown");
        assertFalse(region.isOnEntryCooldown(playerB), "Player B should NOT be on cooldown");
    }
}
