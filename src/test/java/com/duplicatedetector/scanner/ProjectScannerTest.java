package com.duplicatedetector.scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class ProjectScannerTest {

    private ProjectScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new ProjectScanner();
    }

    @Test
    void testScanProjectWithValidDirectory(@TempDir Path tempDir) throws IOException {
        // Create test Java files
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);
        
        Path javaFile1 = srcDir.resolve("TestClass1.java");
        Files.write(javaFile1, "public class TestClass1 { }".getBytes());
        
        Path javaFile2 = srcDir.resolve("TestClass2.java");
        Files.write(javaFile2, "public class TestClass2 { }".getBytes());
        
        // Create a non-Java file
        Path textFile = srcDir.resolve("readme.txt");
        Files.write(textFile, "This is a readme".getBytes());

        List<Path> javaFiles = scanner.scanProject(tempDir);
        
        assertEquals(2, javaFiles.size());
        assertTrue(javaFiles.contains(javaFile1));
        assertTrue(javaFiles.contains(javaFile2));
        assertFalse(javaFiles.contains(textFile));
    }

    @Test
    void testScanProjectExcludesBuildDirectories(@TempDir Path tempDir) throws IOException {
        // Create source directory
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);
        Path javaFile = srcDir.resolve("TestClass.java");
        Files.write(javaFile, "public class TestClass { }".getBytes());
        
        // Create build directories that should be excluded
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
        Path targetJavaFile = targetDir.resolve("CompiledClass.java");
        Files.write(targetJavaFile, "public class CompiledClass { }".getBytes());
        
        Path buildDir = tempDir.resolve("build");
        Files.createDirectories(buildDir);
        Path buildJavaFile = buildDir.resolve("BuiltClass.java");
        Files.write(buildJavaFile, "public class BuiltClass { }".getBytes());

        List<Path> javaFiles = scanner.scanProject(tempDir);
        
        assertEquals(1, javaFiles.size());
        assertTrue(javaFiles.contains(javaFile));
        assertFalse(javaFiles.contains(targetJavaFile));
        assertFalse(javaFiles.contains(buildJavaFile));
    }

    @Test
    void testScanProjectWithNestedDirectories(@TempDir Path tempDir) throws IOException {
        // Create nested directory structure
        Path srcDir = tempDir.resolve("src").resolve("main").resolve("java");
        Files.createDirectories(srcDir);
        
        Path javaFile1 = srcDir.resolve("TestClass1.java");
        Files.write(javaFile1, "public class TestClass1 { }".getBytes());
        
        Path javaFile2 = srcDir.resolve("com").resolve("test").resolve("TestClass2.java");
        Files.createDirectories(javaFile2.getParent());
        Files.write(javaFile2, "public class TestClass2 { }".getBytes());

        List<Path> javaFiles = scanner.scanProject(tempDir);
        
        assertEquals(2, javaFiles.size());
        assertTrue(javaFiles.contains(javaFile1));
        assertTrue(javaFiles.contains(javaFile2));
    }

    @Test
    void testScanProjectWithNonExistentPath() {
        Path nonExistentPath = Path.of("/non/existent/path");
        
        assertThrows(IllegalArgumentException.class, () -> {
            scanner.scanProject(nonExistentPath);
        });
    }

    @Test
    void testScanProjectWithFileInsteadOfDirectory(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.write(file, "This is a file".getBytes());
        
        assertThrows(IllegalArgumentException.class, () -> {
            scanner.scanProject(file);
        });
    }

    @Test
    void testScanProjectWithNoJavaFiles(@TempDir Path tempDir) throws IOException {
        Path textFile = tempDir.resolve("readme.txt");
        Files.write(textFile, "This is a readme".getBytes());
        
        List<Path> javaFiles = scanner.scanProject(tempDir);
        
        assertTrue(javaFiles.isEmpty());
    }

    @Test
    void testGetTotalLinesOfCode(@TempDir Path tempDir) throws IOException {
        // Create test Java files with known line counts
        Path javaFile1 = tempDir.resolve("TestClass1.java");
        Files.write(javaFile1, "public class TestClass1 {\n    public void method1() {\n        System.out.println(\"test1\");\n    }\n}".getBytes());
        
        Path javaFile2 = tempDir.resolve("TestClass2.java");
        Files.write(javaFile2, "public class TestClass2 {\n    public void method2() {\n        System.out.println(\"test2\");\n    }\n}".getBytes());
        
        List<Path> javaFiles = List.of(javaFile1, javaFile2);
        
        long totalLines = scanner.getTotalLinesOfCode(javaFiles);
        
        // Each file has 5 lines
        assertEquals(10, totalLines);
    }

    @Test
    void testGetTotalLinesOfCodeWithEmptyList() throws IOException {
        List<Path> emptyList = List.of();
        
        long totalLines = scanner.getTotalLinesOfCode(emptyList);
        
        assertEquals(0, totalLines);
    }
}
