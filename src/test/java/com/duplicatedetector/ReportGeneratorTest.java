package com.duplicatedetector;

import com.duplicatedetector.model.DuplicateGroup;
import com.duplicatedetector.model.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {

    @TempDir
    Path tempDir;
    
    private ReportGenerator reportGenerator;
    private PerformanceConfig config;
    private List<DuplicateGroup> duplicateGroups;
    private Path projectPath;

    @BeforeEach
    void setUp() {
        projectPath = tempDir.resolve("test-project");
        config = new PerformanceConfig();
        duplicateGroups = new ArrayList<>();
    }

    @Test
    void testGenerateReportWithNoDuplicates() throws IOException {
        // Create ReportGenerator with empty list
        reportGenerator = new ReportGenerator(projectPath, config, duplicateGroups);
        
        // When
        reportGenerator.generateReport();
        
        // Then - should create reports directory and file
        Path reportsDir = projectPath.resolve("reports");
        assertTrue(Files.exists(reportsDir), "Reports directory should be created");
        
        // Find the generated report file
        Path reportFile = findReportFile(reportsDir);
        assertNotNull(reportFile, "Report file should be created");
        
        // Verify report content
        String reportContent = Files.readString(reportFile);
        assertTrue(reportContent.contains("JAVA DUPLICATE DETECTOR - DETAILED ANALYSIS REPORT"));
        assertTrue(reportContent.contains("✅ No duplicate methods found! Your code follows good practices."));
        assertTrue(reportContent.contains("Generated: "));
        assertTrue(reportContent.contains("Project: " + projectPath.toAbsolutePath()));
    }

    @Test
    void testGenerateReportWithDuplicates() throws IOException {
        // Given
        MethodInfo method1 = createMethodInfo("TestClass1", "method1", "src/TestClass1.java", 10, 20);
        MethodInfo method2 = createMethodInfo("TestClass2", "method2", "src/TestClass2.java", 15, 25);
        
        DuplicateGroup group = new DuplicateGroup(Arrays.asList(method1, method2), 0.85, "EXACT");
        duplicateGroups.add(group);
        
        // Create ReportGenerator after adding groups
        reportGenerator = new ReportGenerator(projectPath, config, duplicateGroups);
        
        // When
        reportGenerator.generateReport();
        
        // Then
        Path reportsDir = projectPath.resolve("reports");
        Path reportFile = findReportFile(reportsDir);
        assertNotNull(reportFile);
        
        String reportContent = Files.readString(reportFile);
        
        assertTrue(reportContent.contains("Total duplicate groups: 1"));
        assertTrue(reportContent.contains("Total duplicate methods: 2"));
        assertTrue(reportContent.contains("TestClass1.method1()"));
        assertTrue(reportContent.contains("TestClass2.method2()"));
        assertTrue(reportContent.contains("85.0"));
        assertTrue(reportContent.contains("DETAILED ANALYSIS"));
        assertTrue(reportContent.contains("Group 1:"));
    }

    @Test
    void testGenerateReportWithMultipleGroups() throws IOException {
        // Given
        MethodInfo method1 = createMethodInfo("Class1", "method1", "src/Class1.java", 10, 20);
        MethodInfo method2 = createMethodInfo("Class2", "method2", "src/Class2.java", 15, 25);
        MethodInfo method3 = createMethodInfo("Class3", "method3", "src/Class3.java", 20, 30);
        
        DuplicateGroup group1 = new DuplicateGroup(Arrays.asList(method1, method2), 0.95, "EXACT");
        DuplicateGroup group2 = new DuplicateGroup(Arrays.asList(method3), 0.75, "STRUCTURAL");
        
        duplicateGroups.addAll(Arrays.asList(group1, group2));
        
        // Create ReportGenerator after adding groups
        reportGenerator = new ReportGenerator(projectPath, config, duplicateGroups);
        
        // When
        reportGenerator.generateReport();
        
        // Then
        Path reportFile = findReportFile(projectPath.resolve("reports"));
        String reportContent = Files.readString(reportFile);
        
        assertTrue(reportContent.contains("Total duplicate groups: 2"));
        assertTrue(reportContent.contains("Total duplicate methods: 3"));
        assertTrue(reportContent.contains("Group 1:"));
        assertTrue(reportContent.contains("Group 2:"));
        assertTrue(reportContent.contains("95.0"));
        assertTrue(reportContent.contains("75.0"));
        assertTrue(reportContent.contains("DETAILED ANALYSIS"));
    }

    @Test
    void testReportFileNaming() throws IOException {
        // Create ReportGenerator with empty list
        reportGenerator = new ReportGenerator(projectPath, config, duplicateGroups);
        
        // When
        reportGenerator.generateReport();
        
        // Then
        Path reportsDir = projectPath.resolve("reports");
        Path reportFile = findReportFile(reportsDir);
        
        String filename = reportFile.getFileName().toString();
        assertTrue(filename.startsWith("duplicate-detection-report_"));
        assertTrue(filename.endsWith(".txt"));
        
        // Verify timestamp format (YYYYMMDD_HHMMSS)
        String timestamp = filename.substring("duplicate-detection-report_".length(), filename.length() - 4);
        assertTrue(timestamp.matches("\\d{8}_\\d{6}"), "Timestamp should be in YYYYMMDD_HHMMSS format");
    }

    @Test
    void testReportContentStructure() throws IOException {
        // Given
        MethodInfo method1 = createMethodInfo("TestClass", "testMethod", "src/TestClass.java", 10, 20);
        DuplicateGroup group = new DuplicateGroup(Arrays.asList(method1), 0.8, "SIGNATURE");
        duplicateGroups.add(group);
        
        // Create ReportGenerator after adding groups
        reportGenerator = new ReportGenerator(projectPath, config, duplicateGroups);
        
        // When
        reportGenerator.generateReport();
        
        // Then
        Path reportFile = findReportFile(projectPath.resolve("reports"));
        String reportContent = Files.readString(reportFile);
        
        // Check header
        assertTrue(reportContent.contains("JAVA DUPLICATE DETECTOR - DETAILED ANALYSIS REPORT"));
        assertTrue(reportContent.contains("Generated: "));
        assertTrue(reportContent.contains("Project: "));
        assertTrue(reportContent.contains("Configuration: "));
        
        // Check summary statistics
        assertTrue(reportContent.contains("SUMMARY STATISTICS"));
        assertTrue(reportContent.contains("Total duplicate groups:"));
        assertTrue(reportContent.contains("Total duplicate methods:"));
        
        // Check detailed analysis
        assertTrue(reportContent.contains("DETAILED ANALYSIS"));
        assertTrue(reportContent.contains("Group 1:"));
        assertTrue(reportContent.contains("Methods involved"));
        
        // Check footer
        assertTrue(reportContent.contains("RECOMMENDATIONS AND NEXT STEPS"));
        assertTrue(reportContent.contains("Report generated by Java Duplicate Detector"));
    }

    @Test
    void testReportWithCrossClassDuplication() throws IOException {
        // Given
        MethodInfo method1 = createMethodInfo("Class1", "sameMethod", "src/Class1.java", 10, 20);
        MethodInfo method2 = createMethodInfo("Class2", "sameMethod", "src/Class2.java", 15, 25);
        
        DuplicateGroup group = new DuplicateGroup(Arrays.asList(method1, method2), 1.0, "EXACT");
        duplicateGroups.add(group);
        
        // Create ReportGenerator after adding groups
        reportGenerator = new ReportGenerator(projectPath, config, duplicateGroups);
        
        // When
        reportGenerator.generateReport();
        
        // Then
        Path reportFile = findReportFile(projectPath.resolve("reports"));
        String reportContent = Files.readString(reportFile);
        
        assertTrue(reportContent.contains("Total duplicate groups: 1"));
        assertTrue(reportContent.contains("Total duplicate methods: 2"));
        assertTrue(reportContent.contains("Class1.sameMethod()"));
        assertTrue(reportContent.contains("Class2.sameMethod()"));
        assertTrue(reportContent.contains("100.0"));
        assertTrue(reportContent.contains("Cross-class duplications: 1"));
    }

    @Test
    void testReportWithHighSeverityIssues() throws IOException {
        // Given
        MethodInfo method1 = createMethodInfo("Class1", "method1", "src/Class1.java", 10, 20);
        MethodInfo method2 = createMethodInfo("Class2", "method2", "src/Class2.java", 15, 25);
        
        DuplicateGroup group = new DuplicateGroup(Arrays.asList(method1, method2), 1.0, "EXACT");
        duplicateGroups.add(group);
        
        // Create ReportGenerator after adding groups
        reportGenerator = new ReportGenerator(projectPath, config, duplicateGroups);
        
        // When
        reportGenerator.generateReport();
        
        // Then
        Path reportFile = findReportFile(projectPath.resolve("reports"));
        String reportContent = Files.readString(reportFile);
        
        assertTrue(reportContent.contains("Total duplicate groups: 1"));
        assertTrue(reportContent.contains("Total duplicate methods: 2"));
        assertTrue(reportContent.contains("100.0"));
        assertTrue(reportContent.contains("Group 1:"));
        assertTrue(reportContent.contains("High severity issues: 1"));
    }

    @Test
    void testReportWithCustomConfiguration() throws IOException {
        // Given
        PerformanceConfig customConfig = new PerformanceConfig();
        customConfig.setSimilarityThreshold(0.8);
        customConfig.setMaxParallelThreads(4);
        customConfig.setMinMethodLength(100);
        
        ReportGenerator customReportGenerator = new ReportGenerator(projectPath, customConfig, duplicateGroups);
        
        // When
        customReportGenerator.generateReport();
        
        // Then
        Path reportFile = findReportFile(projectPath.resolve("reports"));
        String reportContent = Files.readString(reportFile);
        
        assertTrue(reportContent.contains("similarityThreshold=0.8"));
        assertTrue(reportContent.contains("maxParallelThreads=4"));
        assertTrue(reportContent.contains("minMethodLength=100"));
    }

    @Test
    void testReportHandlesIOException() {
        // Given - Create a path that will cause IOException when trying to create directory
        Path invalidPath = Path.of("/invalid/path/that/cannot/be/created");
        ReportGenerator invalidReportGenerator = new ReportGenerator(invalidPath, config, duplicateGroups);
        
        // When/Then - Should not throw exception, should handle gracefully
        assertDoesNotThrow(() -> invalidReportGenerator.generateReport());
    }

    @Test
    void testReportWithEmptyProjectPath() throws IOException {
        // Given
        Path emptyPath = tempDir.resolve("empty");
        Files.createDirectories(emptyPath);
        ReportGenerator emptyReportGenerator = new ReportGenerator(emptyPath, config, duplicateGroups);
        
        // When
        emptyReportGenerator.generateReport();
        
        // Then
        Path reportsDir = emptyPath.resolve("reports");
        assertTrue(Files.exists(reportsDir));
        
        Path reportFile = findReportFile(reportsDir);
        assertNotNull(reportFile);
        
        String reportContent = Files.readString(reportFile);
        assertTrue(reportContent.contains("✅ No duplicate methods found!"));
    }

    @Test
    void testReportGroupsSortedBySimilarity() throws IOException {
        // Given - Create groups with different similarity scores
        MethodInfo method1 = createMethodInfo("Class1", "method1", "src/Class1.java", 10, 20);
        MethodInfo method2 = createMethodInfo("Class2", "method2", "src/Class2.java", 15, 25);
        MethodInfo method3 = createMethodInfo("Class3", "method3", "src/Class3.java", 20, 30);
        
        DuplicateGroup lowSimilarityGroup = new DuplicateGroup(Arrays.asList(method1), 0.7, "STRUCTURAL");
        DuplicateGroup highSimilarityGroup = new DuplicateGroup(Arrays.asList(method2, method3), 0.95, "EXACT");
        
        // Add in reverse order to test sorting
        duplicateGroups.add(lowSimilarityGroup);
        duplicateGroups.add(highSimilarityGroup);
        
        // Create ReportGenerator after adding groups
        reportGenerator = new ReportGenerator(projectPath, config, duplicateGroups);
        
        // When
        reportGenerator.generateReport();
        
        // Then
        Path reportFile = findReportFile(projectPath.resolve("reports"));
        String reportContent = Files.readString(reportFile);
        
        assertTrue(reportContent.contains("Total duplicate groups: 2"));
        assertTrue(reportContent.contains("Total duplicate methods: 3"));
        assertTrue(reportContent.contains("95.0"));
        assertTrue(reportContent.contains("70.0"));
        assertTrue(reportContent.contains("Group 1:"));
        assertTrue(reportContent.contains("Group 2:"));
    }

    @Test
    void testReportIncludesMethodDetails() throws IOException {
        // Given
        MethodInfo method = createMethodInfo("TestClass", "testMethod", "src/main/java/TestClass.java", 15, 45);
        DuplicateGroup group = new DuplicateGroup(Arrays.asList(method), 0.8, "SIGNATURE");
        duplicateGroups.add(group);
        
        // Create ReportGenerator after adding groups
        reportGenerator = new ReportGenerator(projectPath, config, duplicateGroups);
        
        // When
        reportGenerator.generateReport();
        
        // Then
        Path reportFile = findReportFile(projectPath.resolve("reports"));
        String reportContent = Files.readString(reportFile);
        
        assertTrue(reportContent.contains("TestClass.testMethod()"));
        assertTrue(reportContent.contains("src/main/java/TestClass.java"));
        assertTrue(reportContent.contains("lines 15-45"));
        assertTrue(reportContent.contains("Total duplicate groups: 1"));
        assertTrue(reportContent.contains("Total duplicate methods: 1"));
    }

    // Helper methods
    private MethodInfo createMethodInfo(String className, String methodName, String filePath, int startLine, int endLine) {
        String signature = "public void " + methodName + "()";
        String body = "public void " + methodName + "() { /* method body */ }";
        Path path = Path.of(filePath);
        String packageName = "com.test";
        
        return new MethodInfo(className, methodName, signature, body, path, startLine, endLine, packageName);
    }

    private Path findReportFile(Path reportsDir) throws IOException {
        return Files.list(reportsDir)
                .filter(path -> path.getFileName().toString().startsWith("duplicate-detection-report_"))
                .findFirst()
                .orElse(null);
    }
}
