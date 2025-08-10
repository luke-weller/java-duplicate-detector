package com.duplicatedetector.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class DuplicateGroupTest {

    private DuplicateGroup duplicateGroup;
    private MethodInfo method1;
    private MethodInfo method2;
    private Path testPath;

    @BeforeEach
    void setUp() {
        testPath = Paths.get("src/test/TestClass.java");
        method1 = new MethodInfo(
            "TestClass", "method1", "public void method1()",
            "System.out.println(\"test1\");", testPath, 10, 15, "com.test"
        );
        method2 = new MethodInfo(
            "TestClass", "method2", "public void method2()",
            "System.out.println(\"test2\");", testPath, 20, 25, "com.test"
        );
        
        List<MethodInfo> methods = Arrays.asList(method1, method2);
        duplicateGroup = new DuplicateGroup(methods, 0.85, "STRUCTURAL");
    }

    @Test
    void testConstructorAndGetters() {
        assertEquals(2, duplicateGroup.getMethods().size());
        assertEquals(0.85, duplicateGroup.getSimilarityScore(), 0.001);
        assertEquals("STRUCTURAL", duplicateGroup.getSimilarityType());
    }

    @Test
    void testGetGroupSize() {
        assertEquals(2, duplicateGroup.getGroupSize());
    }

    @Test
    void testHasMultipleMethods() {
        assertTrue(duplicateGroup.hasMultipleMethods());
    }

    @Test
    void testHasMultipleMethodsWithSingleMethod() {
        List<MethodInfo> singleMethod = Collections.singletonList(method1);
        DuplicateGroup singleGroup = new DuplicateGroup(singleMethod, 0.5, "EXACT");
        assertFalse(singleGroup.hasMultipleMethods());
    }

    @Test
    void testEquals() {
        List<MethodInfo> sameMethods = Arrays.asList(method1, method2);
        DuplicateGroup sameGroup = new DuplicateGroup(sameMethods, 0.85, "STRUCTURAL");
        assertEquals(duplicateGroup, sameGroup);
    }

    @Test
    void testNotEquals() {
        List<MethodInfo> differentMethods = Collections.singletonList(method1);
        DuplicateGroup differentGroup = new DuplicateGroup(differentMethods, 0.85, "STRUCTURAL");
        assertNotEquals(duplicateGroup, differentGroup);
    }

    @Test
    void testNotEqualsWithDifferentScore() {
        List<MethodInfo> sameMethods = Arrays.asList(method1, method2);
        DuplicateGroup differentScoreGroup = new DuplicateGroup(sameMethods, 0.90, "STRUCTURAL");
        assertNotEquals(duplicateGroup, differentScoreGroup);
    }

    @Test
    void testNotEqualsWithDifferentType() {
        List<MethodInfo> sameMethods = Arrays.asList(method1, method2);
        DuplicateGroup differentTypeGroup = new DuplicateGroup(sameMethods, 0.85, "EXACT");
        assertNotEquals(duplicateGroup, differentTypeGroup);
    }

    @Test
    void testHashCode() {
        List<MethodInfo> sameMethods = Arrays.asList(method1, method2);
        DuplicateGroup sameGroup = new DuplicateGroup(sameMethods, 0.85, "STRUCTURAL");
        assertEquals(duplicateGroup.hashCode(), sameGroup.hashCode());
    }

    @Test
    void testToString() {
        String toString = duplicateGroup.toString();
        assertTrue(toString.contains("DuplicateGroup"));
    }

    @Test
    void testEqualsWithNull() {
        assertNotEquals(null, duplicateGroup);
    }

    @Test
    void testEqualsWithDifferentClass() {
        assertNotEquals("string", duplicateGroup);
    }

    @Test
    void testEmptyGroup() {
        DuplicateGroup emptyGroup = new DuplicateGroup(Collections.emptyList(), 0.0, "NONE");
        assertEquals(0, emptyGroup.getGroupSize());
        assertFalse(emptyGroup.hasMultipleMethods());
    }
}
