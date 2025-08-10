package com.duplicatedetector.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collections;

/**
 * Scans a Java project directory to find all Java source files with optimized performance.
 */
public class ProjectScanner {
    private static final Logger log = LoggerFactory.getLogger(ProjectScanner.class);
    
    private static final String JAVA_FILE_EXTENSION = ".java";
    private static final String[] EXCLUDE_DIRS = {
        "target", "build", "out", "bin", ".git", ".svn", ".idea", ".vscode", 
        "node_modules", "dist", "coverage", "logs", "tmp", "temp"
    };
    private static final String[] EXCLUDE_FILES = {
        "package-info.java", "module-info.java"
    };
    private static final int MAX_PARALLEL_THREADS = Runtime.getRuntime().availableProcessors();

    /**
     * Scans the project directory and returns all Java source files.
     * Optimized with parallel processing for large projects.
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

        log.info("Scanning project directory: {} (using {} threads)", projectPath, MAX_PARALLEL_THREADS);
        
        long startTime = System.currentTimeMillis();
        
        List<Path> javaFiles = new ArrayList<>();
        
        // Use parallel file tree walk for large projects
        if (isLargeProject(projectPath)) {
            javaFiles = scanProjectParallel(projectPath);
        } else {
            javaFiles = scanProjectSequential(projectPath);
        }
        
        long endTime = System.currentTimeMillis();
        log.info("Found {} Java files in {} ms", javaFiles.size(), endTime - startTime);
        return javaFiles;
    }

    /**
     * Determines if a project is large enough to warrant parallel processing.
     */
    private boolean isLargeProject(Path projectPath) throws IOException {
        // Count files quickly to determine project size
        AtomicInteger fileCount = new AtomicInteger(0);
        Files.walkFileTree(projectPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (isJavaFile(file)) {
                    fileCount.incrementAndGet();
                    // Stop counting if we already know it's large
                    if (fileCount.get() > 100) {
                        return FileVisitResult.TERMINATE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (shouldExcludeDirectory(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        return fileCount.get() > 50; // Use parallel processing for projects with >50 Java files
    }

    /**
     * Sequential scanning for small projects.
     */
    private List<Path> scanProjectSequential(Path projectPath) throws IOException {
        List<Path> javaFiles = new ArrayList<>();
        
        Files.walkFileTree(projectPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (isJavaFile(file) && !shouldExcludeFile(file)) {
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
        
        return javaFiles;
    }

    /**
     * Parallel scanning for large projects.
     */
    private List<Path> scanProjectParallel(Path projectPath) throws IOException {
        List<Path> javaFiles = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(MAX_PARALLEL_THREADS);
        
        try {
            // First, collect all directories to process
            List<Path> directories = new ArrayList<>();
            Files.walkFileTree(projectPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!shouldExcludeDirectory(dir)) {
                        directories.add(dir);
                    } else {
                        log.debug("Excluding directory: {}", dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            
            // Process directories in parallel
            List<Future<List<Path>>> futures = new ArrayList<>();
            
            for (Path dir : directories) {
                futures.add(executor.submit(() -> {
                    List<Path> files = new ArrayList<>();
                    try {
                        // Use walkFileTree to recursively scan each directory
                        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                if (isJavaFile(file) && !shouldExcludeFile(file)) {
                                    files.add(file);
                                    log.debug("Found Java file: {}", file);
                                }
                                return FileVisitResult.CONTINUE;
                            }
                            
                            @Override
                            public FileVisitResult preVisitDirectory(Path subDir, BasicFileAttributes attrs) {
                                if (shouldExcludeDirectory(subDir)) {
                                    return FileVisitResult.SKIP_SUBTREE;
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        log.warn("Failed to scan directory: {}", dir, e);
                    }
                    return files;
                }));
            }
            
            // Collect results
            for (Future<List<Path>> future : futures) {
                try {
                    javaFiles.addAll(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error during parallel scanning", e);
                    Thread.currentThread().interrupt();
                }
            }
            
        } finally {
            executor.shutdown();
        }
        
        return javaFiles;
    }

    /**
     * Checks if the given path is a Java source file.
     * Enhanced with additional validation.
     */
    private boolean isJavaFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(JAVA_FILE_EXTENSION) && !shouldExcludeFile(path);
    }

    /**
     * Checks if the given directory should be excluded from scanning.
     * Enhanced with more comprehensive exclusion patterns.
     */
    private boolean shouldExcludeDirectory(Path dir) {
        String dirName = dir.getFileName().toString().toLowerCase();
        
        // Check exact matches
        for (String excludeDir : EXCLUDE_DIRS) {
            if (excludeDir.equals(dirName)) {
                return true;
            }
        }
        
        // Check for hidden directories
        if (dirName.startsWith(".")) {
            return true;
        }
        
        // Check for common build/test directories (but be more specific)
        // Only exclude test directories at the root level, not nested ones like com/test
        if (dirName.equals("generated") || dirName.equals("build") || 
            dirName.equals("target") || dirName.equals("bin") ||
            dirName.equals("out") || dirName.equals("classes")) {
            return true;
        }
        
        // Only exclude test directories if they're at the root level
        if ((dirName.equals("test") || dirName.equals("tests")) && 
            dir.getNameCount() <= 2) { // Root level or one level deep
            return true;
        }
        
        return false;
    }

    /**
     * Checks if a specific file should be excluded from scanning.
     */
    private boolean shouldExcludeFile(Path file) {
        String fileName = file.getFileName().toString();
        
        // Check for excluded file patterns
        for (String excludeFile : EXCLUDE_FILES) {
            if (excludeFile.equals(fileName)) {
                return true;
            }
        }
        
        // Check for generated files
        if (fileName.contains("Generated") || fileName.contains("generated")) {
            return true;
        }
        
        // Check for test files if we want to exclude them (but be more specific)
        if (fileName.startsWith("Test") && fileName.endsWith(".java")) {
            return false; // Don't exclude test files in test scenarios
        }
        
        return false;
    }

    /**
     * Gets the total number of lines of code in all Java files.
     * Optimized with parallel processing for large projects.
     * 
     * @param javaFiles list of Java file paths
     * @return total number of lines
     * @throws IOException if an I/O error occurs
     */
    public long getTotalLinesOfCode(List<Path> javaFiles) throws IOException {
        if (javaFiles.size() < 100) {
            return getTotalLinesOfCodeSequential(javaFiles);
        } else {
            return getTotalLinesOfCodeParallel(javaFiles);
        }
    }

    /**
     * Sequential line counting for small projects.
     */
    private long getTotalLinesOfCodeSequential(List<Path> javaFiles) throws IOException {
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

    /**
     * Parallel line counting for large projects.
     */
    private long getTotalLinesOfCodeParallel(List<Path> javaFiles) throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(MAX_PARALLEL_THREADS);
        AtomicInteger totalLines = new AtomicInteger(0);
        
        try {
            List<Future<Void>> futures = new ArrayList<>();
            
            for (Path file : javaFiles) {
                futures.add(executor.submit(() -> {
                    try {
                        long lines = Files.lines(file).count();
                        totalLines.addAndGet((int) lines);
                        log.debug("File {} has {} lines", file, lines);
                    } catch (IOException e) {
                        log.warn("Failed to count lines in file: {}", file, e);
                    }
                    return null;
                }));
            }
            
            // Wait for all tasks to complete
            for (Future<Void> future : futures) {
                future.get();
            }
            
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during parallel line counting", e);
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
        
        return totalLines.get();
    }
}
