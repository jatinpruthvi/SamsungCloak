package com.samsungcloak.hardware;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Storage I/O Degradation Hook for Samsung Galaxy A12 (SM-A125U)
 * 
 * Simulates the slowing of app-load times as eMMC storage fills up
 * or undergoes fragmentation over a 30-day period.
 * 
 * Storage Specs: eMMC 5.1, 32GB/64GB variants
 */
public class StorageIODegradationHook {
    
    // SM-A125U Storage Constants
    private static final double STORAGE_CAPACITY_GB = 32.0; // Base model
    private static final double BASE_READ_SPEED_MBPS = 250.0; // eMMC 5.1 sequential read
    private static final double BASE_WRITE_SPEED_MBPS = 125.0; // eMMC 5.1 sequential write
    private static final double BASE_RANDOM_READ_IOPS = 4000.0; // 4K random read
    private static final double BASE_RANDOM_WRITE_IOPS = 2000.0; // 4K random write
    
    // Fragmentation and fill level degradation factors
    private static final double FRAGMENTATION_RATE_PER_DAY = 0.003; // 0.3% per day
    private static final double FILL_LEVEL_DEGRADATION_THRESHOLD = 0.75; // 75% full
    private static final double CRITICAL_FILL_LEVEL = 0.90; // 90% full
    
    // Wear leveling and block degradation
    private static final double WEAR_LEVELING_FACTOR = 0.0001; // Per write cycle
    private static final double BAD_BLOCK_RATE = 0.00001; // Per 1000 writes
    
    private final Random random;
    private double usedStorageGB;
    private double fragmentationLevel; // 0.0 to 1.0
    private int simulationDay;
    private long totalWriteOperations;
    private long totalReadOperations;
    private int badBlockCount;
    
    // App-specific storage metrics
    private final Map<String, AppStorageProfile> appStorageProfiles;
    
    public StorageIODegradationHook() {
        this.random = new Random();
        this.usedStorageGB = STORAGE_CAPACITY_GB * 0.3; // Start at 30% used
        this.fragmentationLevel = 0.05; // Initial fragmentation
        this.simulationDay = 1;
        this.totalWriteOperations = 0;
        this.totalReadOperations = 0;
        this.badBlockCount = 0;
        this.appStorageProfiles = new HashMap<>();
    }
    
    /**
     * Update storage state for a new simulation day.
     * 
     * @param newStorageGB additional storage used
     * @param filesDeletedGB storage freed up
     * @param isHeavyWriteDay true if heavy write operations occurred
     * @return Updated storage state
     */
    public StorageState updateStorageState(double newStorageGB, double filesDeletedGB, 
                                          boolean isHeavyWriteDay) {
        // Update storage usage
        usedStorageGB += newStorageGB - filesDeletedGB;
        usedStorageGB = Math.max(0.0, Math.min(STORAGE_CAPACITY_GB, usedStorageGB));
        
        // Update fragmentation
        if (newStorageGB > 0) {
            double fragmentationIncrease = FRAGMENTATION_RATE_PER_DAY;
            if (isHeavyWriteDay) {
                fragmentationIncrease *= 2.0;
            }
            fragmentationLevel += fragmentationIncrease;
            fragmentationLevel = Math.min(1.0, fragmentationLevel);
        }
        
        // Simulate garbage collection (slight fragmentation reduction)
        if (filesDeletedGB > 0 && random.nextDouble() < 0.3) {
            fragmentationLevel *= 0.95;
        }
        
        // Update simulation day
        simulationDay++;
        
        // Simulate bad block development
        if (isHeavyWriteDay && random.nextDouble() < BAD_BLOCK_RATE) {
            badBlockCount++;
        }
        
        return getStorageState();
    }
    
    /**
     * Simulate app load time with storage degradation effects.
     * 
     * @param appName name of the app being loaded
     * @param appSizeMB size of the app in MB
     * @param isColdLoad true if app not recently loaded
     * @return Load time in milliseconds
     */
    public double simulateAppLoadTime(String appName, double appSizeMB, boolean isColdLoad) {
        // Get or create app storage profile
        AppStorageProfile profile = appStorageProfiles.computeIfAbsent(
            appName, k -> new AppStorageProfile(appSizeMB)
        );
        
        // Calculate base load time
        double baseReadSpeedMBps = calculateEffectiveReadSpeed();
        double baseLoadTimeMs = (appSizeMB / baseReadSpeedMBps) * 1000.0;
        
        // Apply fragmentation penalty
        double fragmentationMultiplier = calculateFragmentationMultiplier();
        
        // Apply fill level penalty
        double fillLevelMultiplier = calculateFillLevelMultiplier();
        
        // Apply bad block penalty
        double badBlockMultiplier = 1.0 + (badBlockCount * 0.001);
        
        // Cold load penalty (not in cache)
        double coldLoadMultiplier = isColdLoad ? 1.5 : 1.0;
        
        // Random I/O operations (metadata, database reads)
        double randomIOOverhead = calculateRandomIOOverhead(appSizeMB);
        
        // Calculate total load time
        double totalLoadTimeMs = baseLoadTimeMs * fragmentationMultiplier * 
                                fillLevelMultiplier * badBlockMultiplier * 
                                coldLoadMultiplier + randomIOOverhead;
        
        // Add stochastic variation
        double variation = (random.nextDouble() - 0.5) * (totalLoadTimeMs * 0.2);
        totalLoadTimeMs += variation;
        
        // Update app profile
        profile.lastLoadTimeMs = totalLoadTimeMs;
        profile.totalLoads++;
        totalReadOperations++;
        
        return totalLoadTimeMs;
    }
    
