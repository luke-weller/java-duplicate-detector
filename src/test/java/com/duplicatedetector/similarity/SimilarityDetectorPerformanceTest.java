package com.duplicatedetector.similarity;

import com.duplicatedetector.model.MethodInfo;
import com.duplicatedetector.model.DuplicateGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

class SimilarityDetectorPerformanceTest {

    private SimilarityDetector detector;

    @BeforeEach
    void setUp() {
        detector = new SimilarityDetector();
    }

    @Test
    void testParallelProcessingWithManyMethods() {
        // Create a large number of methods to test parallel processing
        List<MethodInfo> methods = new ArrayList<>();
        
        // Create 20 methods with similar patterns
        for (int i = 0; i < 20; i++) {
            String body = "public void processData" + i + "(int[] data) {\n" +
                         "    for (int j = 0; j < data.length; j++) {\n" +
                         "        data[j] = data[j] * " + (i + 1) + ";\n" +
                         "    }\n" +
                         "}";
            methods.add(createMethodInfo("method" + i, body));
        }
        
        long startTime = System.currentTimeMillis();
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods, 0.7);
        long endTime = System.currentTimeMillis();
        
        // Verify that parallel processing completed successfully
        assertNotNull(groups);
        assertTrue(endTime - startTime < 10000); // Should complete within 10 seconds
        
