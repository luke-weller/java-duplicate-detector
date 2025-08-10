package com.duplicatedetector.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Scans a Java project directory to find all Java source files.
 */
public class ProjectScanner {
    private static final Logger log = LoggerFactory.getLogger(ProjectScanner.class);
    
    private static final String JAVA_FILE_EXTENSION = ".java";
    private static final String[] EXCLUDE_DIRS = {
        "target", "build", "out", "bin", ".git", ".svn", ".idea", ".vscode"
    };

    /**
     * Scans the project directory and returns all Java source files.
     * 
     * @param projectPath the root path of the Java project
     * @return list of Java file paths
     * @throws IOException if an I/O error occurs during scanning
     */
    public List<Path> scanProject(Path projectPath) throws IOException {
        if (!Files.exists(projectPath)) {
            throw new IllegalArgumentException("Project path does not exist: " + projectPath);
        }
        
        if (!Files.isDirectory(projectPath)) {
            throw new IllegalArgumentException("Project path is not a directory: " + projectPath);
        }

        log.info("Scanning project directory: {}", projectPath);
        
        List<Path> javaFiles = new ArrayList<>();
        
        Files.walkFileTree(projectPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (isJavaFile(file)) {
                    javaFiles.add(file);
                    log.debug("Found Java file: {}", file);
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (shouldExcludeDirectory(dir)) {
                    log.debug("Excluding directory: {}", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                log.warn("Failed to visit file: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });
        
        log.info("Found {} Java files in project", javaFiles.size());
        return javaFiles;
    }

    /**
     * Checks if the given path is a Java source file.
     */
    private boolean isJavaFile(Path path) {
        return path.toString().toLowerCase().endsWith(JAVA_FILE_EXTENSION);
    }

    /**
     * Checks if the given directory should be excluded from scanning.
     */
    private boolean shouldExcludeDirectory(Path dir) {
        String dirName = dir.getFileName().toString().toLowerCase();
        for (String excludeDir : EXCLUDE_DIRS) {
            if (excludeDir.equals(dirName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the total number of lines of code in all Java files.
     * 
     * @param javaFiles list of Java file paths
     * @return total number of lines
     * @throws IOException if an I/O error occurs
     */
    public long getTotalLinesOfCode(List<Path> javaFiles) throws IOException {
        long totalLines = 0;
        
        for (Path file : javaFiles) {
            try {
                long lines = Files.lines(file).count();
                totalLines += lines;
                log.debug("File {} has {} lines", file, lines);
            } catch (IOException e) {
                log.warn("Failed to count lines in file: {}", file, e);
            }
        }
        
        return totalLines;
    }
}