    /**
     * Simulate write operation with storage degradation effects.
     * 
     * @param dataSizeMB size of data to write
     * @param isSequential true if sequential write, false if random
     * @return Write time in milliseconds
     */
    public double simulateWriteOperation(double dataSizeMB, boolean isSequential) {
        // Calculate effective write speed
        double effectiveWriteSpeedMBps = calculateEffectiveWriteSpeed(isSequential);
        
        // Base write time
        double baseWriteTimeMs = (dataSizeMB / effectiveWriteSpeedMBps) * 1000.0;
        
        // Apply fill level penalty (writes slow down as storage fills)
        double fillLevelMultiplier = calculateFillLevelMultiplier();
        
        // Apply wear leveling overhead
        double wearLevelingOverhead = calculateWearLevelingOverhead(dataSizeMB);
        
        // Bad block remapping overhead
        double badBlockOverhead = 0.0;
        if (badBlockCount > 0 && random.nextDouble() < 0.05) {
            badBlockOverhead = badBlockCount * 2.0; // ms per bad block
        }
        
        // Calculate total write time
        double totalWriteTimeMs = baseWriteTimeMs * fillLevelMultiplier + 
                                 wearLevelingOverhead + badBlockOverhead;
        
        // Add stochastic variation
        double variation = (random.nextDouble() - 0.5) * (totalWriteTimeMs * 0.3);
        totalWriteTimeMs += variation;
        
        // Update counters
        totalWriteOperations++;
        
        return totalWriteTimeMs;
    }
    
    /**
     * Calculate effective read speed considering all degradation factors.
     */
    private double calculateEffectiveReadSpeed() {
        double fragmentationMultiplier = calculateFragmentationMultiplier();
        double fillLevelMultiplier = calculateFillLevelMultiplier();
        
        return BASE_READ_SPEED_MBPS / (fragmentationMultiplier * fillLevelMultiplier);
    }
    
    /**
     * Calculate effective write speed considering all degradation factors.
     */
    private double calculateEffectiveWriteSpeed(boolean isSequential) {
        double baseSpeed = isSequential ? BASE_WRITE_SPEED_MBPS : (BASE_WRITE_SPEED_MBPS * 0.4);
        double fillLevelMultiplier = calculateFillLevelMultiplier();
        
        return baseSpeed / fillLevelMultiplier;
    }
    
    /**
     * Calculate fragmentation degradation multiplier.
     * Higher fragmentation = more disk seeks = slower performance.
     */
    private double calculateFragmentationMultiplier() {
        // Fragmentation has exponential impact on performance
        return 1.0 + (fragmentationLevel * fragmentationLevel * 3.0);
    }
    
    /**
     * Calculate fill level degradation multiplier.
     * Performance degrades significantly when storage is >75% full.
     */
    private double calculateFillLevelMultiplier() {
        double fillLevel = usedStorageGB / STORAGE_CAPACITY_GB;
        
        if (fillLevel < FILL_LEVEL_DEGRADATION_THRESHOLD) {
            return 1.0;
        } else if (fillLevel < CRITICAL_FILL_LEVEL) {
            // Linear degradation from 75% to 90%
            double degradationRange = (fillLevel - FILL_LEVEL_DEGRADATION_THRESHOLD) / 
                                    (CRITICAL_FILL_LEVEL - FILL_LEVEL_DEGRADATION_THRESHOLD);
            return 1.0 + (degradationRange * 1.5);
        } else {
            // Exponential degradation above 90%
            double criticalDegradation = (fillLevel - CRITICAL_FILL_LEVEL) / 
                                        (1.0 - CRITICAL_FILL_LEVEL);
            return 2.5 + (criticalDegradation * criticalDegradation * 3.0);
        }
    }
    
    /**
     * Calculate wear leveling overhead for write operations.
     */
    private double calculateWearLevelingOverhead(double dataSizeMB) {
        // Wear leveling becomes more significant as storage fills
        double fillLevel = usedStorageGB / STORAGE_CAPACITY_GB;
        if (fillLevel < 0.5) {
            return 0.0;
        }
        
        double overheadPerMB = fillLevel * fillLevel * WEAR_LEVELING_FACTOR;
        return dataSizeMB * overheadPerMB * 1000.0; // Convert to ms
    }
    