        // Should find some duplicate groups
        assertTrue(groups.size() > 0);
    }

    @Test
    void testCachingBehavior() {
        MethodInfo method1 = createMethodInfo("method1", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; } }");
        MethodInfo method2 = createMethodInfo("method2", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 3; } }");
        
        List<MethodInfo> methods = List.of(method1, method2);
        
        // First run
        long startTime1 = System.currentTimeMillis();
        List<DuplicateGroup> groups1 = detector.findSimilarMethods(methods, 0.7);
        long endTime1 = System.currentTimeMillis();
        long time1 = endTime1 - startTime1;
        
        // Second run (should use cache)
        long startTime2 = System.currentTimeMillis();
        List<DuplicateGroup> groups2 = detector.findSimilarMethods(methods, 0.7);
        long endTime2 = System.currentTimeMillis();
        long time2 = endTime2 - startTime2;
        
        // Results should be identical
        assertEquals(groups1.size(), groups2.size());
        if (groups1.size() > 0) {
            assertEquals(groups1.get(0).getSimilarityScore(), groups2.get(0).getSimilarityScore(), 0.001);
        }
        
        // Second run should be faster due to caching (though timing can be variable)
        assertTrue(time2 <= time1 + 10); // Allow small timing variations
    }

    @Test
    void testMethodGroupingOptimization() {
        // Create methods with different characteristics to test grouping
        List<MethodInfo> methods = new ArrayList<>();
        
        // Methods with same parameter count and similar length
        methods.add(createMethodInfo("method1", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; } }"));
        methods.add(createMethodInfo("method2", 
            "public void processData(int[] data) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * 3; } }"));
        
        // Methods with different parameter count
        methods.add(createMethodInfo("method3", 
            "public void processData(int[] data, int factor) { for (int i = 0; i < data.length; i++) { data[i] = data[i] * factor; } }"));
        
        // Methods with very different length
        methods.add(createMethodInfo("method4", 
            "public void simpleMethod() { return; }"));
        
        long startTime = System.currentTimeMillis();
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods, 0.7);
        long endTime = System.currentTimeMillis();
        
        // Should complete quickly due to optimization
        assertTrue(endTime - startTime < 5000);
        assertNotNull(groups);
    }

    @Test
    void testEarlyFilteringBehavior() {
        // Create methods that should be filtered out early
        List<MethodInfo> methods = new ArrayList<>();
        
        // Very short method
        methods.add(createMethodInfo("shortMethod", "return 0;"));
        
        // Method with very different length
        methods.add(createMethodInfo("longMethod", 
            "public void veryLongMethod(int[] data) {\n" +
            "    for (int i = 0; i < data.length; i++) {\n" +
            "        for (int j = 0; j < data.length; j++) {\n" +
            "            for (int k = 0; k < data.length; k++) {\n" +
            "                data[i] = data[i] * data[j] * data[k];\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}"));
        
        long startTime = System.currentTimeMillis();
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods, 0.7);
        long endTime = System.currentTimeMillis();
        
        // Should complete very quickly due to early filtering
        assertTrue(endTime - startTime < 1000);
        assertNotNull(groups);
    }

    @Test
    void testFastStructuralSimilarityForLargeMethods() {
        // Create very large method bodies to test the fast similarity algorithm
        StringBuilder largeBody1 = new StringBuilder();
        StringBuilder largeBody2 = new StringBuilder();
        
        // Create large method bodies with similar structure
        for (int i = 0; i < 1000; i++) {
            largeBody1.append("data[").append(i).append("] = data[").append(i).append("] * 2; ");
            largeBody2.append("data[").append(i).append("] = data[").append(i).append("] * 3; ");
        }
        
        MethodInfo method1 = createMethodInfo("largeMethod1", 
            "public void processLargeData(int[] data) { " + largeBody1.toString() + " }");
        MethodInfo method2 = createMethodInfo("largeMethod2", 
            "public void processLargeData(int[] data) { " + largeBody2.toString() + " }");
        
        List<MethodInfo> methods = List.of(method1, method2);
        
        long startTime = System.currentTimeMillis();
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods, 0.7);
        long endTime = System.currentTimeMillis();
        
        // Should complete quickly even with large methods
        assertTrue(endTime - startTime < 5000);
        assertNotNull(groups);
        
        // Should find similarity due to similar structure
        if (groups.size() > 0) {
            assertTrue(groups.get(0).getSimilarityScore() > 0.5);
        }
    }

    @Test
    void testSamplingForLargeGroups() {
        // Create many similar methods to test sampling behavior
        List<MethodInfo> methods = new ArrayList<>();
        
        for (int i = 0; i < 50; i++) {
            String body = "public void processData" + i + "(int[] data) {\n" +
                         "    for (int j = 0; j < data.length; j++) {\n" +
                         "        data[j] = data[j] * " + (i % 5 + 1) + ";\n" +
                         "    }\n" +
                         "}";
            methods.add(createMethodInfo("method" + i, body));
        }
        
        long startTime = System.currentTimeMillis();
        List<DuplicateGroup> groups = detector.findSimilarMethods(methods, 0.7);
        long endTime = System.currentTimeMillis();
        
        // Should complete quickly due to sampling
        assertTrue(endTime - startTime < 10000);
        assertNotNull(groups);
        
        // Should find some groups
        assertTrue(groups.size() > 0);
    }

    @Test
    void testConcurrentExecutionSafety() {
        // Test that parallel processing is thread-safe
        List<MethodInfo> methods = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            String body = "public void processData" + i + "(int[] data) {\n" +
                         "    for (int j = 0; j < data.length; j++) {\n" +
                         "        data[j] = data[j] * " + (i + 1) + ";\n" +
                         "    }\n" +
                         "}";
            methods.add(createMethodInfo("method" + i, body));
        }
        
        // Run multiple times concurrently to test thread safety
        AtomicReference<List<DuplicateGroup>> results1 = new AtomicReference<>();
        AtomicReference<List<DuplicateGroup>> results2 = new AtomicReference<>();
        
        Thread thread1 = new Thread(() -> {
            results1.set(detector.findSimilarMethods(methods, 0.7));
        });
        
        Thread thread2 = new Thread(() -> {
            results2.set(detector.findSimilarMethods(methods, 0.7));
        });
        
        thread1.start();
        thread2.start();
        
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            fail("Thread execution was interrupted");
        }
        
        // Both results should be valid
        assertNotNull(results1.get());
        assertNotNull(results2.get());
        assertEquals(results1.get().size(), results2.get().size());
    }

    @Test
    void testPerformanceWithDifferentThresholds() {
        List<MethodInfo> methods = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            String body = "public void processData" + i + "(int[] data) {\n" +
                         "    for (int j = 0; j < data.length; j++) {\n" +
                         "        data[j] = data[j] * " + (i + 1) + ";\n" +
                         "    }\n" +
                         "}";
            methods.add(createMethodInfo("method" + i, body));
        }
        
        // Test with different thresholds
        long time1 = measureExecutionTime(() -> detector.findSimilarMethods(methods, 0.5));
        long time2 = measureExecutionTime(() -> detector.findSimilarMethods(methods, 0.7));
        long time3 = measureExecutionTime(() -> detector.findSimilarMethods(methods, 0.9));
        
        // All should complete within reasonable time
        assertTrue(time1 < 5000);
        assertTrue(time2 < 5000);
        assertTrue(time3 < 5000);
    }

    private long measureExecutionTime(Runnable task) {
        long startTime = System.currentTimeMillis();
        task.run();
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    private MethodInfo createMethodInfo(String methodName, String body) {
        return new MethodInfo(
            "TestClass", 
            methodName, 
            "public void " + methodName + "(int[] data)", 
            body, 
            Paths.get("TestClass.java"), 
            1, 
            10, 
            "com.test"
        );
    }
}
