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
        Path path = Paths.get(projectPath);
        run(path);
        
        scanner.close();
    }
    
    /**
     * Runs the duplicate detection on the specified project path.
     */
    public void run(Path projectPath) {
        try {
            log.info("Starting duplicate detection for project: {}", projectPath);
            
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
            List<DuplicateGroup> duplicateGroups = similarityDetector.findSimilarMethods(methods);
            
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