    /**
     * Calculate random I/O overhead for app loading.
     */
    private double calculateRandomIOOverhead(double appSizeMB) {
        // Number of 4K random reads needed
        int randomReadCount = (int) Math.ceil((appSizeMB * 1024) / 4.0);
        
        // Effective random IOPS
        double effectiveRandomIOPS = BASE_RANDOM_READ_IOPS / 
                                   (calculateFragmentationMultiplier() * calculateFillLevelMultiplier());
        
        // Time for random reads
        double randomReadTimeMs = (randomReadCount / effectiveRandomIOPS) * 1000.0;
        
        // Add latency per I/O operation
        double ioLatencyMs = 0.5; // eMMC typical latency
        randomReadTimeMs += randomReadCount * ioLatencyMs;
        
        return randomReadTimeMs;
    }
    
    /**
     * Simulate database query performance degradation.
     * 
     * @param queryComplexity 1.0 to 10.0 (simple to complex)
     * @return Query execution time in milliseconds
     */
    public double simulateDatabaseQuery(double queryComplexity) {
        // Base query time
        double baseQueryTimeMs = queryComplexity * 2.0;
        
        // Fragmentation affects database files significantly
        double fragmentationMultiplier = calculateFragmentationMultiplier();
        
        // Fill level affects database performance
        double fillLevelMultiplier = calculateFillLevelMultiplier();
        
        // Random I/O overhead for database queries
        double randomIOOverhead = queryComplexity * 5.0 * fragmentationMultiplier;
        
        double totalQueryTimeMs = baseQueryTimeMs * fragmentationMultiplier * 
                                fillLevelMultiplier + randomIOOverhead;
        
        // Add stochastic variation
        double variation = (random.nextDouble() - 0.5) * (totalQueryTimeMs * 0.4);
        totalQueryTimeMs += variation;
        
        totalReadOperations++;
        
        return totalQueryTimeMs;
    }
    
    /**
     * Get current storage state for telemetry.
     */
    public StorageState getStorageState() {
        double fillLevel = usedStorageGB / STORAGE_CAPACITY_GB;
        double effectiveReadSpeed = calculateEffectiveReadSpeed();
        double effectiveWriteSpeed = calculateEffectiveWriteSpeed(true);
        
        return new StorageState(
            STORAGE_CAPACITY_GB,
            usedStorageGB,
            fillLevel,
            fragmentationLevel,
            simulationDay,
            effectiveReadSpeed,
            effectiveWriteSpeed,
            badBlockCount,
            totalReadOperations,
            totalWriteOperations
        );
    }
    
    /**
     * Get app-specific storage profile.
     */
    public AppStorageProfile getAppStorageProfile(String appName) {
        return appStorageProfiles.get(appName);
    }
    
    /**
     * Force garbage collection (simulates Android's storage optimization).
     */
    public void forceGarbageCollection() {
        fragmentationLevel *= 0.7; // Reduce fragmentation
    }
    
    /**
     * Data class for app-specific storage metrics.
     */
    public static class AppStorageProfile {
        public final double appSizeMB;
        public double lastLoadTimeMs;
        public int totalLoads;
        
        public AppStorageProfile(double appSizeMB) {
            this.appSizeMB = appSizeMB;
            this.lastLoadTimeMs = 0.0;
            this.totalLoads = 0;
        }
    }
    
    /**
     * Data class for storage state telemetry.
     */
    public static class StorageState {
        public final double totalCapacityGB;
        public final double usedStorageGB;
        public final double fillLevel;
        public final double fragmentationLevel;
        public final int simulationDay;
        public final double effectiveReadSpeedMBps;
        public final double effectiveWriteSpeedMBps;
        public final int badBlockCount;
        public final long totalReadOperations;
        public final long totalWriteOperations;
        
        public StorageState(double totalCapacityGB, double usedStorageGB, 
                          double fillLevel, double fragmentationLevel,
                          int simulationDay, double effectiveReadSpeedMBps,
                          double effectiveWriteSpeedMBps, int badBlockCount,
                          long totalReadOperations, long totalWriteOperations) {
            this.totalCapacityGB = totalCapacityGB;
            this.usedStorageGB = usedStorageGB;
            this.fillLevel = fillLevel;
            this.fragmentationLevel = fragmentationLevel;
            this.simulationDay = simulationDay;
            this.effectiveReadSpeedMBps = effectiveReadSpeedMBps;
            this.effectiveWriteSpeedMBps = effectiveWriteSpeedMBps;
            this.badBlockCount = badBlockCount;
            this.totalReadOperations = totalReadOperations;
            this.totalWriteOperations = totalWriteOperations;
        }
    }
}
