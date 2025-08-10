package com.duplicatedetector.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents a group of methods that are considered similar or duplicate.
 */
public class DuplicateGroup {
    private final List<MethodInfo> methods;
    private final double similarityScore;
    private final String similarityType;

    public DuplicateGroup(List<MethodInfo> methods, double similarityScore, String similarityType) {
        this.methods = methods;
        this.similarityScore = similarityScore;
        this.similarityType = similarityType;
    }

    public List<MethodInfo> getMethods() {
        return methods;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public String getSimilarityType() {
        return similarityType;
    }

    public int getGroupSize() {
        return methods.size();
    }

    public boolean hasMultipleMethods() {
        return methods.size() > 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DuplicateGroup that = (DuplicateGroup) o;
        return Double.compare(that.similarityScore, similarityScore) == 0 && 
               Objects.equals(methods, that.methods) && 
               Objects.equals(similarityType, that.similarityType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methods, similarityScore, similarityType);
    }

    @Override
    public String toString() {
        return "DuplicateGroup{" +
                "methods=" + methods +
                ", similarityScore=" + similarityScore +
                ", similarityType='" + similarityType + '\'' +
                '}';
    }
}
