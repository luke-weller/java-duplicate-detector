package com.duplicatedetector.analyzer;

import com.duplicatedetector.model.MethodInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains detailed analysis information about duplicate methods and refactoring recommendations.
 */
public class RefactoringAnalysis {
    private String similarityType;
    private double similarityScore;
    private List<MethodInfo> methods;
    private String duplicationType;
    private String severity;
    private List<String> recommendations;
    private String refactoringPattern;
    private boolean crossClassIssue;
    private String crossClassRecommendation;
    private List<String> commonParameters;
    private List<String> insights;
    private List<String> codeQualityInsights;
    
    public RefactoringAnalysis() {
        this.recommendations = new ArrayList<>();
        this.insights = new ArrayList<>();
        this.codeQualityInsights = new ArrayList<>();
        this.commonParameters = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getSimilarityType() {
        return similarityType;
    }
    
    public void setSimilarityType(String similarityType) {
        this.similarityType = similarityType;
    }
    
    public double getSimilarityScore() {
        return similarityScore;
    }
    
    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }
    
    public List<MethodInfo> getMethods() {
        return methods;
    }
    
    public void setMethods(List<MethodInfo> methods) {
        this.methods = methods;
    }
    
    public String getDuplicationType() {
        return duplicationType;
    }
    
    public void setDuplicationType(String duplicationType) {
        this.duplicationType = duplicationType;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public List<String> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
    
    public String getRefactoringPattern() {
        return refactoringPattern;
    }
    
    public void setRefactoringPattern(String refactoringPattern) {
        this.refactoringPattern = refactoringPattern;
    }
    
    public boolean isCrossClassIssue() {
        return crossClassIssue;
    }
    
    public void setCrossClassIssue(boolean crossClassIssue) {
        this.crossClassIssue = crossClassIssue;
    }
    
    public String getCrossClassRecommendation() {
        return crossClassRecommendation;
    }
    
    public void setCrossClassRecommendation(String crossClassRecommendation) {
        this.crossClassRecommendation = crossClassRecommendation;
    }
    
    public List<String> getCommonParameters() {
        return commonParameters;
    }
    
    public void setCommonParameters(List<String> commonParameters) {
        this.commonParameters = commonParameters;
    }
    
    public List<String> getInsights() {
        return insights;
    }
    
    public void setInsights(List<String> insights) {
        this.insights = insights;
    }
    
    public List<String> getCodeQualityInsights() {
        return codeQualityInsights;
    }
    
    public void setCodeQualityInsights(List<String> codeQualityInsights) {
        this.codeQualityInsights = codeQualityInsights;
    }
    
    /**
     * Adds an insight to the analysis.
     */
    public void addInsight(String insight) {
        this.insights.add(insight);
    }
    
    /**
     * Gets the formatted similarity percentage.
     */
    public String getFormattedSimilarityPercentage() {
        return String.format("%.1f", similarityScore * 100);
    }
    
    /**
     * Gets the number of methods in this group.
     */
    public int getMethodCount() {
        return methods != null ? methods.size() : 0;
    }
    
    /**
     * Gets the number of unique classes involved.
     */
    public long getUniqueClassCount() {
        return methods != null ? 
            methods.stream().map(MethodInfo::getClassName).distinct().count() : 0;
    }
}
