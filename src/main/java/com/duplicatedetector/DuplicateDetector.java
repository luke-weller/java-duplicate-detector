package com.duplicatedetector;

import com.duplicatedetector.analyzer.MethodAnalyzer;
import com.duplicatedetector.analyzer.RefactoringAnalyzer;
import com.duplicatedetector.analyzer.RefactoringAnalysis;
import com.duplicatedetector.model.MethodInfo;
import com.duplicatedetector.model.DuplicateGroup;
import com.duplicatedetector.scanner.ProjectScanner;
import com.duplicatedetector.similarity.SimilarityDetector;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Main class for the Java Duplicate Detector tool.
 * Scans Java projects to find duplicate or similar methods that can be refactored.
 */
public class DuplicateDetector {
    private static final Logger log = LoggerFactory.getLogger(DuplicateDetector.class);
    
    public static void main(String[] args) {
        DuplicateDetector detector = new DuplicateDetector();
        
        if (args.length > 0) {
            log.info("Starting duplicate detection with command line argument: {}", args[0]);
            detector.run(Paths.get(args[0]));
        } else {
            log.info("Starting duplicate detection in interactive mode");
            detector.runInteractive();
        }
    }
    
    /**
     * Runs the duplicate detection in interactive mode.
     */
    public void runInteractive() {
        Scanner scanner = new Scanner(System.in);
        
        log.info("=== Java Duplicate Detector ===");
        log.info("This tool scans Java projects to find duplicate or similar methods.");
        log.info("");
        
        log.info("Enter the path to your Java project: ");
        String projectPath = scanner.nextLine().trim();
        
        if (projectPath.isEmpty()) {
            log.warn("No path provided. Exiting.");
            return;
        }
        
        log.info("User provided project path: {}", projectPath);
        
        // Performance configuration selection
        PerformanceConfig config = selectPerformanceConfig(scanner);
        
        Path path = Paths.get(projectPath);
        run(path, config);
        
        scanner.close();
    }
    
