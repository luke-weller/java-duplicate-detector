package com.duplicatedetector.similarity;

import com.duplicatedetector.model.MethodInfo;
import com.duplicatedetector.model.DuplicateGroup;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects similar methods using various similarity algorithms.
 */
public class SimilarityDetector {
    private static final Logger log = LoggerFactory.getLogger(SimilarityDetector.class);
    
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;
    private static final int MIN_METHOD_LENGTH = 50;

    /**
     * Finds similar methods in the given list.
     * 
     * @param methods list of methods to analyze
     * @return list of duplicate groups
     */
    public List<DuplicateGroup> findSimilarMethods(List<MethodInfo> methods) {
        return findSimilarMethods(methods, DEFAULT_SIMILARITY_THRESHOLD);
    }

    /**
     * Finds similar methods in the given list with a custom threshold.
     * 
     * @param methods list of methods to analyze
     * @param similarityThreshold minimum similarity score (0.0 to 1.0)
     * @return list of duplicate groups
     */
    public List<DuplicateGroup> findSimilarMethods(List<MethodInfo> methods, double similarityThreshold) {
        log.info("Finding similar methods with threshold: {}", similarityThreshold);
        
        // Filter out very short methods
        List<MethodInfo> filteredMethods = methods.stream()
                .filter(method -> method.getMethodLength() >= MIN_METHOD_LENGTH)
                .collect(Collectors.toList());
        
        log.info("Analyzing {} methods (filtered from {} total)", 
                   filteredMethods.size(), methods.size());
        
        List<DuplicateGroup> duplicateGroups = new ArrayList<>();
        Set<MethodInfo> processedMethods = new HashSet<>();
        
        for (int i = 0; i < filteredMethods.size(); i++) {
            MethodInfo method1 = filteredMethods.get(i);
            
            if (processedMethods.contains(method1)) {
                continue;
            }
            
            List<MethodInfo> similarMethods = new ArrayList<>();
            similarMethods.add(method1);
            processedMethods.add(method1);
            
            for (int j = i + 1; j < filteredMethods.size(); j++) {
                MethodInfo method2 = filteredMethods.get(j);
                
                if (processedMethods.contains(method2)) {
                    continue;
                }
                
                double similarity = calculateSimilarity(method1, method2);
                
                if (similarity >= similarityThreshold) {
                    similarMethods.add(method2);
                    processedMethods.add(method2);
                    log.debug("Found similar methods: {} and {} (similarity: {})", 
                               method1.getMethodName(), method2.getMethodName(), similarity);
                }
            }
            
            if (similarMethods.size() > 1) {
                double avgSimilarity = calculateAverageSimilarity(similarMethods);
                String similarityType = determineSimilarityType(similarMethods);
                
                DuplicateGroup group = new DuplicateGroup(similarMethods, avgSimilarity, similarityType);
                duplicateGroups.add(group);
                
                log.info("Created duplicate group with {} methods (similarity: {})", 
                           similarMethods.size(), avgSimilarity);
            }
        }
        
        log.info("Found {} duplicate groups", duplicateGroups.size());
        return duplicateGroups;
    }

    /**
     * Calculates similarity between two methods using multiple algorithms.
     */
    private double calculateSimilarity(MethodInfo method1, MethodInfo method2) {
        // Calculate different types of similarity
        double structuralSimilarity = calculateStructuralSimilarity(method1, method2);
        double contentSimilarity = calculateContentSimilarity(method1, method2);
        double signatureSimilarity = calculateSignatureSimilarity(method1, method2);
        
        // Weighted average of different similarity measures
        return structuralSimilarity * 0.5 + contentSimilarity * 0.3 + signatureSimilarity * 0.2;
    }

    /**
     * Calculates structural similarity based on method structure and control flow.
     */
    private double calculateStructuralSimilarity(MethodInfo method1, MethodInfo method2) {
        String body1 = normalizeMethodBody(method1.getBody());
        String body2 = normalizeMethodBody(method2.getBody());
        
        // Use Levenshtein distance for structural similarity
        int maxLength = Math.max(body1.length(), body2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = StringUtils.getLevenshteinDistance(body1, body2);
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Calculates content similarity based on actual code content.
     */
    private double calculateContentSimilarity(MethodInfo method1, MethodInfo method2) {
        String body1 = method1.getBody().toLowerCase();
        String body2 = method2.getBody().toLowerCase();
        
        // Extract tokens (words, operators, etc.)
        Set<String> tokens1 = extractTokens(body1);
        Set<String> tokens2 = extractTokens(body2);
        
        if (tokens1.isEmpty() && tokens2.isEmpty()) return 1.0;
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0;
        
        // Calculate Jaccard similarity
        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);
        
        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);
        
        double intersectionSize = intersection.size();
        double unionSize = union.size();
        return intersectionSize / unionSize;
    }

    /**
     * Calculates signature similarity based on method signatures.
     */
    private double calculateSignatureSimilarity(MethodInfo method1, MethodInfo method2) {
        String sig1 = method1.getSignature().toLowerCase();
        String sig2 = method2.getSignature().toLowerCase();
        
        // Extract return type and parameter types
        String returnType1 = extractReturnType(sig1);
        String returnType2 = extractReturnType(sig2);
        
        List<String> params1 = extractParameterTypes(sig1);
        List<String> params2 = extractParameterTypes(sig2);
        
        double returnTypeSimilarity = returnType1.equals(returnType2) ? 1.0 : 0.0;
        double paramSimilarity = calculateParameterSimilarity(params1, params2);
        
        return returnTypeSimilarity * 0.3 + paramSimilarity * 0.7;
    }

