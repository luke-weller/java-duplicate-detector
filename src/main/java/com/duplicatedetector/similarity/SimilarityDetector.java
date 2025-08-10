package com.duplicatedetector.similarity;

import com.duplicatedetector.model.MethodInfo;
import com.duplicatedetector.model.DuplicateGroup;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Detects similar methods using various similarity algorithms with optimized performance.
 */
public class SimilarityDetector {
    private static final Logger log = LoggerFactory.getLogger(SimilarityDetector.class);
    
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;
    private static final int MIN_METHOD_LENGTH = 50;
    private static final int BATCH_SIZE = 1000; // Process methods in batches
    private static final int MAX_PARALLEL_THREADS = Runtime.getRuntime().availableProcessors();
    
    // Cache for similarity calculations to avoid recomputation
    private final Map<String, Double> similarityCache = new ConcurrentHashMap<>();
    
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
     * Optimized for large datasets with parallel processing and caching.
     * 
     * @param methods list of methods to analyze
     * @param similarityThreshold minimum similarity score (0.0 to 1.0)
     * @return list of duplicate groups
     */
    public List<DuplicateGroup> findSimilarMethods(List<MethodInfo> methods, double similarityThreshold) {
        log.info("Finding similar methods with threshold: {} (using {} threads)", 
                similarityThreshold, MAX_PARALLEL_THREADS);
        
        long startTime = System.currentTimeMillis();
        
        // Filter out very short methods early
        List<MethodInfo> filteredMethods = methods.stream()
                .filter(method -> method.getMethodLength() >= MIN_METHOD_LENGTH)
                .collect(Collectors.toList());
        
        log.info("Analyzing {} methods (filtered from {} total)", 
                   filteredMethods.size(), methods.size());
        
        if (filteredMethods.size() < 2) {
            log.info("Not enough methods to analyze for duplicates");
            return new ArrayList<>();
        }
        
        // Pre-compute method signatures and create method groups for faster comparison
        Map<String, List<MethodInfo>> methodGroups = preprocessMethods(filteredMethods);
        
        // Find similar methods using optimized algorithm
        List<DuplicateGroup> duplicateGroups = findSimilarMethodsOptimized(
            filteredMethods, methodGroups, similarityThreshold);
        
        long endTime = System.currentTimeMillis();
        log.info("Found {} duplicate groups in {} ms", 
                duplicateGroups.size(), endTime - startTime);
        
        return duplicateGroups;
    }

    /**
     * Preprocesses methods to group them by signature characteristics for faster comparison.
     */
    private Map<String, List<MethodInfo>> preprocessMethods(List<MethodInfo> methods) {
        Map<String, List<MethodInfo>> groups = new HashMap<>();
        
        for (MethodInfo method : methods) {
            // Create a key based on method characteristics for grouping
            String key = createMethodGroupKey(method);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(method);
        }
        
        log.debug("Created {} method groups for optimized comparison", groups.size());
        return groups;
    }

    /**
     * Creates a grouping key based on method characteristics to reduce unnecessary comparisons.
     */
    private String createMethodGroupKey(MethodInfo method) {
        // Group by parameter count and method length range
        int paramCount = extractParameterTypes(method.getSignature()).size();
        int lengthRange = method.getMethodLength() / 100; // Group by 100-character ranges
        
        return paramCount + "_" + lengthRange;
    }

