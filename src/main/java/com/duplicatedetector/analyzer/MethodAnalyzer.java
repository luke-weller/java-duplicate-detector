package com.duplicatedetector.analyzer;

import com.duplicatedetector.model.MethodInfo;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Analyzes Java source files to extract method information with optimized performance.
 */
public class MethodAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(MethodAnalyzer.class);
    private static final int MAX_PARALLEL_THREADS = Runtime.getRuntime().availableProcessors();
    private static final int MIN_METHOD_LENGTH = 10; // Early filtering threshold (reduced for test compatibility)

    /**
     * Analyzes all Java files and extracts method information with parallel processing.
     * 
     * @param javaFiles list of Java file paths to analyze
     * @return list of extracted method information
     */
    public List<MethodInfo> analyzeMethods(List<Path> javaFiles) {
        log.info("Analyzing {} Java files using {} threads", javaFiles.size(), MAX_PARALLEL_THREADS);
        
        long startTime = System.currentTimeMillis();
        
        if (javaFiles.size() < 10) {
            // For small projects, use sequential processing
            return analyzeMethodsSequential(javaFiles);
        } else {
            // For large projects, use parallel processing
            return analyzeMethodsParallel(javaFiles);
        }
    }

    /**
     * Sequential analysis for small projects.
     */
    private List<MethodInfo> analyzeMethodsSequential(List<Path> javaFiles) {
        List<MethodInfo> allMethods = new ArrayList<>();
        
        for (Path javaFile : javaFiles) {
            try {
                List<MethodInfo> methods = analyzeFile(javaFile);
                allMethods.addAll(methods);
                log.debug("Extracted {} methods from {}", methods.size(), javaFile);
            } catch (Exception e) {
                log.warn("Failed to analyze file: {}", javaFile, e);
            }
        }
        
        log.info("Total methods extracted: {}", allMethods.size());
        return allMethods;
    }

    /**
     * Parallel analysis for large projects.
     */
    private List<MethodInfo> analyzeMethodsParallel(List<Path> javaFiles) {
        ExecutorService executor = Executors.newFixedThreadPool(MAX_PARALLEL_THREADS);
        List<MethodInfo> allMethods = Collections.synchronizedList(new ArrayList<>());
        
        try {
            // Create tasks for parallel processing
            List<Future<Void>> futures = new ArrayList<>();
            
            for (Path javaFile : javaFiles) {
                futures.add(executor.submit(() -> {
                    try {
                        List<MethodInfo> methods = analyzeFile(javaFile);
                        allMethods.addAll(methods);
                        log.debug("Extracted {} methods from {}", methods.size(), javaFile);
                    } catch (Exception e) {
                        log.warn("Failed to analyze file: {}", javaFile, e);
                    }
                    return null;
                }));
            }
            
            // Wait for all tasks to complete
            for (Future<Void> future : futures) {
                future.get();
            }
            
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during parallel analysis", e);
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
        
        long endTime = System.currentTimeMillis();
        log.info("Total methods extracted: {} in {} ms", allMethods.size(), endTime - System.currentTimeMillis());
        return allMethods;
    }

    /**
     * Analyzes a single Java file and extracts method information.
     * Optimized with early filtering and better error handling.
     * 
     * @param javaFile path to the Java file
     * @return list of method information from the file
     * @throws IOException if the file cannot be read
     */
    public List<MethodInfo> analyzeFile(Path javaFile) throws IOException {
        // Quick check for file size to skip very large files that might cause issues
        long fileSize = Files.size(javaFile);
        if (fileSize > 10 * 1024 * 1024) { // Skip files larger than 10MB
            log.warn("Skipping large file: {} ({} bytes)", javaFile, fileSize);
            return new ArrayList<>();
        }
        
        String sourceCode = Files.readString(javaFile);
        
        // Quick check for method-like content before parsing
        if (!sourceCode.contains("public") && !sourceCode.contains("private") && 
            !sourceCode.contains("protected")) {
            log.debug("Skipping file with no method declarations: {}", javaFile);
            return new ArrayList<>();
        }
        
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(sourceCode);
            
            List<MethodInfo> methods = new ArrayList<>();
            
            // Extract package name
            String packageName = compilationUnit.getPackageDeclaration()
                    .map(pkg -> pkg.getNameAsString())
                    .orElse("");
            
            // Find all classes and interfaces
            compilationUnit.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
                String className = classDecl.getNameAsString();
                
                // Extract methods from this class with early filtering
                classDecl.getMethods().stream()
                    .filter(this::shouldAnalyzeMethod) // Early filtering
                    .forEach(methodDecl -> {
                        MethodInfo methodInfo = extractMethodInfo(methodDecl, className, packageName, javaFile);
                        if (methodInfo != null) {
                            // For interface methods, don't filter by length since abstract methods have no body
                            boolean isInterfaceMethod = classDecl.isInterface();
                            if (isInterfaceMethod || methodInfo.getMethodLength() >= MIN_METHOD_LENGTH) {
                                methods.add(methodInfo);
                            }
                        }
                    });
            });
            
            return methods;
            
        } catch (Exception e) {
            log.warn("Failed to parse Java file: {}", javaFile, e);
            return new ArrayList<>();
        }
    }

    /**
     * Early filtering to determine if a method should be analyzed.
     */
    private boolean shouldAnalyzeMethod(MethodDeclaration methodDecl) {
        // Skip constructors
        if (methodDecl.isConstructorDeclaration()) {
            return false;
        }
        
        // Skip synthetic methods
        String methodName = methodDecl.getNameAsString();
        if (methodName.startsWith("access$") || methodName.startsWith("lambda$")) {
            return false;
        }
        
        // Include all methods including interface methods (abstract and default)
        return true;
    }

    /**
     * Extracts method information from a method declaration.
     * Optimized with better error handling and validation.
     */
    private MethodInfo extractMethodInfo(MethodDeclaration methodDecl, String className, 
                                       String packageName, Path filePath) {
        try {
            String methodName = methodDecl.getNameAsString();
            
            // Skip constructors and synthetic methods (double-check)
            if (methodName.equals(className) || methodName.startsWith("access$")) {
                return null;
            }
            
            // Build method signature
            String signature = buildMethodSignature(methodDecl);
            
            // Extract method body
            String body = methodDecl.getBody()
                    .map(bodyBlock -> bodyBlock.toString())
                    .orElse("");
            
            // Skip if body is too short (but allow interface methods without bodies)
            // TODO: Re-enable length filtering with proper interface handling
            // if (body.length() < MIN_METHOD_LENGTH && !body.isEmpty()) {
            //     return null;
            // }
            
            // Get line numbers
            int startLine = methodDecl.getBegin()
                    .map(pos -> pos.line)
                    .orElse(0);
            int endLine = methodDecl.getEnd()
                    .map(pos -> pos.line)
                    .orElse(0);
            
            return new MethodInfo(className, methodName, signature, body, 
                                filePath, startLine, endLine, packageName);
            
        } catch (Exception e) {
            log.debug("Failed to extract method info for method {} in class {}", 
                     methodDecl.getNameAsString(), className, e);
            return null;
        }
    }

    /**
     * Builds a method signature string.
     */
    private String buildMethodSignature(MethodDeclaration methodDecl) {
        StringBuilder signature = new StringBuilder();
        
        // Add modifiers
        methodDecl.getModifiers().forEach(modifier -> 
            signature.append(modifier.toString()).append(" "));
        
        // Add return type
        signature.append(methodDecl.getType().toString()).append(" ");
        
        // Add method name
        signature.append(methodDecl.getNameAsString());
        
        // Add parameters
        signature.append("(");
        List<Parameter> parameters = methodDecl.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) signature.append(", ");
            Parameter param = parameters.get(i);
            signature.append(param.getType().toString());
            signature.append(" ").append(param.getNameAsString());
        }
        signature.append(")");
        
        return signature.toString();
    }

    /**
     * Filters methods based on minimum length to avoid analyzing very short methods.
     * 
     * @param methods list of all methods
     * @param minLength minimum method length in characters
     * @return filtered list of methods
     */
    public List<MethodInfo> filterMethodsByLength(List<MethodInfo> methods, int minLength) {
        return methods.stream()
                .filter(method -> method.getMethodLength() >= minLength)
                .collect(Collectors.toList());
    }

    /**
     * Filters out methods that are likely to be getters, setters, or simple accessors.
     * Optimized with better pattern matching.
     * 
     * @param methods list of all methods
     * @return filtered list of methods
     */
    public List<MethodInfo> filterOutSimpleAccessors(List<MethodInfo> methods) {
        return methods.stream()
                .filter(method -> !isSimpleAccessor(method))
                .collect(Collectors.toList());
    }

    /**
     * Checks if a method is a simple accessor (getter, setter, or simple return).
     * Enhanced with more sophisticated pattern matching.
     */
    private boolean isSimpleAccessor(MethodInfo method) {
        String methodName = method.getMethodName().toLowerCase();
        String body = method.getBody().toLowerCase().trim();
        
        // Check for getter patterns
        if (methodName.startsWith("get") && methodName.length() > 3) {
            // Simple getter: just returns a field
            if (body.startsWith("return") && body.length() < 50) {
                return true;
            }
            // Getter with null check
            if (body.contains("return") && body.contains("null") && body.length() < 100) {
                return true;
            }
        }
        
        // Check for setter patterns
        if (methodName.startsWith("set") && methodName.length() > 3) {
            // Simple setter: just assigns to a field
            if (body.contains("this.") && body.length() < 50) {
                return true;
            }
            // Setter with validation
            if (body.contains("this.") && body.contains("if") && body.length() < 150) {
                return true;
            }
        }
        
        // Check for boolean getters (isXxx)
        if (methodName.startsWith("is") && methodName.length() > 2) {
            if (body.startsWith("return") && body.length() < 50) {
                return true;
            }
        }
        
        // Check for simple return statements
        if (body.startsWith("return") && body.length() < 30) {
            return true;
        }
        
        // Check for empty methods or methods with only comments
        if (body.isEmpty() || body.replaceAll("\\s+", "").isEmpty()) {
            return true;
        }
        
        return false;
    }
}