    /**
     * Allows user to select performance configuration.
     */
    private PerformanceConfig selectPerformanceConfig(Scanner scanner) {
        log.info("");
        log.info("Select performance configuration:");
        log.info("1. Default (balanced performance and accuracy)");
        log.info("2. High Performance (optimized for large projects)");
        log.info("3. High Accuracy (more thorough analysis)");
        log.info("4. Memory Constrained (for limited memory environments)");
        log.info("5. Custom configuration");
        log.info("");
        log.info("Enter your choice (1-5): ");
        
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                log.info("Using default configuration");
                return new PerformanceConfig();
            case "2":
                log.info("Using high performance configuration for large projects");
                return PerformanceConfig.forLargeProjects();
            case "3":
                log.info("Using high accuracy configuration");
                return PerformanceConfig.forHighAccuracy();
            case "4":
                log.info("Using memory constrained configuration");
                return PerformanceConfig.forMemoryConstrained();
            case "5":
                return createCustomConfig(scanner);
            default:
                log.info("Invalid choice, using default configuration");
                return new PerformanceConfig();
        }
    }
    
    /**
     * Creates a custom performance configuration based on user input.
     */
    private PerformanceConfig createCustomConfig(Scanner scanner) {
        PerformanceConfig config = new PerformanceConfig();
        
        log.info("");
        log.info("Custom Configuration Setup:");
        
        // Similarity threshold
        log.info("Enter similarity threshold (0.0-1.0, default 0.7): ");
        String thresholdStr = scanner.nextLine().trim();
        if (!thresholdStr.isEmpty()) {
            try {
                double threshold = Double.parseDouble(thresholdStr);
                if (threshold >= 0.0 && threshold <= 1.0) {
                    config.setSimilarityThreshold(threshold);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid threshold, using default");
            }
        }
        
        // Min method length
        log.info("Enter minimum method length (default 50): ");
        String lengthStr = scanner.nextLine().trim();
        if (!lengthStr.isEmpty()) {
            try {
                int length = Integer.parseInt(lengthStr);
                if (length > 0) {
                    config.setMinMethodLength(length);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid length, using default");
            }
        }
        
        // Parallel threads
        log.info("Enter number of parallel threads (default {}): ", Runtime.getRuntime().availableProcessors());
        String threadsStr = scanner.nextLine().trim();
        if (!threadsStr.isEmpty()) {
            try {
                int threads = Integer.parseInt(threadsStr);
                if (threads > 0) {
                    config.setMaxParallelThreads(threads);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid thread count, using default");
            }
        }
        
        log.info("Custom configuration created: {}", config);
        return config;
    }
    
    /**
     * Runs the duplicate detection on the specified project path.
     */
    public void run(Path projectPath) {
        run(projectPath, new PerformanceConfig());
    }
    
    /**
     * Runs the duplicate detection on the specified project path with custom performance configuration.
     */
    public void run(Path projectPath, PerformanceConfig config) {
        try {
            log.info("Starting duplicate detection for project: {} with config: {}", projectPath, config);
            
            // Step 1: Scan the project for Java files
            log.debug("Step 1: Scanning project for Java files");
            ProjectScanner scanner = new ProjectScanner();
            List<Path> javaFiles = scanner.scanProject(projectPath);
            
            if (javaFiles.isEmpty()) {
                log.warn("No Java files found in the specified project: {}", projectPath);
                return;
            }
            
            log.info("Found {} Java files to analyze", javaFiles.size());
            log.debug("Java files found: {}", javaFiles);
            
            // Step 2: Analyze methods in all files
            log.debug("Step 2: Analyzing methods in all files");
            MethodAnalyzer analyzer = new MethodAnalyzer();
            List<MethodInfo> methods = analyzer.analyzeMethods(javaFiles);
            
            if (methods.isEmpty()) {
                log.warn("No methods found in the project: {}", projectPath);
                return;
            }
            
            log.info("Analyzed {} methods from {} files", methods.size(), javaFiles.size());
            log.debug("Methods found: {}", methods.stream().map(MethodInfo::getFullMethodName).collect(java.util.stream.Collectors.toList()));
            
            // Step 3: Detect similar methods
            log.debug("Step 3: Detecting similar methods");
            SimilarityDetector similarityDetector = new SimilarityDetector();
            List<DuplicateGroup> duplicateGroups = similarityDetector.findSimilarMethods(methods, config.getSimilarityThreshold());
            
            // Step 4: Display results
            log.debug("Step 4: Displaying results");
            displayResults(duplicateGroups);
            
        } catch (Exception e) {
            log.error("Error during duplicate detection for project: {}", projectPath, e);
            throw new RuntimeException("Failed to analyze project: " + projectPath, e);
        }
    }
    
    /**
     * Displays the results of the duplicate detection with detailed analysis.
     */
    private void displayResults(List<DuplicateGroup> duplicateGroups) {
        log.info("=== Java Duplicate Detector - Detailed Analysis ===");
        
        if (duplicateGroups.isEmpty()) {
            log.info("✅ No duplicate methods found! Your code follows good practices.");
            return;
        }
        
        // Sort groups by similarity score (highest first) for better prioritization
        List<DuplicateGroup> sortedGroups = new ArrayList<>(duplicateGroups);
        sortedGroups.sort((g1, g2) -> Double.compare(g2.getSimilarityScore(), g1.getSimilarityScore()));
        
        log.info("🔍 Found {} groups of similar methods requiring attention:", sortedGroups.size());
        log.info("📊 Groups are ordered by similarity score (highest first) for prioritization");
        log.info("");
        
        RefactoringAnalyzer analyzer = new RefactoringAnalyzer();
        
        for (int i = 0; i < sortedGroups.size(); i++) {
            DuplicateGroup group = sortedGroups.get(i);
            RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
            
            displayGroupAnalysis(i + 1, analysis);
        }
        
        displaySummary(sortedGroups);
        log.info("=== End of Analysis ===");
    }
    
    /**
     * Displays detailed analysis for a single group.
     */
    private void displayGroupAnalysis(int groupNumber, RefactoringAnalysis analysis) {
        // Header with severity indicator
        String severityIcon = getSeverityIcon(analysis.getSeverity());
        log.info("{} Group {}: {} ({}% similarity)", 
            severityIcon, groupNumber, analysis.getDuplicationType(), 
            analysis.getFormattedSimilarityPercentage());
        
        // Method details
        log.info("📋 Methods involved ({} methods across {} classes):", 
            analysis.getMethodCount(), analysis.getUniqueClassCount());
        
        for (MethodInfo method : analysis.getMethods()) {
            log.info("   • {}.{}() in {} (lines {}-{})", 
                method.getClassName(), 
                method.getMethodName(), 
                method.getFilePath(),
                method.getStartLine(),
                method.getEndLine());
        }
        
        // Cross-class issue warning
        if (analysis.isCrossClassIssue()) {
            log.info("⚠️  Cross-class duplication detected!");
            log.info("   {}", analysis.getCrossClassRecommendation());
        }
        
        // Refactoring pattern recommendation
        log.info("🎯 Recommended Pattern: {}", analysis.getRefactoringPattern());
        
        // Specific recommendations
        log.info("💡 Refactoring Recommendations:");
        for (String recommendation : analysis.getRecommendations()) {
            log.info("   • {}", recommendation);
        }
        
        // Code quality insights
        if (!analysis.getCodeQualityInsights().isEmpty()) {
            log.info("🔧 Code Quality Insights:");
            for (String insight : analysis.getCodeQualityInsights()) {
                log.info("   • {}", insight);
            }
        }
        
        // Additional insights
        if (!analysis.getInsights().isEmpty()) {
            log.info("📊 Analysis Insights:");
            for (String insight : analysis.getInsights()) {
                log.info("   • {}", insight);
            }
        }
        
        // Common parameters
        if (!analysis.getCommonParameters().isEmpty()) {
            log.info("🔗 Common Parameter Types: {}", 
                String.join(", ", analysis.getCommonParameters()));
        }
        
        log.info("");
    }
    
    /**
     * Displays a summary of all findings.
     */
    private void displaySummary(List<DuplicateGroup> duplicateGroups) {
        log.info("📈 Summary:");
        
        long totalMethods = duplicateGroups.stream()
            .mapToLong(group -> group.getMethods().size())
            .sum();
        
        long highSeverity = duplicateGroups.stream()
            .filter(group -> {
                RefactoringAnalyzer analyzer = new RefactoringAnalyzer();
                RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
                return "HIGH".equals(analysis.getSeverity());
            })
            .count();
        
        long crossClassIssues = duplicateGroups.stream()
            .filter(group -> {
                RefactoringAnalyzer analyzer = new RefactoringAnalyzer();
                RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
                return analysis.isCrossClassIssue();
            })
            .count();
        
        log.info("   • Total duplicate methods: {}", totalMethods);
        log.info("   • High severity issues: {}", highSeverity);
        log.info("   • Cross-class duplications: {}", crossClassIssues);
        log.info("   • Groups requiring immediate attention: {}", highSeverity);
        
        if (highSeverity > 0) {
            log.info("🚨 Priority: Focus on high severity issues first!");
        }
        
        log.info("");
        log.info("💡 Next Steps:");
        log.info("   1. Review groups in order (highest similarity first)");
        log.info("   2. Focus on high severity duplications");
        log.info("   3. Apply recommended design patterns");
        log.info("   4. Consider creating shared utility classes");
        log.info("   5. Update unit tests after refactoring");
        log.info("   6. Run the tool again to verify improvements");
    }
    
    /**
     * Returns an appropriate icon for the severity level.
     */
    private String getSeverityIcon(String severity) {
        switch (severity) {
            case "HIGH": return "🚨";
            case "MEDIUM": return "⚠️";
            case "LOW": return "ℹ️";
            default: return "❓";
        }
    }
}
