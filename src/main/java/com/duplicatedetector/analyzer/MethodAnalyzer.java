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
import java.util.List;

/**
 * Analyzes Java source files to extract method information.
 */
public class MethodAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(MethodAnalyzer.class);

    /**
     * Analyzes all Java files and extracts method information.
     * 
     * @param javaFiles list of Java file paths to analyze
     * @return list of extracted method information
     */
    public List<MethodInfo> analyzeMethods(List<Path> javaFiles) {
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
     * Analyzes a single Java file and extracts method information.
     * 
     * @param javaFile path to the Java file
     * @return list of method information from the file
     * @throws IOException if the file cannot be read
     */
    public List<MethodInfo> analyzeFile(Path javaFile) throws IOException {
        String sourceCode = Files.readString(javaFile);
        CompilationUnit compilationUnit = StaticJavaParser.parse(sourceCode);
        
        List<MethodInfo> methods = new ArrayList<>();
        
        // Extract package name
        String packageName = compilationUnit.getPackageDeclaration()
                .map(pkg -> pkg.getNameAsString())
                .orElse("");
        
        // Find all classes and interfaces
        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            String className = classDecl.getNameAsString();
            
            // Extract methods from this class
            classDecl.getMethods().forEach(methodDecl -> {
                MethodInfo methodInfo = extractMethodInfo(methodDecl, className, packageName, javaFile);
                if (methodInfo != null) {
                    methods.add(methodInfo);
                }
            });
        });
        
        return methods;
    }

    /**
     * Extracts method information from a method declaration.
     */
    private MethodInfo extractMethodInfo(MethodDeclaration methodDecl, String className, 
                                       String packageName, Path filePath) {
        try {
            String methodName = methodDecl.getNameAsString();
            
            // Skip constructors and synthetic methods
            if (methodName.equals(className) || methodName.startsWith("access$")) {
                return null;
            }
            
            // Build method signature
            String signature = buildMethodSignature(methodDecl);
            
            // Extract method body
            String body = methodDecl.getBody()
                    .map(bodyBlock -> bodyBlock.toString())
                    .orElse("");
            
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
            log.warn("Failed to extract method info for method in class {}", className, e);
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
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Filters out methods that are likely to be getters, setters, or simple accessors.
     * 
     * @param methods list of all methods
     * @return filtered list of methods
     */
    public List<MethodInfo> filterOutSimpleAccessors(List<MethodInfo> methods) {
        return methods.stream()
                .filter(method -> !isSimpleAccessor(method))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Checks if a method is a simple accessor (getter, setter, or simple return).
     */
    private boolean isSimpleAccessor(MethodInfo method) {
        String methodName = method.getMethodName().toLowerCase();
        String body = method.getBody().toLowerCase();
        
        // Check for getter patterns
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return body.contains("return") && body.length() < 50;
        }
        
        // Check for setter patterns
        if (methodName.startsWith("set") && methodName.length() > 3) {
            return body.contains("this.") && body.length() < 50;
        }
        
        // Check for simple return statements
        if (body.trim().startsWith("return") && body.length() < 30) {
            return true;
        }
        
        return false;
    }
}
