package com.duplicatedetector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class PerformanceConfigTest {

    private PerformanceConfig config;

    @BeforeEach
    void setUp() {
        config = new PerformanceConfig();
    }

    @Test
    void testDefaultConfiguration() {
        assertEquals(0.7, config.getSimilarityThreshold(), 0.001);
        assertEquals(50, config.getMinMethodLength());
        assertEquals(Runtime.getRuntime().availableProcessors(), config.getMaxParallelThreads());
        assertTrue(config.isEnableSimilarityCache());
        assertEquals(10000, config.getMaxCacheSize());
        assertEquals(50, config.getParallelProcessingThreshold());
        assertEquals(1000, config.getBatchSize());
        assertTrue(config.isEnableEarlyFiltering());
        assertEquals(0.5, config.getLengthDifferenceThreshold(), 0.001);
        assertEquals(2, config.getMaxParameterDifference());
        assertEquals(10, config.getMaxGroupSizeForFullAnalysis());
        assertEquals(10, config.getSampleSizeForLargeGroups());
        assertEquals(10 * 1024 * 1024, config.getMaxFileSizeBytes());
        assertTrue(config.isSkipLargeFiles());
        assertTrue(config.isEnableMemoryOptimization());
        assertEquals(50000, config.getMaxMethodsInMemory());
    }

    @Test
    void testForLargeProjectsConfiguration() {
        PerformanceConfig largeProjectConfig = PerformanceConfig.forLargeProjects();
        
        assertTrue(largeProjectConfig.getMaxParallelThreads() >= 4);
        assertEquals(20, largeProjectConfig.getParallelProcessingThreshold());
        assertEquals(2000, largeProjectConfig.getBatchSize());
        assertEquals(20000, largeProjectConfig.getMaxCacheSize());
        assertEquals(100000, largeProjectConfig.getMaxMethodsInMemory());
        assertTrue(largeProjectConfig.isEnableEarlyFiltering());
        assertEquals(0.6, largeProjectConfig.getLengthDifferenceThreshold(), 0.001);
    }

    @Test
    void testForMemoryConstrainedConfiguration() {
        PerformanceConfig memoryConfig = PerformanceConfig.forMemoryConstrained();
        
        assertEquals(2, memoryConfig.getMaxParallelThreads());
        assertFalse(memoryConfig.isEnableSimilarityCache());
        assertEquals(10000, memoryConfig.getMaxMethodsInMemory());
        assertEquals(500, memoryConfig.getBatchSize());
        assertEquals(1000, memoryConfig.getMaxCacheSize());
    }

    @Test
    void testForHighAccuracyConfiguration() {
        PerformanceConfig accuracyConfig = PerformanceConfig.forHighAccuracy();
        
        assertEquals(0.6, accuracyConfig.getSimilarityThreshold(), 0.001);
        assertEquals(30, accuracyConfig.getMinMethodLength());
        assertEquals(20, accuracyConfig.getMaxGroupSizeForFullAnalysis());
        assertFalse(accuracyConfig.isEnableEarlyFiltering());
        assertEquals(0.7, accuracyConfig.getLengthDifferenceThreshold(), 0.001);
    }

    @Test
    void testSettersAndGetters() {
        config.setSimilarityThreshold(0.8);
        assertEquals(0.8, config.getSimilarityThreshold(), 0.001);
        
        config.setMinMethodLength(100);
        assertEquals(100, config.getMinMethodLength());
        
        config.setMaxParallelThreads(4);
        assertEquals(4, config.getMaxParallelThreads());
        
        config.setEnableSimilarityCache(false);
        assertFalse(config.isEnableSimilarityCache());
        
        config.setMaxCacheSize(5000);
        assertEquals(5000, config.getMaxCacheSize());
        
        config.setParallelProcessingThreshold(100);
        assertEquals(100, config.getParallelProcessingThreshold());
        
        config.setBatchSize(1500);
        assertEquals(1500, config.getBatchSize());
        
        config.setEnableEarlyFiltering(false);
        assertFalse(config.isEnableEarlyFiltering());
        
        config.setLengthDifferenceThreshold(0.8);
        assertEquals(0.8, config.getLengthDifferenceThreshold(), 0.001);
        
        config.setMaxParameterDifference(3);
        assertEquals(3, config.getMaxParameterDifference());
        
        config.setMaxGroupSizeForFullAnalysis(15);
        assertEquals(15, config.getMaxGroupSizeForFullAnalysis());
        
        config.setSampleSizeForLargeGroups(20);
        assertEquals(20, config.getSampleSizeForLargeGroups());
        
        config.setMaxFileSizeBytes(5 * 1024 * 1024);
        assertEquals(5 * 1024 * 1024, config.getMaxFileSizeBytes());
        
        config.setSkipLargeFiles(false);
        assertFalse(config.isSkipLargeFiles());
        
        config.setEnableMemoryOptimization(false);
        assertFalse(config.isEnableMemoryOptimization());
        
        config.setMaxMethodsInMemory(25000);
        assertEquals(25000, config.getMaxMethodsInMemory());
    }

    @Test
    void testToString() {
        String configString = config.toString();
        
        assertTrue(configString.contains("PerformanceConfig"));
        assertTrue(configString.contains("similarityThreshold=0.7"));
        assertTrue(configString.contains("minMethodLength=50"));
        assertTrue(configString.contains("maxParallelThreads=" + Runtime.getRuntime().availableProcessors()));
        assertTrue(configString.contains("enableSimilarityCache=true"));
        assertTrue(configString.contains("maxCacheSize=10000"));
    }

    @Test
    void testConfigurationEquality() {
        PerformanceConfig config1 = new PerformanceConfig();
        PerformanceConfig config2 = new PerformanceConfig();
        
        // Set same values
        config1.setSimilarityThreshold(0.8);
        config2.setSimilarityThreshold(0.8);
        config1.setMinMethodLength(100);
        config2.setMinMethodLength(100);
        
        // Test toString equality (since we don't override equals)
        assertEquals(config1.toString(), config2.toString());
    }

    @Test
    void testBoundaryValues() {
        // Test minimum values
        config.setSimilarityThreshold(0.0);
        assertEquals(0.0, config.getSimilarityThreshold(), 0.001);
        
        config.setSimilarityThreshold(1.0);
        assertEquals(1.0, config.getSimilarityThreshold(), 0.001);
        
        config.setMinMethodLength(1);
        assertEquals(1, config.getMinMethodLength());
        
        config.setMaxParallelThreads(1);
        assertEquals(1, config.getMaxParallelThreads());
        
        config.setMaxCacheSize(1);
        assertEquals(1, config.getMaxCacheSize());
        
        config.setBatchSize(1);
        assertEquals(1, config.getBatchSize());
        
        config.setMaxMethodsInMemory(1);
        assertEquals(1, config.getMaxMethodsInMemory());
    }

    @Test
    void testConfigurationProfilesAreDifferent() {
        PerformanceConfig defaultConfig = new PerformanceConfig();
        PerformanceConfig largeProjectConfig = PerformanceConfig.forLargeProjects();
        PerformanceConfig memoryConfig = PerformanceConfig.forMemoryConstrained();
        PerformanceConfig accuracyConfig = PerformanceConfig.forHighAccuracy();
        
        // Verify they are different configurations
        assertNotEquals(defaultConfig.toString(), largeProjectConfig.toString());
        assertNotEquals(defaultConfig.toString(), memoryConfig.toString());
        assertNotEquals(defaultConfig.toString(), accuracyConfig.toString());
        assertNotEquals(largeProjectConfig.toString(), memoryConfig.toString());
        assertNotEquals(largeProjectConfig.toString(), accuracyConfig.toString());
        assertNotEquals(memoryConfig.toString(), accuracyConfig.toString());
    }
}
