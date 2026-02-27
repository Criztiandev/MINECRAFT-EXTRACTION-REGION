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
                    "min_z INTEGER NOT NULL," +
                    "max_z INTEGER NOT NULL," +
                    "next_reset_time INTEGER NOT NULL DEFAULT 0," +
                    "reset_interval_minutes INTEGER NOT NULL DEFAULT 360," +
                    "conduit_world TEXT," +
                    "conduit_x INTEGER," +
                    "conduit_y INTEGER," +
                    "conduit_z INTEGER," +
                    "cooldown_minutes INTEGER NOT NULL DEFAULT 10," +
                    "max_capacity INTEGER NOT NULL DEFAULT 5," +
                    "min_capacity INTEGER NOT NULL DEFAULT 1," +
                    "cooldown_end_time INTEGER NOT NULL DEFAULT 0," +
                    "mimic_enabled INTEGER NOT NULL DEFAULT 1," +
                    "drop_world TEXT," +
                    "drop_min_x INTEGER," +
                    "drop_max_x INTEGER," +
                    "drop_min_y INTEGER," +
                    "drop_max_y INTEGER," +
                    "drop_min_z INTEGER," +
                    "drop_max_z INTEGER," +
                    "slow_falling_seconds INTEGER NOT NULL DEFAULT 10," +
                    "blindness_seconds INTEGER NOT NULL DEFAULT 3" +
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
                "type TEXT NOT NULL DEFAULT 'CHEST_REPLENISH'",
                "conduit_world TEXT",
                "conduit_x INTEGER",
                "conduit_y INTEGER",
                "conduit_z INTEGER",
                "cooldown_minutes INTEGER NOT NULL DEFAULT 10",
                "max_capacity INTEGER NOT NULL DEFAULT 5",
                "min_capacity INTEGER NOT NULL DEFAULT 1",
                "cooldown_end_time INTEGER NOT NULL DEFAULT 0",
                "mimic_enabled INTEGER NOT NULL DEFAULT 1",
                "drop_world TEXT",
                "drop_min_x INTEGER",
                "drop_max_x INTEGER",
                "drop_min_y INTEGER",
                "drop_max_y INTEGER",
                "drop_min_z INTEGER",
                "drop_max_z INTEGER",
                "slow_falling_seconds INTEGER NOT NULL DEFAULT 10",
                "blindness_seconds INTEGER NOT NULL DEFAULT 3"
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
                        int minZ = rs.getInt("min_z");
                        int maxZ = rs.getInt("max_z");
                        long nextResetTime = rs.getLong("next_reset_time");
                        int resetIntervalMinutes = rs.getInt("reset_interval_minutes");

                        SavedRegion region = new SavedRegion(id, world, minX, maxX, minZ, maxZ);
                        region.setNextResetTime(nextResetTime);
                        region.setResetIntervalMinutes(resetIntervalMinutes);
                        
                        try {
                            region.setType(com.criztiandev.extractionregion.models.RegionType.valueOf(rs.getString("type")));
                            region.setCooldownMinutes(rs.getInt("cooldown_minutes"));
                            region.setMaxCapacity(rs.getInt("max_capacity"));
                            region.setMinCapacity(rs.getInt("min_capacity"));
                            region.setCooldownEndTime(rs.getLong("cooldown_end_time"));
                            region.setMimicEnabled(rs.getInt("mimic_enabled") == 1);
                            
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
                    // 1. Upsert Region
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO regions (id, type, world, min_x, max_x, min_z, max_z, next_reset_time, reset_interval_minutes, conduit_world, conduit_x, conduit_y, conduit_z, cooldown_minutes, max_capacity, min_capacity, cooldown_end_time, mimic_enabled, drop_world, drop_min_x, drop_max_x, drop_min_y, drop_max_y, drop_min_z, drop_max_z, slow_falling_seconds, blindness_seconds) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON CONFLICT(id) DO UPDATE SET " +
                            "type=excluded.type, world=excluded.world, min_x=excluded.min_x, max_x=excluded.max_x, " +
                            "min_z=excluded.min_z, max_z=excluded.max_z, " +
                            "next_reset_time=excluded.next_reset_time, reset_interval_minutes=excluded.reset_interval_minutes, " +
                            "conduit_world=excluded.conduit_world, conduit_x=excluded.conduit_x, conduit_y=excluded.conduit_y, conduit_z=excluded.conduit_z, " +
                            "cooldown_minutes=excluded.cooldown_minutes, max_capacity=excluded.max_capacity, min_capacity=excluded.min_capacity, " +
                            "cooldown_end_time=excluded.cooldown_end_time, mimic_enabled=excluded.mimic_enabled, " +
                            "drop_world=excluded.drop_world, drop_min_x=excluded.drop_min_x, drop_max_x=excluded.drop_max_x, " +
                            "drop_min_y=excluded.drop_min_y, drop_max_y=excluded.drop_max_y, drop_min_z=excluded.drop_min_z, drop_max_z=excluded.drop_max_z, " +
                            "slow_falling_seconds=excluded.slow_falling_seconds, blindness_seconds=excluded.blindness_seconds")) {
                        
                        stmt.setString(1, region.getId());
                        stmt.setString(2, region.getType().name());
                        stmt.setString(3, region.getWorld());
                        stmt.setInt(4, region.getMinX());
                        stmt.setInt(5, region.getMaxX());
                        stmt.setInt(6, region.getMinZ());
                        stmt.setInt(7, region.getMaxZ());
                        stmt.setLong(8, region.getNextResetTime());
                        stmt.setInt(9, region.getResetIntervalMinutes());
                        
                        if (region.getConduitLocation() != null && region.getConduitLocation().getWorld() != null) {
                            stmt.setString(10, region.getConduitLocation().getWorld().getName());
                            stmt.setInt(11, region.getConduitLocation().getBlockX());
                            stmt.setInt(12, region.getConduitLocation().getBlockY());
                            stmt.setInt(13, region.getConduitLocation().getBlockZ());
                        } else {
                            stmt.setNull(10, java.sql.Types.VARCHAR);
                            stmt.setNull(11, java.sql.Types.INTEGER);
                            stmt.setNull(12, java.sql.Types.INTEGER);
                            stmt.setNull(13, java.sql.Types.INTEGER);
                        }
                        
                        stmt.setInt(14, region.getCooldownMinutes());
                        stmt.setInt(15, region.getMaxCapacity());
                        stmt.setInt(16, region.getMinCapacity());
                        stmt.setLong(17, region.getCooldownEndTime());
                        stmt.setInt(18, region.isMimicEnabled() ? 1 : 0);
                        
                        stmt.setString(19, region.getDropWorld());
                        stmt.setInt(20, region.getDropMinX());
                        stmt.setInt(21, region.getDropMaxX());
                        stmt.setInt(22, region.getDropMinY());
                        stmt.setInt(23, region.getDropMaxY());
                        stmt.setInt(24, region.getDropMinZ());
                        stmt.setInt(25, region.getDropMaxZ());
                        stmt.setInt(26, region.getSlowFallingSeconds());
                        stmt.setInt(27, region.getBlindnessSeconds());
                        
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
