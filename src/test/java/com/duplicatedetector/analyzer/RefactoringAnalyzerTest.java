package com.duplicatedetector.analyzer;

import com.duplicatedetector.model.MethodInfo;
import com.duplicatedetector.model.DuplicateGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RefactoringAnalyzerTest {
    
    private RefactoringAnalyzer analyzer;
    private MethodInfo method1;
    private MethodInfo method2;
    private MethodInfo method3;
    
    @BeforeEach
    void setUp() {
        analyzer = new RefactoringAnalyzer();
        
        Path testPath1 = Paths.get("/test/path/Class1.java");
        Path testPath2 = Paths.get("/test/path/Class2.java");
        
        method1 = new MethodInfo("Class1", "processData", "public void processData(int[] data)", 
                                "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; } }", 
                                testPath1, 10, 15, "com.test");
        
        method2 = new MethodInfo("Class2", "processData", "public void processData(int[] data)", 
                                "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; } }", 
                                testPath2, 20, 25, "com.test");
        
        method3 = new MethodInfo("Class1", "calculateSum", "public int calculateSum(int[] data)", 
                                "public int calculateSum(int[] data) { int sum = 0; for (int i = 0; i < data.length; i++) { sum += data[i]; } return sum; }", 
                                testPath1, 30, 35, "com.test");
    }
    
    @Test
    void testAnalyzeExactDuplication() {
        // Create a duplicate group with identical methods
        DuplicateGroup group = new DuplicateGroup(List.of(method1, method2), 1.0, "EXACT");
        
        RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
        
        assertEquals("EXACT", analysis.getSimilarityType());
        assertEquals(1.0, analysis.getSimilarityScore(), 0.001);
        assertEquals("Exact Code Duplication", analysis.getDuplicationType());
        assertEquals("HIGH", analysis.getSeverity());
        assertEquals("Extract Method + Strategy Pattern", analysis.getRefactoringPattern());
        assertTrue(analysis.isCrossClassIssue());
        assertEquals("Consider creating a shared utility class or service", analysis.getCrossClassRecommendation());
        
        // Check recommendations
        List<String> recommendations = analysis.getRecommendations();
        assertTrue(recommendations.contains("Extract the identical code into a shared utility method"));
        assertTrue(recommendations.contains("Consider creating a common base class or interface"));
        assertTrue(recommendations.contains("Apply the Template Method pattern if the methods follow similar algorithms"));
        assertTrue(recommendations.contains("Use composition over inheritance to share common functionality"));
    }
    
    @Test
    void testAnalyzeSignatureSimilarity() {
        // Create methods with similar signatures but different implementations
        MethodInfo sigMethod1 = new MethodInfo("Class1", "validate", "public boolean validate(String input)", 
                                              "public boolean validate(String input) { return input != null && !input.isEmpty(); }", 
                                              Paths.get("/test/path/Class1.java"), 40, 45, "com.test");
        
        MethodInfo sigMethod2 = new MethodInfo("Class2", "validate", "public boolean validate(String input)", 
                                              "public boolean validate(String input) { return input != null && input.length() > 0; }", 
                                              Paths.get("/test/path/Class2.java"), 50, 55, "com.test");
        
        DuplicateGroup group = new DuplicateGroup(List.of(sigMethod1, sigMethod2), 0.8, "SIGNATURE");
        
        RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
        
        assertEquals("SIGNATURE", analysis.getSimilarityType());
        assertEquals("Signature Similarity", analysis.getDuplicationType());
        assertEquals("MEDIUM", analysis.getSeverity());
        assertEquals("Strategy Pattern + Interface Segregation", analysis.getRefactoringPattern());
        
        // Check recommendations
        List<String> recommendations = analysis.getRecommendations();
        assertTrue(recommendations.contains("Create a common interface defining the contract"));
        assertTrue(recommendations.contains("Apply the Strategy pattern to handle different implementations"));
        assertTrue(recommendations.contains("Consider using generics to make the methods more flexible"));
        assertTrue(recommendations.contains("Extract common parameter validation logic"));
    }
    
    @Test
    void testAnalyzeStructuralSimilarity() {
        // Create methods with similar structure but different content
        MethodInfo structMethod1 = new MethodInfo("Class1", "processItems", "public void processItems(List<String> items)", 
                                                 "public void processItems(List<String> items) { for (String item : items) { if (item != null) { process(item); } } }", 
                                                 Paths.get("/test/path/Class1.java"), 60, 65, "com.test");
        
        MethodInfo structMethod2 = new MethodInfo("Class2", "validateItems", "public void validateItems(List<String> items)", 
                                                 "public void validateItems(List<String> items) { for (String item : items) { if (item != null) { validate(item); } } }", 
                                                 Paths.get("/test/path/Class2.java"), 70, 75, "com.test");
        
        DuplicateGroup group = new DuplicateGroup(List.of(structMethod1, structMethod2), 0.75, "STRUCTURAL");
        
        RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
        
        assertEquals("STRUCTURAL", analysis.getSimilarityType());
        assertEquals("Structural Similarity", analysis.getDuplicationType());
        assertEquals("MEDIUM", analysis.getSeverity());
        assertEquals("Template Method Pattern", analysis.getRefactoringPattern());
        
        // Check recommendations
        List<String> recommendations = analysis.getRecommendations();
        assertTrue(recommendations.contains("Extract common control flow into a template method"));
        assertTrue(recommendations.contains("Apply the Template Method pattern"));
        assertTrue(recommendations.contains("Use the Command pattern for similar operations"));
        assertTrue(recommendations.contains("Consider using the Builder pattern for complex object construction"));
    }
    
    @Test
    void testAnalyzeGenericSimilarity() {
        // Create methods with generic similarity
        DuplicateGroup group = new DuplicateGroup(List.of(method1, method3), 0.6, "GENERIC");
        
        RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
        
        assertEquals("GENERIC", analysis.getSimilarityType());
        assertEquals("Generic Similarity", analysis.getDuplicationType());
        assertEquals("LOW", analysis.getSeverity());
        assertEquals("Review and Refactor", analysis.getRefactoringPattern());
        
        // Check recommendations
        List<String> recommendations = analysis.getRecommendations();
        assertTrue(recommendations.contains("Review the methods for potential abstraction opportunities"));
        assertTrue(recommendations.contains("Consider if the similarity indicates a missing abstraction"));
        assertTrue(recommendations.contains("Look for opportunities to apply common design patterns"));
    }
    
    @Test
    void testAnalyzeSingleMethod() {
        // Test with a single method (should not be considered duplicate)
        DuplicateGroup group = new DuplicateGroup(List.of(method1), 1.0, "EXACT");
        
        RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
        
        assertEquals("EXACT", analysis.getSimilarityType());
        assertEquals(1.0, analysis.getSimilarityScore(), 0.001);
        assertEquals(1, analysis.getMethodCount());
        assertEquals(1, analysis.getUniqueClassCount());
    }
    
    @Test
    void testAnalyzeCrossClassDuplication() {
        // Test cross-class duplication detection
        DuplicateGroup group = new DuplicateGroup(List.of(method1, method2), 0.9, "EXACT");
        
        RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
        
        assertTrue(analysis.isCrossClassIssue());
        assertEquals("Consider creating a shared utility class or service", analysis.getCrossClassRecommendation());
        assertEquals(2, analysis.getUniqueClassCount());
    }
    
    @Test
    void testAnalyzeSameClassDuplication() {
        // Test same-class duplication
        MethodInfo sameClassMethod = new MethodInfo("Class1", "processData2", "public void processData2(int[] data)", 
                                                   "public void processData2(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; } }", 
                                                   Paths.get("/test/path/Class1.java"), 40, 45, "com.test");
        
        DuplicateGroup group = new DuplicateGroup(List.of(method1, sameClassMethod), 0.95, "EXACT");
        
        RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
        
        assertFalse(analysis.isCrossClassIssue());
        assertEquals(1, analysis.getUniqueClassCount());
    }
    
    @Test
    void testAnalyzeLongMethods() {
        // Create a very long method
        StringBuilder longBody = new StringBuilder("public void veryLongMethod() { ");
        for (int i = 0; i < 200; i++) {
            longBody.append("System.out.println(\"line ").append(i).append("\"); ");
        }
        longBody.append("}");
        
        MethodInfo longMethod = new MethodInfo("Class1", "veryLongMethod", "public void veryLongMethod()", 
                                              longBody.toString(), 
                                              Paths.get("/test/path/Class1.java"), 50, 55, "com.test");
        
        DuplicateGroup group = new DuplicateGroup(List.of(longMethod, method1), 0.7, "STRUCTURAL");
        
        RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
        
        List<String> qualityInsights = analysis.getCodeQualityInsights();
        assertTrue(qualityInsights.stream().anyMatch(insight -> insight.contains("veryLongMethod") && insight.contains("quite long")));
    }
    
    @Test
    void testAnalyzeMethodsWithSameName() {
        // Test methods with identical names
        MethodInfo sameNameMethod = new MethodInfo("Class2", "processData", "public void processData(String data)", 
                                                  "public void processData(String data) { System.out.println(data); }", 
                                                  Paths.get("/test/path/Class2.java"), 60, 65, "com.test");
        
        DuplicateGroup group = new DuplicateGroup(List.of(method1, sameNameMethod), 0.6, "GENERIC");
        
        RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
        
        List<String> qualityInsights = analysis.getCodeQualityInsights();
        assertTrue(qualityInsights.stream().anyMatch(insight -> insight.contains("identical names")));
    }
    
    @Test
    void testAnalyzeParameterPatterns() {
        // Test methods with common parameter types
        MethodInfo paramMethod1 = new MethodInfo("Class1", "process", "public void process(String input, int count)", 
                                                "public void process(String input, int count) { }", 
                                                Paths.get("/test/path/Class1.java"), 70, 75, "com.test");
        
        MethodInfo paramMethod2 = new MethodInfo("Class2", "validate", "public boolean validate(String input, int maxLength)", 
                                                "public boolean validate(String input, int maxLength) { return input.length() <= maxLength; }", 
                                                Paths.get("/test/path/Class2.java"), 80, 85, "com.test");
        
        DuplicateGroup group = new DuplicateGroup(List.of(paramMethod1, paramMethod2), 0.7, "SIGNATURE");
        
        RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
        
        List<String> commonParams = analysis.getCommonParameters();
        assertTrue(commonParams.contains("String"));
        assertTrue(commonParams.contains("int"));
        
        List<String> insights = analysis.getInsights();
        assertTrue(insights.stream().anyMatch(insight -> insight.contains("Common parameter types detected")));
    }
    
    @Test
    void testAnalyzeControlFlowPatterns() {
        // Test methods with similar control flow
        MethodInfo controlMethod1 = new MethodInfo("Class1", "process", "public void process(List<String> items)", 
                                                  "public void process(List<String> items) { for (String item : items) { if (item != null) { processItem(item); } } }", 
                                                  Paths.get("/test/path/Class1.java"), 90, 95, "com.test");
        
        MethodInfo controlMethod2 = new MethodInfo("Class2", "validate", "public void validate(List<String> items)", 
                                                  "public void validate(List<String> items) { for (String item : items) { if (item != null) { validateItem(item); } } }", 
                                                  Paths.get("/test/path/Class2.java"), 100, 105, "com.test");
        
        DuplicateGroup group = new DuplicateGroup(List.of(controlMethod1, controlMethod2), 0.8, "STRUCTURAL");
        
        RefactoringAnalysis analysis = analyzer.analyzeDuplicateGroup(group);
        
        List<String> insights = analysis.getInsights();
        assertTrue(insights.stream().anyMatch(insight -> insight.contains("loops pattern detected")));
        assertTrue(insights.stream().anyMatch(insight -> insight.contains("conditionals pattern detected")));
        assertTrue(insights.stream().anyMatch(insight -> insight.contains("method calls pattern detected")));
    }
}
