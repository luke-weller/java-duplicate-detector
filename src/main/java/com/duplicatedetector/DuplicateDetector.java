package com.duplicatedetector;

import com.duplicatedetector.analyzer.MethodAnalyzer;
import com.duplicatedetector.model.MethodInfo;
import com.duplicatedetector.model.DuplicateGroup;
import com.duplicatedetector.scanner.ProjectScanner;
import com.duplicatedetector.similarity.SimilarityDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Main class for the Java Duplicate Detector tool.
 * Scans Java projects to find duplicate or similar methods that can be refactored.
 */
public class DuplicateDetector {
    private static final Logger log = LoggerFactory.getLogger(DuplicateDetector.class);
    
    public static void main(String[] args) {
        DuplicateDetector detector = new DuplicateDetector();
        
        if (args.length > 0) {
            log.info("Starting duplicate detection with command line argument: {}", args[0]);
            detector.run(Paths.get(args[0]));
        } else {
            log.info("Starting duplicate detection in interactive mode");
            detector.runInteractive();
        }
    }
    
    /**
     * Runs the duplicate detection in interactive mode.
     */
    public void runInteractive() {
        Scanner scanner = new Scanner(System.in);
        
        log.info("=== Java Duplicate Detector ===");
        log.info("This tool scans Java projects to find duplicate or similar methods.");
        log.info("");
        
        log.info("Enter the path to your Java project: ");
        String projectPath = scanner.nextLine().trim();
        
        if (projectPath.isEmpty()) {
            log.warn("No path provided. Exiting.");
            return;
        }
        
        log.info("User provided project path: {}", projectPath);
        Path path = Paths.get(projectPath);
        run(path);
        
        scanner.close();
    }
    
    /**
     * Runs the duplicate detection on the specified project path.
     */
    public void run(Path projectPath) {
        try {
            log.info("Starting duplicate detection for project: {}", projectPath);
            
            // Step 1: Scan the project for Java files
            log.debug("Step 1: Scanning project for Java files");
            ProjectScanner scanner = new ProjectScanner();
            List<Path> javaFiles = scanner.scanProject(projectPath);
            
            if (javaFiles.isEmpty()) {
                log.warn("No Java files found in the specified project: {}", projectPath);
                return;
            }
            
            log.info("Found {} Java files to analyze", javaFiles.size());
            log.debug("Java files found: {}", javaFiles);
            
            // Step 2: Analyze methods in all files
            log.debug("Step 2: Analyzing methods in all files");
            MethodAnalyzer analyzer = new MethodAnalyzer();
            List<MethodInfo> methods = analyzer.analyzeMethods(javaFiles);
            
            if (methods.isEmpty()) {
                log.warn("No methods found in the project: {}", projectPath);
                return;
            }
            
            log.info("Analyzed {} methods from {} files", methods.size(), javaFiles.size());
            log.debug("Methods found: {}", methods.stream().map(MethodInfo::getFullMethodName).collect(java.util.stream.Collectors.toList()));
            
            // Step 3: Detect similar methods
            log.debug("Step 3: Detecting similar methods");
            SimilarityDetector similarityDetector = new SimilarityDetector();
            List<DuplicateGroup> duplicateGroups = similarityDetector.findSimilarMethods(methods);
            
            // Step 4: Display results
            log.debug("Step 4: Displaying results");
            displayResults(duplicateGroups);
            
        } catch (Exception e) {
            log.error("Error during duplicate detection for project: {}", projectPath, e);
            throw new RuntimeException("Failed to analyze project: " + projectPath, e);
        }
    }
    
    /**
     * Displays the results of the duplicate detection.
     */
    private void displayResults(List<DuplicateGroup> duplicateGroups) {
        log.info("=== Duplicate Detection Results ===");
        
        if (duplicateGroups.isEmpty()) {
            log.info("No duplicate methods found!");
            return;
        }
        
        log.info("Found {} groups of similar methods:", duplicateGroups.size());
        
        for (int i = 0; i < duplicateGroups.size(); i++) {
            DuplicateGroup group = duplicateGroups.get(i);
            String similarityPercentage = String.format("%.1f", group.getSimilarityScore() * 100);
            log.info("Group {} (Similarity: {}%):", i + 1, similarityPercentage);
            
            for (MethodInfo method : group.getMethods()) {
                log.info("  - {}.{}() in {} (lines {}-{})", 
                    method.getClassName(), 
                    method.getMethodName(), 
                    method.getFilePath(),
                    method.getStartLine(),
                    method.getEndLine());
            }
            
            if (group.getMethods().size() > 1) {
                log.info("  Suggested refactoring: Extract common logic into a shared method");
            }
            
            log.info("");
        }
        
        log.info("=== End of Results ===");
    }
}
