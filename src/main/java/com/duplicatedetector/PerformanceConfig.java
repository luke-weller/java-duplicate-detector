package com.duplicatedetector;

/**
 * Configuration class for performance tuning of the duplicate detector.
 * Allows users to customize various performance parameters based on their project size and requirements.
 */
public class PerformanceConfig {
    
    // Similarity detection settings
    private double similarityThreshold = 0.7;
    private int minMethodLength = 50;
    private int maxParallelThreads = Runtime.getRuntime().availableProcessors();
    
    // Caching settings
    private boolean enableSimilarityCache = true;
    private int maxCacheSize = 10000;
    
    // Parallel processing thresholds
    private int parallelProcessingThreshold = 50; // Number of files to trigger parallel processing
    private int batchSize = 1000; // Batch size for processing
    
    // Early filtering settings
    private boolean enableEarlyFiltering = true;
    private double lengthDifferenceThreshold = 0.5; // Skip methods with >50% length difference
    private int maxParameterDifference = 2; // Skip methods with >2 parameter difference
    
    // Sampling settings for large groups
    private int maxGroupSizeForFullAnalysis = 10; // Use sampling for groups larger than this
    private int sampleSizeForLargeGroups = 10; // Number of methods to sample
    
    // File processing settings
    private long maxFileSizeBytes = 10 * 1024 * 1024; // 10MB max file size
    private boolean skipLargeFiles = true;
    
    // Memory optimization settings
    private boolean enableMemoryOptimization = true;
    private int maxMethodsInMemory = 50000; // Limit methods kept in memory
    
    /**
     * Default constructor with recommended settings for most projects.
     */
    public PerformanceConfig() {
        // Use default values
    }
    
    /**
     * Constructor for high-performance settings (large projects).
     */
    public static PerformanceConfig forLargeProjects() {
        PerformanceConfig config = new PerformanceConfig();
        config.setMaxParallelThreads(Math.max(4, Runtime.getRuntime().availableProcessors()));
        config.setParallelProcessingThreshold(20);
        config.setBatchSize(2000);
        config.setMaxCacheSize(20000);
        config.setMaxMethodsInMemory(100000);
        config.setEnableEarlyFiltering(true);
        config.setLengthDifferenceThreshold(0.6);
        return config;
    }
    
    /**
     * Constructor for memory-constrained environments.
     */
    public static PerformanceConfig forMemoryConstrained() {
        PerformanceConfig config = new PerformanceConfig();
        config.setMaxParallelThreads(2);
        config.setEnableSimilarityCache(false);
        config.setMaxMethodsInMemory(10000);
        config.setBatchSize(500);
        config.setMaxCacheSize(1000);
        return config;
    }
    
    /**
     * Constructor for high-accuracy settings (smaller projects).
     */
    public static PerformanceConfig forHighAccuracy() {
        PerformanceConfig config = new PerformanceConfig();
        config.setSimilarityThreshold(0.6);
        config.setMinMethodLength(30);
        config.setMaxGroupSizeForFullAnalysis(20);
        config.setEnableEarlyFiltering(false);
        config.setLengthDifferenceThreshold(0.7);
        return config;
    }
    
    // Getters and setters
    
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }
    
    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }
    
    public int getMinMethodLength() {
        return minMethodLength;
    }
    
    public void setMinMethodLength(int minMethodLength) {
        this.minMethodLength = minMethodLength;
    }
    
    public int getMaxParallelThreads() {
        return maxParallelThreads;
    }
    
    public void setMaxParallelThreads(int maxParallelThreads) {
        this.maxParallelThreads = maxParallelThreads;
    }
    
    public boolean isEnableSimilarityCache() {
        return enableSimilarityCache;
    }
    
    public void setEnableSimilarityCache(boolean enableSimilarityCache) {
        this.enableSimilarityCache = enableSimilarityCache;
    }
    
    public int getMaxCacheSize() {
        return maxCacheSize;
    }
    
    public void setMaxCacheSize(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }
    
    public int getParallelProcessingThreshold() {
        return parallelProcessingThreshold;
    }
    
    public void setParallelProcessingThreshold(int parallelProcessingThreshold) {
        this.parallelProcessingThreshold = parallelProcessingThreshold;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public boolean isEnableEarlyFiltering() {
        return enableEarlyFiltering;
    }
    
    public void setEnableEarlyFiltering(boolean enableEarlyFiltering) {
        this.enableEarlyFiltering = enableEarlyFiltering;
    }
    
    public double getLengthDifferenceThreshold() {
        return lengthDifferenceThreshold;
    }
    
    public void setLengthDifferenceThreshold(double lengthDifferenceThreshold) {
        this.lengthDifferenceThreshold = lengthDifferenceThreshold;
    }
    
    public int getMaxParameterDifference() {
        return maxParameterDifference;
    }
    
    public void setMaxParameterDifference(int maxParameterDifference) {
        this.maxParameterDifference = maxParameterDifference;
    }
    
    public int getMaxGroupSizeForFullAnalysis() {
        return maxGroupSizeForFullAnalysis;
    }
    
    public void setMaxGroupSizeForFullAnalysis(int maxGroupSizeForFullAnalysis) {
        this.maxGroupSizeForFullAnalysis = maxGroupSizeForFullAnalysis;
    }
    
    public int getSampleSizeForLargeGroups() {
        return sampleSizeForLargeGroups;
    }
    
    public void setSampleSizeForLargeGroups(int sampleSizeForLargeGroups) {
        this.sampleSizeForLargeGroups = sampleSizeForLargeGroups;
    }
    
    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }
    
    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }
    
    public boolean isSkipLargeFiles() {
        return skipLargeFiles;
    }
    
    public void setSkipLargeFiles(boolean skipLargeFiles) {
        this.skipLargeFiles = skipLargeFiles;
    }
    
    public boolean isEnableMemoryOptimization() {
        return enableMemoryOptimization;
    }
    
    public void setEnableMemoryOptimization(boolean enableMemoryOptimization) {
        this.enableMemoryOptimization = enableMemoryOptimization;
    }
    
    public int getMaxMethodsInMemory() {
        return maxMethodsInMemory;
    }
    
    public void setMaxMethodsInMemory(int maxMethodsInMemory) {
        this.maxMethodsInMemory = maxMethodsInMemory;
    }
    
    @Override
    public String toString() {
        return "PerformanceConfig{" +
                "similarityThreshold=" + similarityThreshold +
                ", minMethodLength=" + minMethodLength +
                ", maxParallelThreads=" + maxParallelThreads +
                ", enableSimilarityCache=" + enableSimilarityCache +
                ", maxCacheSize=" + maxCacheSize +
                ", parallelProcessingThreshold=" + parallelProcessingThreshold +
                ", batchSize=" + batchSize +
                ", enableEarlyFiltering=" + enableEarlyFiltering +
                ", lengthDifferenceThreshold=" + lengthDifferenceThreshold +
                ", maxParameterDifference=" + maxParameterDifference +
                ", maxGroupSizeForFullAnalysis=" + maxGroupSizeForFullAnalysis +
                ", sampleSizeForLargeGroups=" + sampleSizeForLargeGroups +
                ", maxFileSizeBytes=" + maxFileSizeBytes +
                ", skipLargeFiles=" + skipLargeFiles +
                ", enableMemoryOptimization=" + enableMemoryOptimization +
                ", maxMethodsInMemory=" + maxMethodsInMemory +
                '}';
    }
}
