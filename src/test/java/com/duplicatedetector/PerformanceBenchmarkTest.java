package com.duplicatedetector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

class PerformanceBenchmarkTest {

    @BeforeEach
    void setUp() {
        // Reset any static state if needed
    }

    @Test
    void testRunBenchmarkWithValidProject(@TempDir Path tempDir) throws Exception {
        // Create a test project with some Java files
        createTestProject(tempDir);
        
        // Capture console output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            PerformanceBenchmark.runBenchmark(tempDir);
            
            String output = outputStream.toString();
            
            // Verify benchmark output contains expected sections
            assertTrue(output.contains("=== Performance Benchmark ==="));
            assertTrue(output.contains("Testing configuration: Default"));
            assertTrue(output.contains("Testing configuration: High Performance"));
            assertTrue(output.contains("Testing configuration: High Accuracy"));
            assertTrue(output.contains("Testing configuration: Memory Constrained"));
            assertTrue(output.contains("=== Benchmark Complete ==="));
            
            // Verify performance metrics are present
            assertTrue(output.contains("Performance Metrics:"));
            assertTrue(output.contains("Total time:"));
            assertTrue(output.contains("Files processed per second:"));
            assertTrue(output.contains("Methods analyzed per second:"));
            assertTrue(output.contains("Similarity comparisons per second:"));
            assertTrue(output.contains("Estimated memory usage:"));
            
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRunBenchmarkWithNoJavaFiles(@TempDir Path tempDir) throws Exception {
        // Create a directory with no Java files
        Path textFile = tempDir.resolve("readme.txt");
        Files.write(textFile, "This is a readme file".getBytes());
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            PerformanceBenchmark.runBenchmark(tempDir);
            
            String output = outputStream.toString();
            
            // Should still run the benchmark but skip configurations
            assertTrue(output.contains("=== Performance Benchmark ==="));
            assertTrue(output.contains("=== Benchmark Complete ==="));
            
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRunBenchmarkWithEmptyJavaFiles(@TempDir Path tempDir) throws Exception {
        // Create Java files with no methods
        Path javaFile1 = tempDir.resolve("EmptyClass1.java");
        Files.write(javaFile1, "public class EmptyClass1 { }".getBytes());
        
        Path javaFile2 = tempDir.resolve("EmptyClass2.java");
        Files.write(javaFile2, "public class EmptyClass2 { }".getBytes());
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            PerformanceBenchmark.runBenchmark(tempDir);
            
            String output = outputStream.toString();
            
            // Should still run the benchmark but skip configurations
            assertTrue(output.contains("=== Performance Benchmark ==="));
            assertTrue(output.contains("=== Benchmark Complete ==="));
            
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRunBenchmarkWithLargeProject(@TempDir Path tempDir) throws Exception {
        // Create a larger test project with multiple files and methods
        createLargeTestProject(tempDir);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            PerformanceBenchmark.runBenchmark(tempDir);
            
            String output = outputStream.toString();
            
            // Verify all configurations were tested
            assertTrue(output.contains("Testing configuration: Default"));
            assertTrue(output.contains("Testing configuration: High Performance"));
            assertTrue(output.contains("Testing configuration: High Accuracy"));
            assertTrue(output.contains("Testing configuration: Memory Constrained"));
            
            // Verify performance metrics are calculated
            assertTrue(output.contains("Performance Metrics:"));
            assertTrue(output.contains("Total time:"));
            
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRunBenchmarkWithStringPath(@TempDir Path tempDir) throws Exception {
        // Create a test project
        createTestProject(tempDir);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            PerformanceBenchmark.runBenchmark(tempDir.toString());
            
            String output = outputStream.toString();
            
            // Verify benchmark ran successfully
            assertTrue(output.contains("=== Performance Benchmark ==="));
            assertTrue(output.contains("=== Benchmark Complete ==="));
            
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testMainMethodWithValidPath(@TempDir Path tempDir) throws Exception {
        // Create a test project
        createTestProject(tempDir);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            PerformanceBenchmark.main(new String[]{tempDir.toString()});
            
            String output = outputStream.toString();
            
            // Verify benchmark ran successfully
            assertTrue(output.contains("=== Performance Benchmark ==="));
            assertTrue(output.contains("=== Benchmark Complete ==="));
            
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testMainMethodWithNoArguments() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            PerformanceBenchmark.main(new String[]{});
            
            String output = outputStream.toString();
            
            // Should show usage message
            assertTrue(output.contains("Please provide a project path as argument"));
            assertTrue(output.contains("Usage: java PerformanceBenchmark <project-path>"));
            
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testMainMethodWithInvalidPath() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            PerformanceBenchmark.main(new String[]{"/non/existent/path"});
            
            String output = outputStream.toString();
            
            // Should show error message
            assertTrue(output.contains("Benchmark failed"));
            
        } finally {
            System.setOut(originalOut);
        }
    }

    private void createTestProject(Path projectDir) throws Exception {
        // Create a simple test project with duplicate methods
        Path javaFile1 = projectDir.resolve("TestClass1.java");
        String code1 = "public class TestClass1 {\n" +
                      "    public void processData(int[] data) {\n" +
                      "        for (int i = 0; i < data.length; i++) {\n" +
                      "            data[i] = data[i] * 2;\n" +
                      "        }\n" +
                      "    }\n" +
                      "}";
        Files.write(javaFile1, code1.getBytes());
        
        Path javaFile2 = projectDir.resolve("TestClass2.java");
        String code2 = "public class TestClass2 {\n" +
                      "    public void processData(int[] data) {\n" +
                      "        for (int i = 0; i < data.length; i++) {\n" +
                      "            data[i] = data[i] * 3;\n" +
                      "        }\n" +
                      "    }\n" +
                      "}";
        Files.write(javaFile2, code2.getBytes());
    }

    private void createLargeTestProject(Path projectDir) throws Exception {
        // Create a larger test project with multiple files
        for (int i = 1; i <= 5; i++) {
            Path javaFile = projectDir.resolve("TestClass" + i + ".java");
            String code = "public class TestClass" + i + " {\n" +
                         "    public void method" + i + "1(int[] data) {\n" +
                         "        for (int j = 0; j < data.length; j++) {\n" +
                         "            data[j] = data[j] * " + i + ";\n" +
                         "        }\n" +
                         "    }\n" +
                         "    public void method" + i + "2(int[] data) {\n" +
                         "        for (int j = 0; j < data.length; j++) {\n" +
                         "            data[j] = data[j] + " + i + ";\n" +
                         "        }\n" +
                         "    }\n" +
                         "}";
            Files.write(javaFile, code.getBytes());
        }
    }
}
