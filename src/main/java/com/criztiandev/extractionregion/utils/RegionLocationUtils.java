package com.criztiandev.extractionregion.utils;

import com.criztiandev.extractionchest.utils.LocationUtils;
import com.criztiandev.extractionregion.models.RegionSelection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Random;

public class RegionLocationUtils {

    private static final Random random = new Random();

    public static Location getRandomSafeLocation(RegionSelection region, int maxAttempts) {
        World world = Bukkit.getWorld(region.getWorldName());
        if (world == null) return null;

        int minX = region.getMinX();
        int maxX = region.getMaxX();
        int minZ = region.getMinZ();
        int maxZ = region.getMaxZ();

        for (int i = 0; i < maxAttempts; i++) {
            int x = minX + random.nextInt(maxX - minX + 1);
            int z = minZ + random.nextInt(maxZ - minZ + 1);

            Block highestBlock = LocationUtils.getHighestSolidBlock(world, x, z);
            if (highestBlock != null && LocationUtils.isSafe(highestBlock)) {
                return highestBlock.getLocation().add(0, 1, 0);
            }
        }
        return null;
    }
}
