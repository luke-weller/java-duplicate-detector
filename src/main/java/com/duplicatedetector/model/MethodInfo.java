package com.duplicatedetector.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents information about a Java method extracted from source code.
 */
public class MethodInfo {
    private final String className;
    private final String methodName;
    private final String signature;
    private final String body;
    private final Path filePath;
    private final int startLine;
    private final int endLine;
    private final String packageName;

    public MethodInfo(String className, String methodName, String signature, String body, 
                     Path filePath, int startLine, int endLine, String packageName) {
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
        this.body = body;
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getSignature() {
        return signature;
    }

    public String getBody() {
        return body;
    }

    public Path getFilePath() {
        return filePath;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getFullMethodName() {
        return packageName + "." + className + "." + methodName;
    }

    public int getMethodLength() {
        return body != null ? body.length() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInfo that = (MethodInfo) o;
        return startLine == that.startLine && 
               endLine == that.endLine && 
               Objects.equals(className, that.className) && 
               Objects.equals(methodName, that.methodName) && 
               Objects.equals(signature, that.signature) && 
               Objects.equals(filePath, that.filePath) && 
               Objects.equals(packageName, that.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, signature, filePath, startLine, endLine, packageName);
    }

    @Override
    public String toString() {
        return "MethodInfo{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", filePath=" + filePath +
                ", startLine=" + startLine +
                ", endLine=" + endLine +
                '}';
    }
}
