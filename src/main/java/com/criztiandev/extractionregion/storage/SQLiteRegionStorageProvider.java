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
                    "world TEXT NOT NULL," +
                    "min_x INTEGER NOT NULL," +
                    "max_x INTEGER NOT NULL," +
                    "min_z INTEGER NOT NULL," +
                    "max_z INTEGER NOT NULL," +
                    "next_reset_time INTEGER NOT NULL DEFAULT 0," +
                    "reset_interval_minutes INTEGER NOT NULL DEFAULT 360" +
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
                            "INSERT INTO regions (id, world, min_x, max_x, min_z, max_z, next_reset_time, reset_interval_minutes) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON CONFLICT(id) DO UPDATE SET " +
                            "world=excluded.world, min_x=excluded.min_x, max_x=excluded.max_x, " +
                            "min_z=excluded.min_z, max_z=excluded.max_z, " +
                            "next_reset_time=excluded.next_reset_time, reset_interval_minutes=excluded.reset_interval_minutes")) {
                        
                        stmt.setString(1, region.getId());
                        stmt.setString(2, region.getWorld());
                        stmt.setInt(3, region.getMinX());
                        stmt.setInt(4, region.getMaxX());
                        stmt.setInt(5, region.getMinZ());
                        stmt.setInt(6, region.getMaxZ());
                        stmt.setLong(7, region.getNextResetTime());
                        stmt.setInt(8, region.getResetIntervalMinutes());
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
