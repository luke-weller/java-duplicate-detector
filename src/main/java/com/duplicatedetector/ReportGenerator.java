package com.duplicatedetector;

import com.duplicatedetector.analyzer.RefactoringAnalysis;
import com.duplicatedetector.analyzer.RefactoringAnalyzer;
import com.duplicatedetector.model.DuplicateGroup;
import com.duplicatedetector.model.MethodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates comprehensive reports for duplicate detection results.
 * Supports both console output and file output in various formats.
 */
public class ReportGenerator {
    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);
    
    private final Path projectPath;
    private final PerformanceConfig config;
    private final List<DuplicateGroup> duplicateGroups;
    
    public ReportGenerator(Path projectPath, PerformanceConfig config, List<DuplicateGroup> duplicateGroups) {
        this.projectPath = projectPath;
        this.config = config;
        this.duplicateGroups = new ArrayList<>(duplicateGroups);
    }
    
    /**
     * Generates and displays the complete report (console + file).
     */
    public void generateReport() {
        // Generate console report
        generateConsoleReport();
        
        // Generate file report
        generateFileReport();
    }
    
    /**
     * Generates the console report (existing functionality).
     */
    private void generateConsoleReport() {
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
     * Generates a comprehensive file report in the project directory.
     */
    private void generateFileReport() {
        try {
            Path reportPath = determineReportPath();
            String reportContent = generateReportContent();
            
            Files.write(reportPath, reportContent.getBytes());
            log.info("📄 Detailed report saved to: {}", reportPath);
            
        } catch (IOException e) {
            log.error("Failed to generate file report", e);
        }
    }
    
    /**
     * Determines the appropriate path for the report file.
     */
    private Path determineReportPath() {
        // Create reports directory in project root if it doesn't exist
        Path reportsDir = projectPath.resolve("reports");
        try {
            if (!Files.exists(reportsDir)) {
                Files.createDirectories(reportsDir);
            }
        } catch (IOException e) {
            log.warn("Could not create reports directory, using project root", e);
            reportsDir = projectPath;
        }
        
        // Generate timestamp for unique filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("duplicate-detection-report_%s.txt", timestamp);
        
        return reportsDir.resolve(filename);
    }
    
    /**
     * Generates the complete report content for file output.
     */
    private String generateReportContent() {
        StringBuilder report = new StringBuilder();
        
        // Header
        report.append("=".repeat(80)).append("\n");
        report.append("JAVA DUPLICATE DETECTOR - DETAILED ANALYSIS REPORT\n");
        report.append("=".repeat(80)).append("\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("Project: ").append(projectPath.toAbsolutePath()).append("\n");
        report.append("Configuration: ").append(config.toString()).append("\n");
        report.append("=".repeat(80)).append("\n\n");
        
        if (duplicateGroups.isEmpty()) {
            report.append("✅ No duplicate methods found! Your code follows good practices.\n");
            return report.toString();
        }
        
        // Sort groups by similarity score (highest first)
        List<DuplicateGroup> sortedGroups = new ArrayList<>(duplicateGroups);
        sortedGroups.sort((g1, g2) -> Double.compare(g2.getSimilarityScore(), g1.getSimilarityScore()));
        
        // Summary statistics
        report.append("SUMMARY STATISTICS\n");
        report.append("-".repeat(40)).append("\n");
        report.append("Total duplicate groups: ").append(sortedGroups.size()).append("\n");
        
        long totalMethods = sortedGroups.stream()
            .mapToLong(group -> group.getMethods().size())
            .sum();
        report.append("Total duplicate methods: ").append(totalMethods).append("\n");
        
        RefactoringAnalyzer analyzer = new RefactoringAnalyzer();
        long highSeverity = sortedGroups.stream()
            .filter(group -> {
                RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
                return "HIGH".equals(analysis.getSeverity());
            })
            .count();
        report.append("High severity issues: ").append(highSeverity).append("\n");
        
        long crossClassIssues = sortedGroups.stream()
            .filter(group -> {
                RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
                return analysis.isCrossClassIssue();
            })
            .count();
        report.append("Cross-class duplications: ").append(crossClassIssues).append("\n");
        report.append("\n");
        
        // Detailed analysis for each group
        report.append("DETAILED ANALYSIS\n");
        report.append("=".repeat(80)).append("\n\n");
        
        for (int i = 0; i < sortedGroups.size(); i++) {
            DuplicateGroup group = sortedGroups.get(i);
            RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
            
            report.append(generateGroupReport(i + 1, analysis));
        }
        
        // Recommendations and next steps
        report.append("RECOMMENDATIONS AND NEXT STEPS\n");
        report.append("=".repeat(80)).append("\n");
        report.append("1. Review groups in order (highest similarity first)\n");
        report.append("2. Focus on high severity duplications\n");
        report.append("3. Apply recommended design patterns\n");
        report.append("4. Consider creating shared utility classes\n");
        report.append("5. Update unit tests after refactoring\n");
        report.append("6. Run the tool again to verify improvements\n\n");
        
        if (highSeverity > 0) {
            report.append("🚨 PRIORITY: Focus on high severity issues first!\n\n");
        }
        
        report.append("=".repeat(80)).append("\n");
        report.append("Report generated by Java Duplicate Detector\n");
        report.append("=".repeat(80)).append("\n");
        
        return report.toString();
    }
    
    /**
     * Generates detailed report content for a single group.
     */
    private String generateGroupReport(int groupNumber, RefactoringAnalysis analysis) {
        StringBuilder groupReport = new StringBuilder();
        
        // Header
        groupReport.append("Group ").append(groupNumber).append(": ").append(analysis.getDuplicationType())
            .append(" (").append(analysis.getFormattedSimilarityPercentage()).append(" similarity)\n");
        groupReport.append("-".repeat(60)).append("\n");
        
        // Severity
        groupReport.append("Severity: ").append(analysis.getSeverity()).append("\n");
        
        // Methods involved
        groupReport.append("Methods involved (").append(analysis.getMethodCount())
            .append(" methods across ").append(analysis.getUniqueClassCount()).append(" classes):\n");
        
        for (MethodInfo method : analysis.getMethods()) {
            groupReport.append("  • ").append(method.getClassName()).append(".")
                .append(method.getMethodName()).append("() in ")
                .append(method.getFilePath()).append(" (lines ")
                .append(method.getStartLine()).append("-").append(method.getEndLine()).append(")\n");
        }
        groupReport.append("\n");
        
        // Cross-class issue warning
        if (analysis.isCrossClassIssue()) {
            groupReport.append("⚠️  Cross-class duplication detected!\n");
            groupReport.append("   ").append(analysis.getCrossClassRecommendation()).append("\n\n");
        }
        
        // Refactoring pattern recommendation
        groupReport.append("Recommended Pattern: ").append(analysis.getRefactoringPattern()).append("\n\n");
        
        // Specific recommendations
        groupReport.append("Refactoring Recommendations:\n");
        for (String recommendation : analysis.getRecommendations()) {
            groupReport.append("  • ").append(recommendation).append("\n");
        }
        groupReport.append("\n");
        
        // Code quality insights
        if (!analysis.getCodeQualityInsights().isEmpty()) {
            groupReport.append("Code Quality Insights:\n");
            for (String insight : analysis.getCodeQualityInsights()) {
                groupReport.append("  • ").append(insight).append("\n");
            }
            groupReport.append("\n");
        }
        
        // Additional insights
        if (!analysis.getInsights().isEmpty()) {
            groupReport.append("Analysis Insights:\n");
            for (String insight : analysis.getInsights()) {
                groupReport.append("  • ").append(insight).append("\n");
            }
            groupReport.append("\n");
        }
        
        // Common parameters
        if (!analysis.getCommonParameters().isEmpty()) {
            groupReport.append("Common Parameter Types: ")
                .append(String.join(", ", analysis.getCommonParameters())).append("\n\n");
        }
        
        groupReport.append("\n");
        return groupReport.toString();
    }
    
    /**
     * Displays detailed analysis for a single group (console output).
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
     * Displays a summary of all findings (console output).
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
