package com.duplicatedetector.analyzer;

import com.duplicatedetector.model.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class MethodAnalyzerTest {

    private MethodAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new MethodAnalyzer();
    }

    @Test
    void testAnalyzeFileWithSimpleClass(@TempDir Path tempDir) throws IOException {
        String javaCode = "package com.test;\n\n" +
            "public class TestClass {\n" +
            "    public void testMethod() {\n" +
            "        System.out.println(\"Hello World\");\n" +
            "    }\n\n" +
            "    private int calculateSum(int a, int b) {\n" +
            "        return a + b;\n" +
            "    }\n" +
            "}\n";
        
        Path javaFile = tempDir.resolve("TestClass.java");
        Files.write(javaFile, javaCode.getBytes());
        
        List<MethodInfo> methods = analyzer.analyzeFile(javaFile);
        
        assertEquals(2, methods.size());
        
        MethodInfo method1 = methods.get(0);
        assertEquals("TestClass", method1.getClassName());
        assertEquals("testMethod", method1.getMethodName());
        assertEquals("com.test", method1.getPackageName());
        assertTrue(method1.getBody().contains("System.out.println"));
        
        MethodInfo method2 = methods.get(1);
        assertEquals("TestClass", method2.getClassName());
        assertEquals("calculateSum", method2.getMethodName());
        assertTrue(method2.getSignature().contains("int calculateSum(int a, int b)"));
    }

    @Test
    void testAnalyzeFileWithConstructor(@TempDir Path tempDir) throws IOException {
        String javaCode = "public class TestClass {\n" +
            "    public TestClass() {\n" +
            "        // Constructor should be ignored\n" +
            "    }\n\n" +
            "    public void normalMethod() {\n" +
            "        System.out.println(\"Normal method\");\n" +
            "    }\n" +
            "}\n";
        
        Path javaFile = tempDir.resolve("TestClass.java");
        Files.write(javaFile, javaCode.getBytes());
        
        List<MethodInfo> methods = analyzer.analyzeFile(javaFile);
        
        assertEquals(1, methods.size());
        assertEquals("normalMethod", methods.get(0).getMethodName());
    }

    @Test
    void testAnalyzeFileWithInterface(@TempDir Path tempDir) throws IOException {
        String javaCode = "package com.test;\n\n" +
            "public interface TestInterface {\n" +
            "    void method1();\n\n" +
            "    default void method2() {\n" +
            "        System.out.println(\"Default method\");\n" +
            "    }\n" +
            "}\n";
        
        Path javaFile = tempDir.resolve("TestInterface.java");
        Files.write(javaFile, javaCode.getBytes());
        
        List<MethodInfo> methods = analyzer.analyzeFile(javaFile);
        
        assertEquals(2, methods.size());
        assertEquals("TestInterface", methods.get(0).getClassName());
    }

    @Test
    void testAnalyzeFileWithMultipleClasses(@TempDir Path tempDir) throws IOException {
        String javaCode = "package com.test;\n\n" +
            "public class OuterClass {\n" +
            "    public void outerMethod() {\n" +
            "        System.out.println(\"Outer method\");\n" +
            "    }\n\n" +
            "    public static class InnerClass {\n" +
            "        public void innerMethod() {\n" +
            "            System.out.println(\"Inner method\");\n" +
            "        }\n" +
            "    }\n" +
            "}\n";
        
        Path javaFile = tempDir.resolve("OuterClass.java");
        Files.write(javaFile, javaCode.getBytes());
        
        List<MethodInfo> methods = analyzer.analyzeFile(javaFile);
        
        assertEquals(2, methods.size());
        
        boolean hasOuterMethod = methods.stream()
                .anyMatch(m -> m.getMethodName().equals("outerMethod"));
        boolean hasInnerMethod = methods.stream()
                .anyMatch(m -> m.getMethodName().equals("innerMethod"));
        
        assertTrue(hasOuterMethod);
        assertTrue(hasInnerMethod);
    }

    @Test
    void testAnalyzeFileWithNoMethods(@TempDir Path tempDir) throws IOException {
        String javaCode = "public class EmptyClass {\n" +
            "    // No methods\n" +
            "}\n";
        
        Path javaFile = tempDir.resolve("EmptyClass.java");
        Files.write(javaFile, javaCode.getBytes());
        
        List<MethodInfo> methods = analyzer.analyzeFile(javaFile);
        
        assertTrue(methods.isEmpty());
    }

    @Test
    void testAnalyzeMethodsWithMultipleFiles(@TempDir Path tempDir) throws IOException {
        // Create first file
        String javaCode1 = "public class Class1 {\n" +
            "    public void method1() {\n" +
            "        System.out.println(\"Method 1\");\n" +
            "    }\n" +
            "}\n";
        Path file1 = tempDir.resolve("Class1.java");
        Files.write(file1, javaCode1.getBytes());
        
        // Create second file
        String javaCode2 = "public class Class2 {\n" +
            "    public void method2() {\n" +
            "        System.out.println(\"Method 2\");\n" +
            "    }\n" +
            "}\n";
        Path file2 = tempDir.resolve("Class2.java");
        Files.write(file2, javaCode2.getBytes());
        
        List<Path> javaFiles = List.of(file1, file2);
        List<MethodInfo> methods = analyzer.analyzeMethods(javaFiles);
        
        assertEquals(2, methods.size());
    }

    @Test
    void testFilterMethodsByLength() {
        MethodInfo shortMethod = createMethodInfo("shortMethod", "return 0;");
        MethodInfo longMethod = createMethodInfo("longMethod", 
            "System.out.println(\"This is a very long method body with lots of content\");");
        
        List<MethodInfo> methods = List.of(shortMethod, longMethod);
        List<MethodInfo> filtered = analyzer.filterMethodsByLength(methods, 20);
        
        assertEquals(1, filtered.size());
        assertEquals("longMethod", filtered.get(0).getMethodName());
    }

    @Test
    void testFilterOutSimpleAccessors() {
        MethodInfo getter = createMethodInfo("getName", "return this.name;");
        MethodInfo setter = createMethodInfo("setName", "this.name = name;");
        MethodInfo complexMethod = createMethodInfo("processData", 
            "for (int i = 0; i < data.length; i++) { data[i] = data[i] * 2; }");
        
        List<MethodInfo> methods = List.of(getter, setter, complexMethod);
        List<MethodInfo> filtered = analyzer.filterOutSimpleAccessors(methods);
        
        assertEquals(1, filtered.size());
        assertEquals("processData", filtered.get(0).getMethodName());
    }

    private MethodInfo createMethodInfo(String methodName, String body) {
        return new MethodInfo(
            "TestClass", methodName, "public void " + methodName + "()",
            body, Path.of("TestClass.java"), 1, 5, "com.test"
        );
    }
}
