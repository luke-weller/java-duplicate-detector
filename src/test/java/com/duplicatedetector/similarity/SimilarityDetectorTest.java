package com.duplicatedetector.similarity;

import com.duplicatedetector.model.MethodInfo;
import com.duplicatedetector.model.DuplicateGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class SimilarityDetectorTest {

    private SimilarityDetector detector;

    @BeforeEach
    void setUp() {
        detector = new SimilarityDetector();
    }

    @Test
    void testFindSimilarMethodsWithExactDuplicates() {
        MethodInfo method1 = createMethodInfo("method1", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; } }");
        MethodInfo method2 = createMethodInfo("method2", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; } }");
        
        List<MethodInfo> methods = List.of(method1, method2);
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods, 0.8);
        
        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).getGroupSize());
        assertTrue(groups.get(0).getSimilarityScore() > 0.9);
    }

    @Test
    void testFindSimilarMethodsWithSimilarLogic() {
        MethodInfo method1 = createMethodInfo("method1", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; } }");
        MethodInfo method2 = createMethodInfo("method2", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 3; } }");
        
        List<MethodInfo> methods = List.of(method1, method2);
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods, 0.7);
        
        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).getGroupSize());
        assertTrue(groups.get(0).getSimilarityScore() > 0.7);
    }

    @Test
    void testFindSimilarMethodsWithDifferentLogic() {
        MethodInfo method1 = createMethodInfo("method1", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; } }");
        MethodInfo method2 = createMethodInfo("method2", 
            "public void calculateSum(int[] data) { int sum = 0; for (int i = 0; i < data.length; i++) { sum += data[i]; } return sum; }");
        
        List<MethodInfo> methods = List.of(method1, method2);
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods, 0.8);
        
        assertEquals(0, groups.size());
    }

    @Test
    void testFindSimilarMethodsWithShortMethods() {
        MethodInfo shortMethod1 = createMethodInfo("short1", "return 0;");
        MethodInfo shortMethod2 = createMethodInfo("short2", "return 1;");
        
        List<MethodInfo> methods = List.of(shortMethod1, shortMethod2);
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods);
        
        assertEquals(0, groups.size());
    }

    @Test
    void testFindSimilarMethodsWithMultipleGroups() {
        MethodInfo method1 = createMethodInfo("method1", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; } }");
        MethodInfo method2 = createMethodInfo("method2", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 3; } }");
        MethodInfo method3 = createMethodInfo("method3", 
            "public void calculateSum(int[] data) { int sum = 0; for (int i = 0; i < data.length; i++) { sum += data[i]; } }");
        MethodInfo method4 = createMethodInfo("method4", 
            "public void calculateSum(int[] data) { int total = 0; for (int i = 0; i < data.length; i++) { total += data[i]; } }");
        
        List<MethodInfo> methods = List.of(method1, method2, method3, method4);
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods, 0.7);
        
        assertEquals(2, groups.size());
        
        // Check that each group has 2 methods
        assertEquals(2, groups.get(0).getGroupSize());
        assertEquals(2, groups.get(1).getGroupSize());
    }

    @Test
    void testFindSimilarMethodsWithHighThreshold() {
        MethodInfo method1 = createMethodInfo("method1", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; } }");
        MethodInfo method2 = createMethodInfo("method2", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 3; } }");
        
        List<MethodInfo> methods = List.of(method1, method2);
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods, 0.95);
        
        assertEquals(0, groups.size());
    }

    @Test
    void testFindSimilarMethodsWithLowThreshold() {
        MethodInfo method1 = createMethodInfo("method1", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; } }");
        MethodInfo method2 = createMethodInfo("method2", 
            "public void calculateSum(int[] data) { int sum = 0; for (int i = 0; i < data.length; i++) { sum += data[i]; } }");
        
        List<MethodInfo> methods = List.of(method1, method2);
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods, 0.3);
        
        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).getGroupSize());
    }

    @Test
    void testFindSimilarMethodsWithEmptyList() {
        List<MethodInfo> methods = List.of();
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods);
        
        assertEquals(0, groups.size());
    }

    @Test
    void testFindSimilarMethodsWithSingleMethod() {
        MethodInfo method1 = createMethodInfo("method1", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; } }");
        
        List<MethodInfo> methods = List.of(method1);
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods);
        
        assertEquals(0, groups.size());
    }

    @Test
    void testFindSimilarMethodsWithDefaultThreshold() {
        MethodInfo method1 = createMethodInfo("method1", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; } }");
        MethodInfo method2 = createMethodInfo("method2", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 3; } }");
        
        List<MethodInfo> methods = List.of(method1, method2);
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods);
        
        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).getGroupSize());
    }

    private MethodInfo createMethodInfo(String methodName, String body) {
        return new MethodInfo(
            "TestClass", methodName, "public void " + methodName + "(int[] data)",
            body, Paths.get("TestClass.java"), 1, 5, "com.test"
        );
    }
}
