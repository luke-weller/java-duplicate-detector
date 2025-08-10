package com.duplicatedetector.analyzer;

import com.duplicatedetector.model.MethodInfo;
import com.duplicatedetector.model.DuplicateGroup;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Analyzes duplicate methods and provides detailed refactoring recommendations.
 */
public class RefactoringAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(RefactoringAnalyzer.class);
    
    private static final Pattern METHOD_CALL_PATTERN = Pattern.compile("\\b\\w+\\s*\\([^)]*\\)");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\b\\w+\\s*=");
    private static final Pattern LOOP_PATTERN = Pattern.compile("\\b(for|while|do)\\b");
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\b(if|else|switch)\\b");
    private static final Pattern RETURN_PATTERN = Pattern.compile("\\breturn\\b");
    
    /**
     * Analyzes a duplicate group and provides detailed refactoring recommendations.
     */
    public RefactoringAnalysis analyzeDuplicateGroup(DuplicateGroup group) {
        List<MethodInfo> methods = group.getMethods();
        String similarityType = group.getSimilarityType();
        double similarityScore = group.getSimilarityScore();
        
        RefactoringAnalysis analysis = new RefactoringAnalysis();
        analysis.setSimilarityType(similarityType);
        analysis.setSimilarityScore(similarityScore);
        analysis.setMethods(methods);
        
        // Analyze the type of duplication and provide specific recommendations
        switch (similarityType) {
            case "EXACT":
                analyzeExactDuplication(analysis);
                break;
            case "SIGNATURE":
                analyzeSignatureSimilarity(analysis);
                break;
            case "STRUCTURAL":
                analyzeStructuralSimilarity(analysis);
                break;
            default:
                analyzeGenericSimilarity(analysis);
        }
        
        // Add code quality insights
        addCodeQualityInsights(analysis);
        
        return analysis;
    }
    
    /**
     * Analyzes exact duplication (identical code).
     */
    private void analyzeExactDuplication(RefactoringAnalysis analysis) {
        analysis.setDuplicationType("Exact Code Duplication");
        analysis.setSeverity("HIGH");
        
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Extract the identical code into a shared utility method");
        recommendations.add("Consider creating a common base class or interface");
        recommendations.add("Apply the Template Method pattern if the methods follow similar algorithms");
        recommendations.add("Use composition over inheritance to share common functionality");
        
        analysis.setRecommendations(recommendations);
        analysis.setRefactoringPattern("Extract Method + Strategy Pattern");
        
        // Check if methods are in different classes
        Set<String> classNames = analysis.getMethods().stream()
                .map(MethodInfo::getClassName)
                .collect(Collectors.toSet());
        
        if (classNames.size() > 1) {
            analysis.setCrossClassIssue(true);
            analysis.setCrossClassRecommendation("Consider creating a shared utility class or service");
        }
    }
    
    /**
     * Analyzes signature similarity (similar method signatures).
     */
    private void analyzeSignatureSimilarity(RefactoringAnalysis analysis) {
        analysis.setDuplicationType("Signature Similarity");
        analysis.setSeverity("MEDIUM");
        
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Create a common interface defining the contract");
        recommendations.add("Apply the Strategy pattern to handle different implementations");
        recommendations.add("Consider using generics to make the methods more flexible");
        recommendations.add("Extract common parameter validation logic");
        
        analysis.setRecommendations(recommendations);
        analysis.setRefactoringPattern("Strategy Pattern + Interface Segregation");
        
        // Analyze parameter patterns
        analyzeParameterPatterns(analysis);
    }
    
    /**
     * Analyzes structural similarity (similar control flow).
     */
    private void analyzeStructuralSimilarity(RefactoringAnalysis analysis) {
        analysis.setDuplicationType("Structural Similarity");
        analysis.setSeverity("MEDIUM");
        
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Extract common control flow into a template method");
        recommendations.add("Apply the Template Method pattern");
        recommendations.add("Use the Command pattern for similar operations");
        recommendations.add("Consider using the Builder pattern for complex object construction");
        
        analysis.setRecommendations(recommendations);
        analysis.setRefactoringPattern("Template Method Pattern");
        
        // Analyze control flow patterns
        analyzeControlFlowPatterns(analysis);
    }
    
    /**
     * Analyzes generic similarity.
     */
    private void analyzeGenericSimilarity(RefactoringAnalysis analysis) {
        analysis.setDuplicationType("Generic Similarity");
        analysis.setSeverity("LOW");
        
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Review the methods for potential abstraction opportunities");
        recommendations.add("Consider if the similarity indicates a missing abstraction");
        recommendations.add("Look for opportunities to apply common design patterns");
        
        analysis.setRecommendations(recommendations);
        analysis.setRefactoringPattern("Review and Refactor");
    }
    
    /**
     * Analyzes parameter patterns in similar methods.
     */
    private void analyzeParameterPatterns(RefactoringAnalysis analysis) {
        List<MethodInfo> methods = analysis.getMethods();
        Map<String, Integer> paramTypeFrequency = new HashMap<>();
        
        for (MethodInfo method : methods) {
            String signature = method.getSignature();
            List<String> paramTypes = extractParameterTypes(signature);
            
            for (String paramType : paramTypes) {
                paramTypeFrequency.merge(paramType, 1, Integer::sum);
            }
        }
        
        // Find common parameter types
        List<String> commonParams = paramTypeFrequency.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        if (!commonParams.isEmpty()) {
            analysis.setCommonParameters(commonParams);
            analysis.addInsight("Common parameter types detected: " + String.join(", ", commonParams));
        }
    }
    
    /**
     * Analyzes control flow patterns in similar methods.
     */
    private void analyzeControlFlowPatterns(RefactoringAnalysis analysis) {
        List<MethodInfo> methods = analysis.getMethods();
        Map<String, Integer> patternFrequency = new HashMap<>();
        
        for (MethodInfo method : methods) {
            String body = method.getBody().toLowerCase();
            
            // Count different control flow patterns
            countPattern(body, LOOP_PATTERN, "loops", patternFrequency);
            countPattern(body, CONDITIONAL_PATTERN, "conditionals", patternFrequency);
            countPattern(body, METHOD_CALL_PATTERN, "method calls", patternFrequency);
            countPattern(body, RETURN_PATTERN, "returns", patternFrequency);
        }
        
        // Add insights about common patterns
        patternFrequency.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .forEach(entry -> {
                    analysis.addInsight("Common " + entry.getKey() + " pattern detected across methods");
                });
    }
    
    /**
     * Counts occurrences of a pattern in text.
     */
    private void countPattern(String text, Pattern pattern, String name, Map<String, Integer> frequency) {
        int count = (int) pattern.matcher(text).results().count();
        if (count > 0) {
            frequency.merge(name, count, Integer::sum);
        }
    }
    
    /**
     * Adds code quality insights to the analysis.
     */
    private void addCodeQualityInsights(RefactoringAnalysis analysis) {
        List<MethodInfo> methods = analysis.getMethods();
        List<String> insights = new ArrayList<>();
        
        // Check for long methods
        methods.stream()
                .filter(method -> method.getMethodLength() > 100)
                .forEach(method -> {
                    insights.add("Method " + method.getMethodName() + " is quite long (" + 
                               method.getMethodLength() + " chars) - consider breaking it down");
                });
        
        // Check for similar method names
        Set<String> methodNames = methods.stream()
                .map(MethodInfo::getMethodName)
                .collect(Collectors.toSet());
        
        if (methodNames.size() < methods.size()) {
            insights.add("Methods have identical names - consider more descriptive naming");
        }
        
        // Check for similar class names
        Set<String> classNames = methods.stream()
                .map(MethodInfo::getClassName)
                .collect(Collectors.toSet());
        
        if (classNames.size() > 1) {
            insights.add("Methods are spread across " + classNames.size() + " different classes");
        }
        
        analysis.setCodeQualityInsights(insights);
    }
    
    /**
     * Extracts parameter types from method signature.
     */
    private List<String> extractParameterTypes(String signature) {
        int startParen = signature.indexOf('(');
        int endParen = signature.indexOf(')', startParen);
        
        if (startParen == -1 || endParen == -1) return new ArrayList<>();
        
        String params = signature.substring(startParen + 1, endParen);
        if (params.trim().isEmpty()) return new ArrayList<>();
        
        return Arrays.stream(params.split(","))
                .map(param -> param.trim().split("\\s+")[0])
                .collect(Collectors.toList());
    }
}
