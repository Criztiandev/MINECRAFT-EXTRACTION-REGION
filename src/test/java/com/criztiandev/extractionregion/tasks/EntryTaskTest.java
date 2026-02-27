package com.criztiandev.extractionregion.tasks;

import com.criztiandev.extractionregion.ExtractionRegionPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class EntryTaskTest {

    @Mock
    private ExtractionRegionPlugin pluginMock;

    private EntryTask entryTask;

    @BeforeEach
    public void setup() {
        entryTask = new EntryTask(pluginMock);
    }

    @Test
    public void testCalculateDropCoordinates_SameMinMax() {
        int x = 10, y = 64, z = -10;
        double[] coords = entryTask.calculateDropCoordinates(x, x, y, y, z, z);

        assertEquals(10.5, coords[0], "X should center precisely");
        assertEquals(64.0, coords[1], "Y should remain the flat block level");
        assertEquals(-9.5, coords[2], "Z should center precisely (-10 + 0.5)");
    }

    @Test
    public void testCalculateDropCoordinates_PositiveBounds() {
        int minX = 10, maxX = 20;
        int minY = 60, maxY = 70;
        int minZ = 100, maxZ = 110;

        for (int i = 0; i < 1000; i++) {
            double[] coords = entryTask.calculateDropCoordinates(minX, maxX, minY, maxY, minZ, maxZ);

            assertTrue(coords[0] >= 10.5 && coords[0] <= 20.5, "X " + coords[0] + " out of bounds");
            assertTrue(coords[1] >= 60.0 && coords[1] <= 70.0, "Y " + coords[1] + " out of bounds");
            assertTrue(coords[2] >= 100.5 && coords[2] <= 110.5, "Z " + coords[2] + " out of bounds");
            
            // Decimal verifications
            assertTrue(coords[0] % 1 == 0.5, "X should end in .5");
            assertTrue(coords[1] % 1 == 0.0, "Y should end in .0");
            assertTrue(coords[2] % 1 == 0.5, "Z should end in .5");
        }
    }

    @Test
    public void testCalculateDropCoordinates_NegativeBounds() {
        int minX = -50, maxX = -40;
        int minY = -10, maxY = 0;
        int minZ = -100, maxZ = -90;

        for (int i = 0; i < 1000; i++) {
            double[] coords = entryTask.calculateDropCoordinates(minX, maxX, minY, maxY, minZ, maxZ);

            assertTrue(coords[0] >= -49.5 && coords[0] <= -39.5, "X " + coords[0] + " out of bounds");
            assertTrue(coords[1] >= -10.0 && coords[1] <= 0.0, "Y " + coords[1] + " out of bounds");
            assertTrue(coords[2] >= -99.5 && coords[2] <= -89.5, "Z " + coords[2] + " out of bounds");
        }
    }

    @Test
    public void testCalculateDropCoordinates_InvertedBoundsAutoFix() {
        int minX = 20, maxX = 10;
        int minY = 70, maxY = 60;
        int minZ = 200, maxZ = 100;

        for (int i = 0; i < 1000; i++) {
            double[] coords = entryTask.calculateDropCoordinates(minX, maxX, minY, maxY, minZ, maxZ);
            assertTrue(coords[0] >= 10.5 && coords[0] <= 20.5, "X Auto-Fix Failed");
            assertTrue(coords[1] >= 60.0 && coords[1] <= 70.0, "Y Auto-Fix Failed");
            assertTrue(coords[2] >= 100.5 && coords[2] <= 200.5, "Z Auto-Fix Failed");
        }
    }

    @Test
    public void testCalculateDropCoordinates_MassiveConcurrency() throws InterruptedException {
        int minX = -1000, maxX = 1000;
        int minY = 60, maxY = 100;
        int minZ = -1000, maxZ = 1000;

        int threadCount = 50;
        int iterationsPerThread = 1000;
        
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.List<Throwable> exceptions = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int t = 0; t < threadCount; t++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        // 50 threads blasting 1000 drops simultaneously using ThreadLocalRandom
                        double[] coords = entryTask.calculateDropCoordinates(minX, maxX, minY, maxY, minZ, maxZ);
                        
                        assertTrue(coords[0] >= -999.5 && coords[0] <= 1000.5);
                        assertTrue(coords[1] >= 60.0 && coords[1] <= 100.0);
                        assertTrue(coords[2] >= -999.5 && coords[2] <= 1000.5);
                    }
                } catch (Throwable e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        assertTrue(exceptions.isEmpty(), "Massive concurrency test failed with exceptions: " + exceptions);
    }
}
