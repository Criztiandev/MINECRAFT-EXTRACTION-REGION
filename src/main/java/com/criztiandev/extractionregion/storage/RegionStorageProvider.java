package com.criztiandev.extractionregion.storage;

import com.criztiandev.extractionregion.models.SavedRegion;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RegionStorageProvider {
    void initialize();
    void shutdown();

    CompletableFuture<List<SavedRegion>> loadAllRegions();
    CompletableFuture<Void> saveRegion(SavedRegion region);
    CompletableFuture<Void> deleteRegion(String id);
}
