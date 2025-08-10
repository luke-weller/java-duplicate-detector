package com.duplicatedetector;

import com.duplicatedetector.analyzer.MethodAnalyzer;
import com.duplicatedetector.model.MethodInfo;
import com.duplicatedetector.scanner.ProjectScanner;
import com.duplicatedetector.similarity.SimilarityDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Performance benchmark utility for the duplicate detector.
 * Helps users measure and compare performance improvements.
 */
public class PerformanceBenchmark {
    private static final Logger log = LoggerFactory.getLogger(PerformanceBenchmark.class);
    
    /**
     * Runs a performance benchmark on the specified project.
     */
    public static void runBenchmark(String projectPath) {
        runBenchmark(Paths.get(projectPath));
    }
    
    /**
     * Runs a performance benchmark on the specified project.
     */
    public static void runBenchmark(Path projectPath) {
        log.info("=== Performance Benchmark ===");
        log.info("Project: {}", projectPath);
        log.info("");
        
        try {
            // Test different configurations
            PerformanceConfig[] configs = {
                new PerformanceConfig(), // Default
                PerformanceConfig.forLargeProjects(), // High Performance
                PerformanceConfig.forHighAccuracy(), // High Accuracy
                PerformanceConfig.forMemoryConstrained() // Memory Constrained
            };
            
            String[] configNames = {
                "Default",
                "High Performance",
                "High Accuracy", 
                "Memory Constrained"
            };
            
            for (int i = 0; i < configs.length; i++) {
                runBenchmarkWithConfig(projectPath, configs[i], configNames[i]);
                log.info("");
            }
            
            log.info("=== Benchmark Complete ===");
            
        } catch (Exception e) {
            log.error("Benchmark failed", e);
        }
    }
    
    /**
     * Runs benchmark with a specific configuration.
     */
    private static void runBenchmarkWithConfig(Path projectPath, PerformanceConfig config, String configName) {
        log.info("Testing configuration: {}", configName);
        log.info("Config: {}", config);
        
        long totalStartTime = System.currentTimeMillis();
        
        try {
            // Step 1: Project scanning
            long scanStartTime = System.currentTimeMillis();
            ProjectScanner scanner = new ProjectScanner();
            List<Path> javaFiles = scanner.scanProject(projectPath);
            long scanEndTime = System.currentTimeMillis();
            long scanTime = scanEndTime - scanStartTime;
            
            log.info("  Scanning: {} files in {} ms", javaFiles.size(), scanTime);
            
            if (javaFiles.isEmpty()) {
                log.warn("  No Java files found, skipping configuration");
                return;
            }
            
            // Step 2: Method analysis
            long analysisStartTime = System.currentTimeMillis();
            MethodAnalyzer analyzer = new MethodAnalyzer();
            List<MethodInfo> methods = analyzer.analyzeMethods(javaFiles);
            long analysisEndTime = System.currentTimeMillis();
            long analysisTime = analysisEndTime - analysisStartTime;
            
            log.info("  Analysis: {} methods in {} ms", methods.size(), analysisTime);
            
            if (methods.isEmpty()) {
                log.warn("  No methods found, skipping configuration");
                return;
            }
            
            // Step 3: Similarity detection
            long similarityStartTime = System.currentTimeMillis();
            SimilarityDetector similarityDetector = new SimilarityDetector();
            List<com.duplicatedetector.model.DuplicateGroup> duplicateGroups = 
                similarityDetector.findSimilarMethods(methods, config.getSimilarityThreshold());
            long similarityEndTime = System.currentTimeMillis();
            long similarityTime = similarityEndTime - similarityStartTime;
            
            log.info("  Similarity Detection: {} groups in {} ms", duplicateGroups.size(), similarityTime);
            
            // Calculate total time and performance metrics
            long totalEndTime = System.currentTimeMillis();
            long totalTime = totalEndTime - totalStartTime;
            
            // Performance metrics
            double filesPerSecond = (double) javaFiles.size() / (scanTime / 1000.0);
            double methodsPerSecond = (double) methods.size() / (analysisTime / 1000.0);
            double comparisonsPerSecond = calculateComparisonsPerSecond(methods.size(), similarityTime);
            
            log.info("  Performance Metrics:");
            log.info("    Total time: {} ms", totalTime);
            log.info("    Files processed per second: {:.2f}", filesPerSecond);
            log.info("    Methods analyzed per second: {:.2f}", methodsPerSecond);
            log.info("    Similarity comparisons per second: {:.2f}", comparisonsPerSecond);
            
            // Memory usage estimation
            long estimatedMemoryMB = estimateMemoryUsage(javaFiles.size(), methods.size());
            log.info("    Estimated memory usage: {} MB", estimatedMemoryMB);
            
        } catch (Exception e) {
            log.error("  Benchmark failed for configuration: {}", configName, e);
        }
    }
    
    /**
     * Calculates the number of similarity comparisons per second.
     */
    private static double calculateComparisonsPerSecond(int methodCount, long timeMs) {
        if (timeMs == 0) return 0.0;
        
        // Estimate number of comparisons (n * (n-1) / 2 for pairwise comparisons)
        long totalComparisons = (long) methodCount * (methodCount - 1) / 2;
        return (double) totalComparisons / (timeMs / 1000.0);
    }
    
    /**
     * Estimates memory usage in MB.
     */
    private static long estimateMemoryUsage(int fileCount, int methodCount) {
        // Rough estimation based on typical memory usage
        long fileMemory = fileCount * 1024; // ~1KB per file path
        long methodMemory = methodCount * 2048; // ~2KB per method (signature + body)
        long totalBytes = fileMemory + methodMemory;
        return totalBytes / (1024 * 1024); // Convert to MB
    }
    
    /**
     * Main method for running benchmarks from command line.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            log.error("Please provide a project path as argument");
            log.info("Usage: java PerformanceBenchmark <project-path>");
            return;
        }
        
        String projectPath = args[0];
        runBenchmark(projectPath);
    }
}
