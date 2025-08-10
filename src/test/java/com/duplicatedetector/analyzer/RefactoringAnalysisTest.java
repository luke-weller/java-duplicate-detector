package com.duplicatedetector.analyzer;

import com.duplicatedetector.model.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RefactoringAnalysisTest {
    
    private RefactoringAnalysis analysis;
    private MethodInfo testMethod1;
    private MethodInfo testMethod2;
    
    @BeforeEach
    void setUp() {
        analysis = new RefactoringAnalysis();
        
        Path testPath = Paths.get("/test/path/TestClass.java");
        testMethod1 = new MethodInfo("TestClass", "method1", "public void method1()", 
                                    "public void method1() { System.out.println(\"test\"); }", 
                                    testPath, 10, 15, "com.test");
        
        testMethod2 = new MethodInfo("TestClass", "method2", "public void method2()", 
                                    "public void method2() { System.out.println(\"test2\"); }", 
                                    testPath, 20, 25, "com.test");
    }
    
    @Test
    void testDefaultConstructor() {
        assertNotNull(analysis);
        assertNotNull(analysis.getRecommendations());
        assertNotNull(analysis.getInsights());
        assertNotNull(analysis.getCodeQualityInsights());
        assertNotNull(analysis.getCommonParameters());
        assertTrue(analysis.getRecommendations().isEmpty());
        assertTrue(analysis.getInsights().isEmpty());
        assertTrue(analysis.getCodeQualityInsights().isEmpty());
        assertTrue(analysis.getCommonParameters().isEmpty());
    }
    
    @Test
    void testSetAndGetSimilarityType() {
        analysis.setSimilarityType("EXACT");
        assertEquals("EXACT", analysis.getSimilarityType());
    }
    
    @Test
    void testSetAndGetSimilarityScore() {
        analysis.setSimilarityScore(0.85);
        assertEquals(0.85, analysis.getSimilarityScore(), 0.001);
    }
    
    @Test
    void testSetAndGetMethods() {
        List<MethodInfo> methods = List.of(testMethod1, testMethod2);
        analysis.setMethods(methods);
        assertEquals(methods, analysis.getMethods());
    }
    
    @Test
    void testSetAndGetDuplicationType() {
        analysis.setDuplicationType("Exact Code Duplication");
        assertEquals("Exact Code Duplication", analysis.getDuplicationType());
    }
    
    @Test
    void testSetAndGetSeverity() {
        analysis.setSeverity("HIGH");
        assertEquals("HIGH", analysis.getSeverity());
    }
    
    @Test
    void testSetAndGetRecommendations() {
        List<String> recommendations = List.of("Extract method", "Use strategy pattern");
        analysis.setRecommendations(recommendations);
        assertEquals(recommendations, analysis.getRecommendations());
    }
    
    @Test
    void testSetAndGetRefactoringPattern() {
        analysis.setRefactoringPattern("Strategy Pattern");
        assertEquals("Strategy Pattern", analysis.getRefactoringPattern());
    }
    
    @Test
    void testSetAndGetCrossClassIssue() {
        analysis.setCrossClassIssue(true);
        assertTrue(analysis.isCrossClassIssue());
        
        analysis.setCrossClassIssue(false);
        assertFalse(analysis.isCrossClassIssue());
    }
    
    @Test
    void testSetAndGetCrossClassRecommendation() {
        String recommendation = "Create shared utility class";
        analysis.setCrossClassRecommendation(recommendation);
        assertEquals(recommendation, analysis.getCrossClassRecommendation());
    }
    
    @Test
    void testSetAndGetCommonParameters() {
        List<String> params = List.of("String", "int");
        analysis.setCommonParameters(params);
        assertEquals(params, analysis.getCommonParameters());
    }
    
    @Test
    void testSetAndGetInsights() {
        List<String> insights = List.of("Common pattern detected", "Similar structure");
        analysis.setInsights(insights);
        assertEquals(insights, analysis.getInsights());
    }
    
    @Test
    void testSetAndGetCodeQualityInsights() {
        List<String> qualityInsights = List.of("Method too long", "Consider breaking down");
        analysis.setCodeQualityInsights(qualityInsights);
        assertEquals(qualityInsights, analysis.getCodeQualityInsights());
    }
    
    @Test
    void testAddInsight() {
        analysis.addInsight("New insight");
        assertEquals(1, analysis.getInsights().size());
        assertTrue(analysis.getInsights().contains("New insight"));
        
        analysis.addInsight("Another insight");
        assertEquals(2, analysis.getInsights().size());
        assertTrue(analysis.getInsights().contains("Another insight"));
    }
    
    @Test
    void testGetFormattedSimilarityPercentage() {
        analysis.setSimilarityScore(0.756);
        assertEquals("75.6", analysis.getFormattedSimilarityPercentage());
        
        analysis.setSimilarityScore(0.0);
        assertEquals("0.0", analysis.getFormattedSimilarityPercentage());
        
        analysis.setSimilarityScore(1.0);
        assertEquals("100.0", analysis.getFormattedSimilarityPercentage());
    }
    
    @Test
    void testGetMethodCount() {
        assertEquals(0, analysis.getMethodCount());
        
        analysis.setMethods(List.of(testMethod1));
        assertEquals(1, analysis.getMethodCount());
        
        analysis.setMethods(List.of(testMethod1, testMethod2));
        assertEquals(2, analysis.getMethodCount());
    }
    
    @Test
    void testGetUniqueClassCount() {
        assertEquals(0, analysis.getUniqueClassCount());
        
        // Same class
        analysis.setMethods(List.of(testMethod1, testMethod2));
        assertEquals(1, analysis.getUniqueClassCount());
        
        // Different classes
        MethodInfo method3 = new MethodInfo("AnotherClass", "method3", "public void method3()", 
                                           "public void method3() { }", 
                                           Paths.get("/test/path/AnotherClass.java"), 30, 35, "com.test");
        analysis.setMethods(List.of(testMethod1, method3));
        assertEquals(2, analysis.getUniqueClassCount());
    }
    
    @Test
    void testGetMethodCountWithNullMethods() {
        analysis.setMethods(null);
        assertEquals(0, analysis.getMethodCount());
    }
    
    @Test
    void testGetUniqueClassCountWithNullMethods() {
        analysis.setMethods(null);
        assertEquals(0, analysis.getUniqueClassCount());
    }
}
