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
                    "spawn_mode TEXT NOT NULL DEFAULT 'RANDOM'" +
                    ");");

            // Create Auto Spawns Table
            stmt.execute("CREATE TABLE IF NOT EXISTS region_auto_spawns (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "region_id TEXT NOT NULL," +
                    "chest_type TEXT NOT NULL," +
                    "amount INTEGER NOT NULL," +
                    "FOREIGN KEY(region_id) REFERENCES regions(id) ON DELETE CASCADE" +
                    ");");

            // Create Specific Locations Table
            stmt.execute("CREATE TABLE IF NOT EXISTS region_specific_locations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "region_id TEXT NOT NULL," +
                    "coordinates TEXT NOT NULL," +
                    "chest_type TEXT NOT NULL," +
                    "FOREIGN KEY(region_id) REFERENCES regions(id) ON DELETE CASCADE" +
                    ");");

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
                        SavedRegion.SpawnMode spawnMode = SavedRegion.SpawnMode.valueOf(rs.getString("spawn_mode"));

                        SavedRegion region = new SavedRegion(id, world, minX, maxX, minZ, maxZ);
                        region.setSpawnMode(spawnMode);
                        regions.add(region);
                    }
                }

                // 2. Load Auto Spawns for each region
                for (SavedRegion region : regions) {
                    try (PreparedStatement stmt = conn.prepareStatement("SELECT chest_type, amount FROM region_auto_spawns WHERE region_id = ?")) {
                        stmt.setString(1, region.getId());
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                region.setAutoSpawn(rs.getString("chest_type"), rs.getInt("amount"));
                            }
                        }
                    }

                    // 3. Load Specific Locations for each region
                    try (PreparedStatement stmt = conn.prepareStatement("SELECT coordinates, chest_type FROM region_specific_locations WHERE region_id = ?")) {
                        stmt.setString(1, region.getId());
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                region.getSpecificLocations().put(rs.getString("coordinates"), rs.getString("chest_type"));
                            }
                        }
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
                            "INSERT INTO regions (id, world, min_x, max_x, min_z, max_z, spawn_mode) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                            "ON CONFLICT(id) DO UPDATE SET " +
                            "world=excluded.world, min_x=excluded.min_x, max_x=excluded.max_x, " +
                            "min_z=excluded.min_z, max_z=excluded.max_z, spawn_mode=excluded.spawn_mode")) {
                        
                        stmt.setString(1, region.getId());
                        stmt.setString(2, region.getWorld());
                        stmt.setInt(3, region.getMinX());
                        stmt.setInt(4, region.getMaxX());
                        stmt.setInt(5, region.getMinZ());
                        stmt.setInt(6, region.getMaxZ());
                        stmt.setString(7, region.getSpawnMode().name());
                        stmt.executeUpdate();
                    }

                    // 2. Replace Auto Spawns (Delete then Insert)
                    try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM region_auto_spawns WHERE region_id = ?")) {
                        stmt.setString(1, region.getId());
                        stmt.executeUpdate();
                    }
                    if (!region.getAutoSpawns().isEmpty()) {
                        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO region_auto_spawns (region_id, chest_type, amount) VALUES (?, ?, ?)")) {
                            for (Map.Entry<String, Integer> entry : region.getAutoSpawns().entrySet()) {
                                stmt.setString(1, region.getId());
                                stmt.setString(2, entry.getKey());
                                stmt.setInt(3, entry.getValue());
                                stmt.addBatch();
                            }
                            stmt.executeBatch();
                        }
                    }

                    // 3. Replace Specific Locations (Delete then Insert)
                    try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM region_specific_locations WHERE region_id = ?")) {
                        stmt.setString(1, region.getId());
                        stmt.executeUpdate();
                    }
                    if (!region.getSpecificLocations().isEmpty()) {
                        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO region_specific_locations (region_id, coordinates, chest_type) VALUES (?, ?, ?)")) {
                            for (Map.Entry<String, String> entry : region.getSpecificLocations().entrySet()) {
                                stmt.setString(1, region.getId());
                                stmt.setString(2, entry.getKey());
                                stmt.setString(3, entry.getValue());
                                stmt.addBatch();
                            }
                            stmt.executeBatch();
                        }
                    }

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
                    
                    try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM region_auto_spawns WHERE region_id = ?")) {
                        stmt.setString(1, id);
                        stmt.executeUpdate();
                    }
                    
                    try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM region_specific_locations WHERE region_id = ?")) {
                        stmt.setString(1, id);
                        stmt.executeUpdate();
                    }
                    
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
}
