package com.duplicatedetector.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;

class MethodInfoTest {

    private MethodInfo methodInfo;
    private Path testPath;

    @BeforeEach
    void setUp() {
        testPath = Paths.get("src/test/TestClass.java");
        methodInfo = new MethodInfo(
            "TestClass",
            "testMethod",
            "public void testMethod()",
            "System.out.println(\"test\");",
            testPath,
            10,
            15,
            "com.test"
        );
    }

    @Test
    void testConstructorAndGetters() {
        assertEquals("TestClass", methodInfo.getClassName());
        assertEquals("testMethod", methodInfo.getMethodName());
        assertEquals("public void testMethod()", methodInfo.getSignature());
        assertEquals("System.out.println(\"test\");", methodInfo.getBody());
        assertEquals(testPath, methodInfo.getFilePath());
        assertEquals(10, methodInfo.getStartLine());
        assertEquals(15, methodInfo.getEndLine());
        assertEquals("com.test", methodInfo.getPackageName());
    }

    @Test
    void testGetFullMethodName() {
        assertEquals("com.test.TestClass.testMethod", methodInfo.getFullMethodName());
    }

    @Test
    void testGetMethodLength() {
        assertEquals(27, methodInfo.getMethodLength());
    }

    @Test
    void testGetMethodLengthWithNullBody() {
        MethodInfo nullBodyMethod = new MethodInfo(
            "TestClass", "testMethod", "public void testMethod()", 
            null, testPath, 10, 15, "com.test"
        );
        assertEquals(0, nullBodyMethod.getMethodLength());
    }

    @Test
    void testEquals() {
        MethodInfo sameMethod = new MethodInfo(
            "TestClass", "testMethod", "public void testMethod()",
            "System.out.println(\"test\");", testPath, 10, 15, "com.test"
        );
        assertEquals(methodInfo, sameMethod);
    }

    @Test
    void testNotEquals() {
        MethodInfo differentMethod = new MethodInfo(
            "TestClass", "differentMethod", "public void differentMethod()",
            "System.out.println(\"different\");", testPath, 10, 15, "com.test"
        );
        assertNotEquals(methodInfo, differentMethod);
    }

    @Test
    void testHashCode() {
        MethodInfo sameMethod = new MethodInfo(
            "TestClass", "testMethod", "public void testMethod()",
            "System.out.println(\"test\");", testPath, 10, 15, "com.test"
        );
        assertEquals(methodInfo.hashCode(), sameMethod.hashCode());
    }

    @Test
    void testToString() {
        String toString = methodInfo.toString();
        assertTrue(toString.contains("TestClass"));
        assertTrue(toString.contains("testMethod"));
        assertTrue(toString.contains("startLine=10"));
        assertTrue(toString.contains("endLine=15"));
    }

    @Test
    void testEqualsWithNull() {
        assertNotEquals(null, methodInfo);
    }

    @Test
    void testEqualsWithDifferentClass() {
        assertNotEquals("string", methodInfo);
    }
}
