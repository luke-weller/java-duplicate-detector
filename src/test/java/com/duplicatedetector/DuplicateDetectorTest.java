package com.duplicatedetector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

class DuplicateDetectorTest {

    private DuplicateDetector detector;

    @BeforeEach
    void setUp() {
        detector = new DuplicateDetector();
    }

    @Test
    void testRunWithValidProject(@TempDir Path tempDir) throws IOException {
        // Create a test project with duplicate methods
        createTestProject(tempDir);
        
        // Capture console output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            detector.run(tempDir);
            
            String output = outputStream.toString();
            
            // Verify that the tool found duplicate methods
            assertTrue(output.contains("Java Duplicate Detector - Detailed Analysis"));
            assertTrue(output.contains("Found") && output.contains("groups of similar methods"));
            
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRunWithNoJavaFiles(@TempDir Path tempDir) throws IOException {
        // Create a directory with no Java files
        Path textFile = tempDir.resolve("readme.txt");
        Files.write(textFile, "This is a readme file".getBytes());
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            detector.run(tempDir);
            
            String output = outputStream.toString();
            assertTrue(output.contains("No Java files found"));
            
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRunWithNonExistentPath() {
        Path nonExistentPath = Path.of("/non/existent/path");
        
        // Test that the method throws an exception for non-existent paths
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            detector.run(nonExistentPath);
        });
        
        assertTrue(exception.getMessage().contains("Failed to analyze project"));
        assertTrue(exception.getCause().getMessage().contains("Project path does not exist"));
    }

    @Test
    void testRunWithEmptyJavaFiles(@TempDir Path tempDir) throws IOException {
        // Create Java files with no methods
        Path javaFile1 = tempDir.resolve("EmptyClass1.java");
        Files.write(javaFile1, "public class EmptyClass1 { }".getBytes());
        
        Path javaFile2 = tempDir.resolve("EmptyClass2.java");
        Files.write(javaFile2, "public class EmptyClass2 { }".getBytes());
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            detector.run(tempDir);
            
            String output = outputStream.toString();
            assertTrue(output.contains("No methods found"));
            
        } finally {
            System.setOut(originalOut);
        }
    }

    private void createTestProject(Path projectDir) throws IOException {
        // Create source directory structure
        Path srcDir = projectDir.resolve("src").resolve("main").resolve("java");
        Files.createDirectories(srcDir);
        
        // Create first class with duplicate methods
        String class1Code = "package com.test;\n\n" +
            "public class DataProcessor {\n" +
            "    public void processData(int[] data) {\n" +
            "        for (int i = 0; i < data.length; i++) {\n" +
            "            data[i] = data[i] * 2;\n" +
            "        }\n" +
            "    }\n\n" +
            "    public void calculateSum(int[] data) {\n" +
            "        int sum = 0;\n" +
            "        for (int i = 0; i < data.length; i++) {\n" +
            "            sum += data[i];\n" +
            "        }\n" +
            "        System.out.println(\"Sum: \" + sum);\n" +
            "    }\n" +
            "}\n";
        
        Path class1File = srcDir.resolve("DataProcessor.java");
        Files.write(class1File, class1Code.getBytes());
        
        // Create second class with similar methods
        String class2Code = "package com.test;\n\n" +
            "public class ArrayProcessor {\n" +
            "    public void processData(int[] data) {\n" +
            "        for (int i = 0; i < data.length; i++) {\n" +
            "            data[i] = data[i] * 3;\n" +
            "        }\n" +
            "    }\n\n" +
            "    public void calculateSum(int[] data) {\n" +
            "        int total = 0;\n" +
            "        for (int i = 0; i < data.length; i++) {\n" +
            "            total += data[i];\n" +
            "        }\n" +
            "        System.out.println(\"Total: \" + total);\n" +
            "    }\n" +
            "}\n";
        
        Path class2File = srcDir.resolve("ArrayProcessor.java");
        Files.write(class2File, class2Code.getBytes());
    }
}