    /**
     * Normalizes method body by removing whitespace and comments.
     */
    private String normalizeMethodBody(String body) {
        return body.replaceAll("\\s+", " ")
                  .replaceAll("//.*?\\n", "")
                  .replaceAll("/\\*.*?\\*/", "")
                  .trim();
    }

    /**
     * Extracts tokens from method body.
     */
    private Set<String> extractTokens(String body) {
        return Arrays.stream(body.split("[\\s\\p{Punct}]+"))
                .filter(token -> token.length() > 1)
                .collect(Collectors.toSet());
    }

    /**
     * Extracts return type from method signature.
     */
    private String extractReturnType(String signature) {
        int spaceIndex = signature.indexOf(' ');
        if (spaceIndex == -1) return "";
        
        int parenIndex = signature.indexOf('(', spaceIndex);
        if (parenIndex == -1) return "";
        
        return signature.substring(spaceIndex + 1, parenIndex).trim();
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

    /**
     * Calculates similarity between parameter lists.
     */
    private double calculateParameterSimilarity(List<String> params1, List<String> params2) {
        if (params1.size() != params2.size()) {
            return 0.0;
        }
        
        int matchingParams = 0;
        for (int i = 0; i < params1.size(); i++) {
            if (params1.get(i).equals(params2.get(i))) {
                matchingParams++;
            }
        }
        
        double matchingParamsDouble = matchingParams;
        double paramsSize = params1.size();
        return matchingParamsDouble / paramsSize;
    }

    /**
     * Calculates average similarity within a group of methods.
     */
    private double calculateAverageSimilarity(List<MethodInfo> methods) {
        if (methods.size() <= 1) return 1.0;
        
        double totalSimilarity = 0.0;
        int comparisons = 0;
        
        for (int i = 0; i < methods.size(); i++) {
            for (int j = i + 1; j < methods.size(); j++) {
                totalSimilarity += calculateSimilarity(methods.get(i), methods.get(j));
                comparisons++;
            }
        }
        
        return comparisons > 0 ? totalSimilarity / comparisons : 1.0;
    }

    /**
     * Determines the type of similarity for the group.
     */
    private String determineSimilarityType(List<MethodInfo> methods) {
        if (methods.size() <= 1) return "EXACT";
        
        // Check if all methods have identical bodies
        String firstBody = normalizeMethodBody(methods.get(0).getBody());
        boolean allIdentical = methods.stream()
                .allMatch(method -> normalizeMethodBody(method.getBody()).equals(firstBody));
        
        if (allIdentical) return "EXACT";
        
        // Check if methods have identical signatures (same return type and parameters)
        boolean identicalSignatures = methods.stream()
                .map(MethodInfo::getSignature)
                .distinct()
                .count() == 1;
        
        if (identicalSignatures) return "SIGNATURE";
        
        // Check if methods have similar signatures (same parameter count and types)
        boolean similarSignatures = hasSimilarSignatures(methods);
        
        if (similarSignatures) return "SIGNATURE";
        
        // Check if methods have similar structural patterns
        boolean structuralSimilarity = hasStructuralSimilarity(methods);
        
        if (structuralSimilarity) return "STRUCTURAL";
        
        return "GENERIC";
    }
    
    /**
     * Checks if methods have similar signatures.
     */
    private boolean hasSimilarSignatures(List<MethodInfo> methods) {
        if (methods.size() < 2) return false;
        
        // Get parameter types for all methods
        List<List<String>> allParamTypes = methods.stream()
                .map(method -> extractParameterTypes(method.getSignature()))
                .collect(Collectors.toList());
        
        // Check if all methods have the same number of parameters
        int paramCount = allParamTypes.get(0).size();
        boolean sameParamCount = allParamTypes.stream()
                .allMatch(params -> params.size() == paramCount);
        
        if (!sameParamCount) return false;
        
        // Check if parameter types are similar (at least 50% match)
        if (paramCount > 0) {
            for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
                int finalParamIndex = paramIndex;
                Set<String> typesAtPosition = allParamTypes.stream()
                        .map(params -> params.get(finalParamIndex))
                        .collect(Collectors.toSet());
                
                // If more than half have the same type at this position
                if (typesAtPosition.size() <= Math.ceil(methods.size() / 2.0)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if methods have structural similarity.
     */
    private boolean hasStructuralSimilarity(List<MethodInfo> methods) {
        if (methods.size() < 2) return false;
        
        // Check for similar control flow patterns
        List<String> normalizedBodies = methods.stream()
                .map(method -> normalizeMethodBody(method.getBody()))
                .collect(Collectors.toList());
        
        // Look for common structural elements
        String firstBody = normalizedBodies.get(0);
        
        // Check if bodies contain similar structural patterns
        boolean hasLoops = firstBody.contains("for") || firstBody.contains("while");
        boolean hasConditionals = firstBody.contains("if") || firstBody.contains("switch");
        boolean hasMethodCalls = firstBody.contains("(") && firstBody.contains(")");
        
        // If the first method has these patterns, check if others do too
        if (hasLoops || hasConditionals || hasMethodCalls) {
            long methodsWithSimilarStructure = normalizedBodies.stream()
                    .filter(body -> {
                        boolean bodyHasLoops = body.contains("for") || body.contains("while");
                        boolean bodyHasConditionals = body.contains("if") || body.contains("switch");
                        boolean bodyHasMethodCalls = body.contains("(") && body.contains(")");
                        
                        return (hasLoops == bodyHasLoops) && 
                               (hasConditionals == bodyHasConditionals) && 
                               (hasMethodCalls == bodyHasMethodCalls);
                    })
                    .count();
            
            return methodsWithSimilarStructure >= methods.size() * 0.7; // 70% threshold
        }
        
        return false;
    }
}
