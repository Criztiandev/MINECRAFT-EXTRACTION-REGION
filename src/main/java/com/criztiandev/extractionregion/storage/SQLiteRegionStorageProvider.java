package com.criztiandev.extractionregion.storage;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import com.criztiandev.extractionregion.models.SavedRegion;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SQLiteRegionStorageProvider implements RegionStorageProvider {

    private final ExtractionRegionPlugin plugin;
    private HikariDataSource dataSource;

    public SQLiteRegionStorageProvider(ExtractionRegionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, "regions.db");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setPoolName("ExtractionRegion-SQLite");
        
        // SQLite-specific optimizations
        config.setConnectionTestQuery("SELECT 1");
        config.setMaximumPoolSize(1); // SQLite handles single files best with 1 connection per pool

        dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            // Create Regions Table
            stmt.execute("CREATE TABLE IF NOT EXISTS regions (" +
                    "id TEXT PRIMARY KEY," +
                    "type TEXT NOT NULL DEFAULT 'CHEST_REPLENISH'," +
                    "world TEXT NOT NULL," +
                    "min_x INTEGER NOT NULL," +
                    "max_x INTEGER NOT NULL," +
                    "min_y INTEGER NOT NULL DEFAULT -64," +
                    "max_y INTEGER NOT NULL DEFAULT 320," +
                    "min_z INTEGER NOT NULL," +
                    "max_z INTEGER NOT NULL," +
                    "next_reset_time INTEGER NOT NULL DEFAULT 0," +
                    "reset_interval_minutes INTEGER NOT NULL DEFAULT 360," +
                    "conduit_world TEXT," +
                    "conduit_x INTEGER," +
                    "conduit_y INTEGER," +
                    "conduit_z INTEGER," +
                    "cooldown_sequence TEXT NOT NULL DEFAULT '10'," +
                    "cooldown_index INTEGER NOT NULL DEFAULT 0," +
                    "max_capacity INTEGER NOT NULL DEFAULT 5," +
                    "min_capacity INTEGER NOT NULL DEFAULT 1," +
                    "cooldown_end_time INTEGER NOT NULL DEFAULT 0," +
                    "mimic_enabled INTEGER NOT NULL DEFAULT 1," +
                    "mimic_chance INTEGER NOT NULL DEFAULT 5," +
                    "announcement_radius INTEGER NOT NULL DEFAULT 100," +
                    "drop_world TEXT," +
                    "drop_min_x INTEGER," +
                    "drop_max_x INTEGER," +
                    "drop_min_y INTEGER," +
                    "drop_max_y INTEGER," +
                    "drop_min_z INTEGER," +
                    "drop_max_z INTEGER," +
                    "slow_falling_seconds INTEGER NOT NULL DEFAULT 10," +
                    "blindness_seconds INTEGER NOT NULL DEFAULT 3," +
                    "ext_spawn_world TEXT," +
                    "ext_spawn_x REAL," +
                    "ext_spawn_y REAL," +
                    "ext_spawn_z REAL," +
                    "ext_spawn_yaw REAL," +
                    "ext_spawn_pitch REAL," +
                    "possible_durations TEXT NOT NULL DEFAULT '5'," +
                    "beam_color TEXT NOT NULL DEFAULT '#FF0000'," +
                    "alarm_sound TEXT NOT NULL DEFAULT 'BLOCK_BELL_RESONATE'," +
                    "hologram_offset_x REAL NOT NULL DEFAULT 0.5," +
                    "hologram_offset_y REAL NOT NULL DEFAULT 3.0," +
                    "hologram_offset_z REAL NOT NULL DEFAULT 0.5," +
                    "hologram_scale REAL NOT NULL DEFAULT 1.0" +
                    ");");
                    
            // Migration check: If table already existed without the new columns, add them.
            try {
                stmt.execute("ALTER TABLE regions ADD COLUMN next_reset_time INTEGER NOT NULL DEFAULT 0");
                plugin.getLogger().info("Migrated database: added next_reset_time to regions");
            } catch (SQLException ignored) {
                // Column likely already exists
            }
            try {
                stmt.execute("ALTER TABLE regions ADD COLUMN reset_interval_minutes INTEGER NOT NULL DEFAULT 360");
                plugin.getLogger().info("Migrated database: added reset_interval_minutes to regions");
            } catch (SQLException ignored) {
                // Column likely already exists
            }
            
            // Extraction regions migration
            String[] newColumns = {
                "min_y INTEGER NOT NULL DEFAULT -64",
                "max_y INTEGER NOT NULL DEFAULT 320",
                "type TEXT NOT NULL DEFAULT 'CHEST_REPLENISH'",
                "conduit_world TEXT",
                "conduit_x INTEGER",
                "conduit_y INTEGER",
                "conduit_z INTEGER",
                "cooldown_sequence TEXT NOT NULL DEFAULT '10'",
                "cooldown_index INTEGER NOT NULL DEFAULT 0",
                "max_capacity INTEGER NOT NULL DEFAULT 5",
                "min_capacity INTEGER NOT NULL DEFAULT 1",
                "cooldown_end_time INTEGER NOT NULL DEFAULT 0",
                "mimic_enabled INTEGER NOT NULL DEFAULT 1",
                "mimic_chance INTEGER NOT NULL DEFAULT 5",
                "announcement_radius INTEGER NOT NULL DEFAULT 100",
                "drop_world TEXT",
                "drop_min_x INTEGER",
                "drop_max_x INTEGER",
                "drop_min_y INTEGER",
                "drop_max_y INTEGER",
                "drop_min_z INTEGER",
                "drop_max_z INTEGER",
                "slow_falling_seconds INTEGER NOT NULL DEFAULT 10",
                "blindness_seconds INTEGER NOT NULL DEFAULT 3",
                "ext_spawn_world TEXT",
                "ext_spawn_x REAL",
                "ext_spawn_y REAL",
                "ext_spawn_z REAL",
                "ext_spawn_pitch REAL",
                "possible_durations TEXT NOT NULL DEFAULT '5'",
                "beam_color TEXT NOT NULL DEFAULT '#FF0000'",
                "alarm_sound TEXT NOT NULL DEFAULT 'BLOCK_BELL_RESONATE'",
                "hologram_offset_x REAL NOT NULL DEFAULT 0.5",
                "hologram_offset_y REAL NOT NULL DEFAULT 3.0",
                "hologram_offset_z REAL NOT NULL DEFAULT 0.5",
                "hologram_scale REAL NOT NULL DEFAULT 1.0",
                "bypass_cooldown INTEGER NOT NULL DEFAULT 0",
                "extraction_use_command INTEGER NOT NULL DEFAULT 0",
                "extraction_command TEXT NOT NULL DEFAULT 'spawn %player%'",
                "use_cooldown_command INTEGER NOT NULL DEFAULT 0",
                "cooldown_command TEXT NOT NULL DEFAULT 'spawn %player%'",
                "entry_fallback_command TEXT NOT NULL DEFAULT 'spawn %player%'",
                "entry_enabled INTEGER NOT NULL DEFAULT 1"
            };
            
            for (String colDef : newColumns) {
                try {
                    stmt.execute("ALTER TABLE regions ADD COLUMN " + colDef);
                    plugin.getLogger().info("Migrated database: added " + colDef.split(" ")[0] + " to regions");
                } catch (SQLException ignored) {}
            }

            // Tables `region_auto_spawns` and `region_specific_locations` are deprecated
            // We can optionally drop them here if the user wants clean DBs, but ignoring them is safer for downgrades.

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize SQLite database: " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public CompletableFuture<List<SavedRegion>> loadAllRegions() {
        return CompletableFuture.supplyAsync(() -> {
            List<SavedRegion> regions = new ArrayList<>();

            try (Connection conn = dataSource.getConnection()) {
                
                // 1. Load Core Regions
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM regions");
                     ResultSet rs = stmt.executeQuery()) {
                     
                    while (rs.next()) {
                        String id = rs.getString("id");
                        String world = rs.getString("world");
                        int minX = rs.getInt("min_x");
                        int maxX = rs.getInt("max_x");
                        int minY = rs.getInt("min_y");
                        int maxY = rs.getInt("max_y");
                        int minZ = rs.getInt("min_z");
                        int maxZ = rs.getInt("max_z");
                        long nextResetTime = rs.getLong("next_reset_time");
                        int resetIntervalMinutes = rs.getInt("reset_interval_minutes");

                        SavedRegion region = new SavedRegion(id, world, minX, maxX, minZ, maxZ);
                        region.setMinY(minY);
                        region.setMaxY(maxY);
                        region.setNextResetTime(nextResetTime);
                        region.setResetIntervalMinutes(resetIntervalMinutes);
                        
                        try {
                            region.setType(com.criztiandev.extractionregion.models.RegionType.valueOf(rs.getString("type")));
                            
                            String seqStr = rs.getString("cooldown_sequence");
                            if (seqStr == null) {
                                // Fallback to older column if exist, though SQLite usually handles default
                                try {
                                    int oldVal = rs.getInt("cooldown_minutes");
                                    region.setCooldownSequence(new java.util.ArrayList<>(java.util.Arrays.asList(oldVal)));
                                } catch (Exception ignored) {
                                    region.setCooldownSequence(new java.util.ArrayList<>(java.util.Arrays.asList(5, 10, 15, 25)));
                                }
                            } else {
                                java.util.List<Integer> seq = new java.util.ArrayList<>();
                                for (String part : seqStr.split(",")) {
                                    try { seq.add(Integer.parseInt(part.trim())); } catch (NumberFormatException ignored) {}
                                }
                                if (!seq.isEmpty()) region.setCooldownSequence(seq);
                            }
                            
                            try { region.setCooldownIndex(rs.getInt("cooldown_index")); } catch (Exception ignored) {}
                            
                            region.setMaxCapacity(rs.getInt("max_capacity"));
                            region.setMinCapacity(rs.getInt("min_capacity"));
                            region.setCooldownEndTime(rs.getLong("cooldown_end_time"));
                            region.setMimicEnabled(rs.getInt("mimic_enabled") == 1);
                            try { region.setMimicChance(rs.getInt("mimic_chance")); } catch (Exception ignored) {}
                            try { region.setBypassCooldown(rs.getInt("bypass_cooldown") == 1); } catch (Exception ignored) {}
                            try { region.setAnnouncementRadius(rs.getInt("announcement_radius")); } catch (Exception ignored) {}
                            try {
                                String bColor = rs.getString("beam_color");
                                if (bColor != null) region.setBeamColor(bColor);
                            } catch (Exception ignored) {}
                            try {
                                String aSound = rs.getString("alarm_sound");
                                if (aSound != null) region.setAlarmSound(aSound);
                            } catch (Exception ignored) {}
                            
                            try {
                                region.setHologramOffsetX(rs.getDouble("hologram_offset_x"));
                                region.setHologramOffsetY(rs.getDouble("hologram_offset_y"));
                                region.setHologramOffsetZ(rs.getDouble("hologram_offset_z"));
                                region.setHologramScale(rs.getDouble("hologram_scale"));
                            } catch (Exception ignored) {}
                            
                            try { region.setUseCooldownCommand(rs.getInt("use_cooldown_command") == 1); } catch (Exception ignored) {}
                            try {
                                String cmd = rs.getString("cooldown_command");
                                if (cmd != null) region.setCooldownCommand(cmd);
                            } catch (Exception ignored) {}
                            try {
                                String efCmd = rs.getString("entry_fallback_command");
                                if (efCmd != null) region.setEntryFallbackCommand(efCmd);
                            } catch (Exception ignored) {}
                            try { region.setEntryEnabled(rs.getInt("entry_enabled") == 1); } catch (Exception ignored) {}
                            
                            String conduitWorld = rs.getString("conduit_world");
                            if (conduitWorld != null) {
                                org.bukkit.World bWorld = org.bukkit.Bukkit.getWorld(conduitWorld);
                                if (bWorld != null) {
                                    region.setConduitLocation(new org.bukkit.Location(bWorld, rs.getInt("conduit_x"), rs.getInt("conduit_y"), rs.getInt("conduit_z")));
                                }
                            }
                            
                            region.setDropWorld(rs.getString("drop_world"));
                            region.setDropMinX(rs.getInt("drop_min_x"));
                            region.setDropMaxX(rs.getInt("drop_max_x"));
                            region.setDropMinY(rs.getInt("drop_min_y"));
                            region.setDropMaxY(rs.getInt("drop_max_y"));
                            region.setDropMinZ(rs.getInt("drop_min_z"));
                            region.setDropMaxZ(rs.getInt("drop_max_z"));
                            region.setSlowFallingSeconds(rs.getInt("slow_falling_seconds"));
                            region.setBlindnessSeconds(rs.getInt("blindness_seconds"));
                            
                            String extSpawnWorld = rs.getString("ext_spawn_world");
                            if (extSpawnWorld != null) {
                                region.setExtractionSpawnWorld(extSpawnWorld);
                                region.setExtractionSpawnX(rs.getDouble("ext_spawn_x"));
                                region.setExtractionSpawnY(rs.getDouble("ext_spawn_y"));
                                region.setExtractionSpawnZ(rs.getDouble("ext_spawn_z"));
                                region.setExtractionSpawnYaw(rs.getFloat("ext_spawn_yaw"));
                                region.setExtractionSpawnPitch(rs.getFloat("ext_spawn_pitch"));
                            }
                            
                            try { region.setExtractionUseCommand(rs.getInt("extraction_use_command") == 1); } catch (Exception ignored) {}
                            try {
                                String eCmd = rs.getString("extraction_command");
                                if (eCmd != null) region.setExtractionCommand(eCmd);
                            } catch (Exception ignored) {}
                            
                            
                            String dSeqStr = null;
                            try { dSeqStr = rs.getString("possible_durations"); } catch (Exception ignored) {}
                            
                            if (dSeqStr == null) {
                                // Fallback
                                try {
                                    int oldDur = rs.getInt("ext_duration_seconds");
                                    region.setPossibleDurations(new java.util.ArrayList<>(java.util.Arrays.asList(oldDur)));
                                } catch (Exception ignored) {}
                            } else {
                                java.util.List<Integer> dSeq = new java.util.ArrayList<>();
                                for (String part : dSeqStr.split(",")) {
                                    try { dSeq.add(Integer.parseInt(part.trim())); } catch (NumberFormatException ignored) {}
                                }
                                if (!dSeq.isEmpty()) region.setPossibleDurations(dSeq);
                            }
                            
                        } catch (Exception e) {
                            plugin.getLogger().warning("Could not load extraction fields for region " + id + ": " + e.getMessage());
                        }
                        
                        regions.add(region);
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load regions from SQLite: " + e.getMessage());
            }
            return regions;
        });
    }

    @Override
    public CompletableFuture<Void> saveRegion(SavedRegion region) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false); // Start transaction

                try {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO regions (id, type, world, min_x, max_x, min_y, max_y, min_z, max_z, next_reset_time, reset_interval_minutes, conduit_world, conduit_x, conduit_y, conduit_z, cooldown_sequence, cooldown_index, max_capacity, min_capacity, cooldown_end_time, mimic_enabled, mimic_chance, announcement_radius, drop_world, drop_min_x, drop_max_x, drop_min_y, drop_max_y, drop_min_z, drop_max_z, slow_falling_seconds, blindness_seconds, ext_spawn_world, ext_spawn_x, ext_spawn_y, ext_spawn_z, ext_spawn_yaw, ext_spawn_pitch, possible_durations, beam_color, alarm_sound, hologram_offset_x, hologram_offset_y, hologram_offset_z, hologram_scale, bypass_cooldown, extraction_use_command, extraction_command, use_cooldown_command, cooldown_command, entry_fallback_command, entry_enabled) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON CONFLICT(id) DO UPDATE SET " +
                            "type=excluded.type, world=excluded.world, min_x=excluded.min_x, max_x=excluded.max_x, " +
                            "min_y=excluded.min_y, max_y=excluded.max_y, " +
                            "min_z=excluded.min_z, max_z=excluded.max_z, " +
                            "next_reset_time=excluded.next_reset_time, reset_interval_minutes=excluded.reset_interval_minutes, " +
                            "conduit_world=excluded.conduit_world, conduit_x=excluded.conduit_x, conduit_y=excluded.conduit_y, conduit_z=excluded.conduit_z, " +
                            "cooldown_sequence=excluded.cooldown_sequence, cooldown_index=excluded.cooldown_index, max_capacity=excluded.max_capacity, min_capacity=excluded.min_capacity, " +
                            "cooldown_end_time=excluded.cooldown_end_time, mimic_enabled=excluded.mimic_enabled, mimic_chance=excluded.mimic_chance, " +
                            "announcement_radius=excluded.announcement_radius, " +
                            "drop_world=excluded.drop_world, drop_min_x=excluded.drop_min_x, drop_max_x=excluded.drop_max_x, " +
                            "drop_min_y=excluded.drop_min_y, drop_max_y=excluded.drop_max_y, drop_min_z=excluded.drop_min_z, drop_max_z=excluded.drop_max_z, " +
                            "slow_falling_seconds=excluded.slow_falling_seconds, blindness_seconds=excluded.blindness_seconds, " +
                            "ext_spawn_world=excluded.ext_spawn_world, ext_spawn_x=excluded.ext_spawn_x, ext_spawn_y=excluded.ext_spawn_y, " +
                            "ext_spawn_z=excluded.ext_spawn_z, ext_spawn_yaw=excluded.ext_spawn_yaw, ext_spawn_pitch=excluded.ext_spawn_pitch, " +
                            "possible_durations=excluded.possible_durations, beam_color=excluded.beam_color, alarm_sound=excluded.alarm_sound, " +
                            "hologram_offset_x=excluded.hologram_offset_x, hologram_offset_y=excluded.hologram_offset_y, hologram_offset_z=excluded.hologram_offset_z, hologram_scale=excluded.hologram_scale, bypass_cooldown=excluded.bypass_cooldown, " +
                            "extraction_use_command=excluded.extraction_use_command, extraction_command=excluded.extraction_command, " +
                            "use_cooldown_command=excluded.use_cooldown_command, cooldown_command=excluded.cooldown_command, " +
                            "entry_fallback_command=excluded.entry_fallback_command, entry_enabled=excluded.entry_enabled")) {
                        
                        stmt.setString(1, region.getId());
                        stmt.setString(2, region.getType().name());
                        stmt.setString(3, region.getWorld());
                        stmt.setInt(4, region.getMinX());
                        stmt.setInt(5, region.getMaxX());
                        stmt.setInt(6, region.getMinY());
                        stmt.setInt(7, region.getMaxY());
                        stmt.setInt(8, region.getMinZ());
                        stmt.setInt(9, region.getMaxZ());
                        stmt.setLong(10, region.getNextResetTime());
                        stmt.setInt(11, region.getResetIntervalMinutes());
                        
                        if (region.getConduitLocation() != null && region.getConduitLocation().getWorld() != null) {
                            stmt.setString(12, region.getConduitLocation().getWorld().getName());
                            stmt.setInt(13, region.getConduitLocation().getBlockX());
                            stmt.setInt(14, region.getConduitLocation().getBlockY());
                            stmt.setInt(15, region.getConduitLocation().getBlockZ());
                        } else {
                            stmt.setNull(12, java.sql.Types.VARCHAR);
                            stmt.setNull(13, java.sql.Types.INTEGER);
                            stmt.setNull(14, java.sql.Types.INTEGER);
                            stmt.setNull(15, java.sql.Types.INTEGER);
                        }
                        
                        java.util.List<Integer> seq = region.getCooldownSequence();
                        StringBuilder seqBuilder = new StringBuilder();
                        for (int i = 0; i < seq.size(); i++) {
                            seqBuilder.append(seq.get(i));
                            if (i < seq.size() - 1) seqBuilder.append(",");
                        }
                        stmt.setString(16, seqBuilder.toString());
                        stmt.setInt(17, region.getCooldownIndex());
                        stmt.setInt(18, region.getMaxCapacity());
                        stmt.setInt(19, region.getMinCapacity());
                        stmt.setLong(20, region.getCooldownEndTime());
                        stmt.setInt(21, region.isMimicEnabled() ? 1 : 0);
                        stmt.setInt(22, region.getMimicChance());
                        stmt.setInt(23, region.getAnnouncementRadius());
                        
                        stmt.setString(24, region.getDropWorld());
                        stmt.setInt(25, region.getDropMinX());
                        stmt.setInt(26, region.getDropMaxX());
                        stmt.setInt(27, region.getDropMinY());
                        stmt.setInt(28, region.getDropMaxY());
                        stmt.setInt(29, region.getDropMinZ());
                        stmt.setInt(30, region.getDropMaxZ());
                        stmt.setInt(31, region.getSlowFallingSeconds());
                        stmt.setInt(32, region.getBlindnessSeconds());
                        
                        if (region.getExtractionSpawnWorld() != null) {
                            stmt.setString(33, region.getExtractionSpawnWorld());
                            stmt.setDouble(34, region.getExtractionSpawnX());
                            stmt.setDouble(35, region.getExtractionSpawnY());
                            stmt.setDouble(36, region.getExtractionSpawnZ());
                            stmt.setDouble(37, region.getExtractionSpawnYaw());
                            stmt.setDouble(38, region.getExtractionSpawnPitch());
                        } else {
                            stmt.setNull(33, java.sql.Types.VARCHAR);
                            stmt.setNull(34, java.sql.Types.REAL);
                            stmt.setNull(35, java.sql.Types.REAL);
                            stmt.setNull(36, java.sql.Types.REAL);
                            stmt.setNull(37, java.sql.Types.REAL);
                            stmt.setNull(38, java.sql.Types.REAL);
                        }
                        
                        java.util.List<Integer> dSeq = region.getPossibleDurations();
                        StringBuilder dSeqBuilder = new StringBuilder();
                        for (int i = 0; i < dSeq.size(); i++) {
                            dSeqBuilder.append(dSeq.get(i));
                            if (i < dSeq.size() - 1) dSeqBuilder.append(",");
                        }
                        stmt.setString(39, dSeqBuilder.toString());
                        stmt.setString(40, region.getBeamColor());
                        stmt.setString(41, region.getAlarmSound());
                        stmt.setDouble(42, region.getHologramOffsetX());
                        stmt.setDouble(43, region.getHologramOffsetY());
                        stmt.setDouble(44, region.getHologramOffsetZ());
                        stmt.setDouble(45, region.getHologramScale());
                        stmt.setInt(46, region.isBypassCooldown() ? 1 : 0);
                        stmt.setInt(47, region.isExtractionUseCommand() ? 1 : 0);
                        stmt.setString(48, region.getExtractionCommand());
                        stmt.setInt(49, region.isUseCooldownCommand() ? 1 : 0);
                        stmt.setString(50, region.getCooldownCommand());
                        stmt.setString(51, region.getEntryFallbackCommand());
                        stmt.setInt(52, region.isEntryEnabled() ? 1 : 0);
                        
                        stmt.executeUpdate();
                    }

                    // Specific tables no longer managed
                    conn.commit(); // Commit transaction
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save region " + region.getId() + " to SQLite: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteRegion(String id) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false); // Start transaction
                
                try {
                    // With SQLite ON DELETE CASCADE might not always be enabled by default depending on PRAGMA,
                    // so we explicitly delete children just in case.
                    
                    try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM regions WHERE id = ?")) {
                        stmt.setString(1, id);
                        stmt.executeUpdate();
                    }
                    
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete region " + id + " from SQLite: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> renameRegion(String oldId, String newId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Update main region table
                    try (PreparedStatement stmt = conn.prepareStatement("UPDATE regions SET id = ? WHERE id = ?")) {
                        stmt.setString(1, newId);
                        stmt.setString(2, oldId);
                        int updated = stmt.executeUpdate();
                        if (updated == 0) {
                            conn.rollback();
                            return false; // Region didn't exist
                        }
                    }
                    
                    conn.commit();
                    return true;
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to rename region from " + oldId + " to " + newId + " in SQLite: " + e.getMessage());
                return false;
            }
        });
    }
}