    /**
     * Optimized method to find similar methods using parallel processing and smart grouping.
     */
    private List<DuplicateGroup> findSimilarMethodsOptimized(
            List<MethodInfo> methods, 
            Map<String, List<MethodInfo>> methodGroups, 
            double similarityThreshold) {
        
        List<DuplicateGroup> duplicateGroups = Collections.synchronizedList(new ArrayList<>());
        Set<MethodInfo> processedMethods = Collections.synchronizedSet(new HashSet<>());
        
        // Process methods in parallel batches
        ExecutorService executor = Executors.newFixedThreadPool(MAX_PARALLEL_THREADS);
        
        try {
            // Create tasks for parallel processing
            List<Future<Void>> futures = new ArrayList<>();
            
            for (List<MethodInfo> group : methodGroups.values()) {
                if (group.size() > 1) {
                    futures.add(executor.submit(() -> {
                        processMethodGroup(group, methods, similarityThreshold, 
                                        duplicateGroups, processedMethods);
                        return null;
                    }));
                }
            }
            
            // Wait for all tasks to complete
            for (Future<Void> future : futures) {
                future.get();
            }
            
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during parallel processing", e);
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
        
        return duplicateGroups;
    }

    /**
     * Processes a group of methods to find similarities within the group and with other methods.
     */
    private void processMethodGroup(List<MethodInfo> group, List<MethodInfo> allMethods,
                                  double similarityThreshold, List<DuplicateGroup> duplicateGroups,
                                  Set<MethodInfo> processedMethods) {
        
        for (int i = 0; i < group.size(); i++) {
            MethodInfo method1 = group.get(i);
            
            if (processedMethods.contains(method1)) {
                continue;
            }
            
            List<MethodInfo> similarMethods = new ArrayList<>();
            similarMethods.add(method1);
            processedMethods.add(method1);
            
            // Compare with other methods in the same group first (likely to be similar)
            for (int j = i + 1; j < group.size(); j++) {
                MethodInfo method2 = group.get(j);
                
                if (processedMethods.contains(method2)) {
                    continue;
                }
                
                double similarity = calculateSimilarityCached(method1, method2);
                
                if (similarity >= similarityThreshold) {
                    similarMethods.add(method2);
                    processedMethods.add(method2);
                    log.debug("Found similar methods: {} and {} (similarity: {})", 
                               method1.getMethodName(), method2.getMethodName(), similarity);
                }
            }
            
            // Also compare with methods from other groups (cross-group similarities)
            for (MethodInfo method2 : allMethods) {
                if (processedMethods.contains(method2) || similarMethods.contains(method2)) {
                    continue;
                }
                
                // Quick pre-filter to avoid expensive similarity calculation
                if (shouldSkipComparison(method1, method2)) {
                    continue;
                }
                
                double similarity = calculateSimilarityCached(method1, method2);
                
                if (similarity >= similarityThreshold) {
                    similarMethods.add(method2);
                    processedMethods.add(method2);
                    log.debug("Found similar methods: {} and {} (similarity: {})", 
                               method1.getMethodName(), method2.getMethodName(), similarity);
                }
            }
            
            if (similarMethods.size() > 1) {
                double avgSimilarity = calculateAverageSimilarityOptimized(similarMethods);
                String similarityType = determineSimilarityType(similarMethods);
                
                DuplicateGroup groupResult = new DuplicateGroup(similarMethods, avgSimilarity, similarityType);
                duplicateGroups.add(groupResult);
                
                log.info("Created duplicate group with {} methods (similarity: {})", 
                           similarMethods.size(), avgSimilarity);
            }
        }
    }

    /**
     * Quick pre-filter to skip obviously different methods.
     */
    private boolean shouldSkipComparison(MethodInfo method1, MethodInfo method2) {
        // Skip if method lengths are too different
        int lengthDiff = Math.abs(method1.getMethodLength() - method2.getMethodLength());
        int maxLength = Math.max(method1.getMethodLength(), method2.getMethodLength());
        
        if (maxLength > 0 && (double) lengthDiff / maxLength > 0.5) {
            return true; // Skip if length difference is more than 50%
        }
        
        // Skip if parameter counts are too different
        List<String> params1 = extractParameterTypes(method1.getSignature());
        List<String> params2 = extractParameterTypes(method2.getSignature());
        
        if (Math.abs(params1.size() - params2.size()) > 2) {
            return true; // Skip if parameter count difference is more than 2
        }
        
        return false;
    }

    /**
     * Calculates similarity between two methods using cached results.
     */
    private double calculateSimilarityCached(MethodInfo method1, MethodInfo method2) {
        String cacheKey = createCacheKey(method1, method2);
        
        return similarityCache.computeIfAbsent(cacheKey, k -> {
            return calculateSimilarity(method1, method2);
        });
    }

    /**
     * Creates a cache key for two methods (order-independent).
     */
    private String createCacheKey(MethodInfo method1, MethodInfo method2) {
        // Ensure consistent ordering for cache key
        if (method1.hashCode() < method2.hashCode()) {
            return method1.hashCode() + "_" + method2.hashCode();
        } else {
            return method2.hashCode() + "_" + method1.hashCode();
        }
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
     * Optimized to use faster string operations.
     */
    private double calculateStructuralSimilarity(MethodInfo method1, MethodInfo method2) {
        String body1 = normalizeMethodBody(method1.getBody());
        String body2 = normalizeMethodBody(method2.getBody());
        
        // Use faster similarity calculation for large strings
        if (body1.length() > 1000 || body2.length() > 1000) {
            return calculateFastStructuralSimilarity(body1, body2);
        }
        
        // Use Levenshtein distance for smaller strings
        int maxLength = Math.max(body1.length(), body2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = StringUtils.getLevenshteinDistance(body1, body2);
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Fast structural similarity calculation for large method bodies.
     */
    private double calculateFastStructuralSimilarity(String body1, String body2) {
        // Use character frequency analysis for large strings
        Map<Character, Integer> freq1 = getCharacterFrequency(body1);
        Map<Character, Integer> freq2 = getCharacterFrequency(body2);
        
        Set<Character> allChars = new HashSet<>(freq1.keySet());
        allChars.addAll(freq2.keySet());
        
        if (allChars.isEmpty()) return 1.0;
        
        double totalDiff = 0.0;
        for (char c : allChars) {
            int count1 = freq1.getOrDefault(c, 0);
            int count2 = freq2.getOrDefault(c, 0);
            totalDiff += Math.abs(count1 - count2);
        }
        
        int maxLength = Math.max(body1.length(), body2.length());
        return 1.0 - (totalDiff / (2.0 * maxLength));
    }

    /**
     * Gets character frequency map for a string.
     */
    private Map<Character, Integer> getCharacterFrequency(String str) {
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : str.toCharArray()) {
            freq.merge(c, 1, Integer::sum);
        }
        return freq;
    }

    /**
     * Calculates content similarity based on actual code content.
     * Optimized with better tokenization.
     */
    private double calculateContentSimilarity(MethodInfo method1, MethodInfo method2) {
        String body1 = method1.getBody().toLowerCase();
        String body2 = method2.getBody().toLowerCase();
        
        // Extract tokens (words, operators, etc.)
        Set<String> tokens1 = extractTokensOptimized(body1);
        Set<String> tokens2 = extractTokensOptimized(body2);
        
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
     * Optimized token extraction with better filtering.
     */
    private Set<String> extractTokensOptimized(String body) {
        return Arrays.stream(body.split("[\\s\\p{Punct}]+"))
                .filter(token -> token.length() > 1 && !isCommonJavaKeyword(token))
                .collect(Collectors.toSet());
    }

    /**
     * Checks if a token is a common Java keyword that should be filtered out.
     */
    private boolean isCommonJavaKeyword(String token) {
        Set<String> commonKeywords = Set.of(
            "public", "private", "protected", "static", "final", "abstract",
            "class", "interface", "extends", "implements", "return", "if",
            "else", "for", "while", "do", "switch", "case", "default",
            "try", "catch", "finally", "throw", "throws", "new", "this",
            "super", "import", "package", "void", "int", "long", "double",
            "float", "boolean", "char", "byte", "short", "null", "true", "false"
        );
        return commonKeywords.contains(token);
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
     * Calculates average similarity within a group of methods (optimized).
     */
    private double calculateAverageSimilarityOptimized(List<MethodInfo> methods) {
        if (methods.size() <= 1) return 1.0;
        
        // For large groups, sample a subset to avoid O(n²) complexity
        if (methods.size() > 10) {
            return calculateSampledAverageSimilarity(methods);
        }
        
        double totalSimilarity = 0.0;
        int comparisons = 0;
        
        for (int i = 0; i < methods.size(); i++) {
            for (int j = i + 1; j < methods.size(); j++) {
                totalSimilarity += calculateSimilarityCached(methods.get(i), methods.get(j));
                comparisons++;
            }
        }
        
        return comparisons > 0 ? totalSimilarity / comparisons : 1.0;
    }

    /**
     * Calculates average similarity using sampling for large groups.
     */
    private double calculateSampledAverageSimilarity(List<MethodInfo> methods) {
        int sampleSize = Math.min(10, methods.size());
        List<MethodInfo> sample = methods.subList(0, sampleSize);
        
        double totalSimilarity = 0.0;
        int comparisons = 0;
        
        for (int i = 0; i < sample.size(); i++) {
            for (int j = i + 1; j < sample.size(); j++) {
                totalSimilarity += calculateSimilarityCached(sample.get(i), sample.get(j));
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
