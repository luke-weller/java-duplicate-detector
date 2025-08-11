# Java Duplicate Detector

A powerful tool to scan Java projects and identify duplicate or similar logic that can be refactored. Built with a focus on scalability, readability, and maintainability using Test-Driven Development (TDD).

## Features

- **Comprehensive Scanning**: Scans entire Java projects recursively, excluding build directories
- **Advanced Similarity Analysis**: Uses sophisticated algorithms to detect similar methods based on structure, content, and patterns
- **Configurable Thresholds**: Adjustable similarity thresholds for different detection sensitivity
- **Enhanced Detailed Reporting**: Provides comprehensive analysis with:
  - **Similarity Type Classification**: Exact, Signature, Structural, or Generic similarity
  - **Severity Assessment**: High, Medium, or Low priority issues
  - **Refactoring Recommendations**: Specific design patterns and best practices
  - **Code Quality Insights**: Method length analysis, naming suggestions, and architectural concerns
  - **Cross-Class Analysis**: Identifies duplications across different classes
  - **Parameter Pattern Detection**: Common parameter types and validation patterns
  - **Control Flow Analysis**: Identifies similar loops, conditionals, and method call patterns
  - **File Reports**: Generates detailed text reports saved to `reports/` directory with timestamps
- **Actionable Recommendations**: Specific refactoring suggestions for each type of duplication
- **Smart Filtering**: Automatically filters out short methods and simple accessors (getters/setters)
- **Robust Error Handling**: Graceful handling of parsing errors and invalid files
- **Extensive Testing**: Comprehensive unit tests with 90%+ code coverage

## Architecture

The project follows a clean, modular architecture with clear separation of concerns:

```
src/main/java/com/duplicatedetector/
├── DuplicateDetector.java          # Main application entry point
├── model/
│   ├── MethodInfo.java             # Data model for method information
│   └── DuplicateGroup.java         # Data model for groups of similar methods
├── scanner/
│   └── ProjectScanner.java         # Scans project directories for Java files
├── analyzer/
│   └── MethodAnalyzer.java         # Parses Java files and extracts method info
└── similarity/
    └── SimilarityDetector.java     # Detects similar methods using various algorithms
```

## Similarity Detection Algorithms

The tool uses a combination of three similarity measures:

1. **Structural Similarity (50% weight)**: Based on method structure and control flow using Levenshtein distance
2. **Content Similarity (30% weight)**: Token-based similarity using Jaccard index
3. **Signature Similarity (20% weight)**: Method signature comparison (return type, parameters)

## Requirements

- Java 11 or higher
- Maven 3.6 or higher

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd java-duplicate-detector
```

2. Build the project:
```bash
mvn clean compile
```

3. Run tests:
```bash
mvn test
```

## Usage

### Command Line

Run the tool with a project path as an argument:

```bash
mvn exec:java -Dexec.mainClass="com.duplicatedetector.DuplicateDetector" -Dexec.args="/path/to/your/java/project"
```

Or build and run the JAR:

```bash
mvn package
java -jar target/java-duplicate-detector-1.0.0.jar /path/to/your/java/project
```

### Interactive Mode

Run without arguments for interactive mode:

```bash
mvn exec:java -Dexec.mainClass="com.duplicatedetector.DuplicateDetector"
```

The tool will prompt you to:
1. Enter the project path
2. Select a performance configuration:
   - Default (balanced performance and accuracy)
   - High Performance (optimized for large projects)
   - High Accuracy (more thorough analysis)
   - Memory Constrained (for limited memory environments)
   - Custom configuration (user-defined settings)

### Programmatic Usage

Use the tool programmatically with custom performance settings:

```java
import com.duplicatedetector.DuplicateDetector;
import com.duplicatedetector.PerformanceConfig;

// Create detector
DuplicateDetector detector = new DuplicateDetector();

// Use default configuration
detector.run(Paths.get("/path/to/project"));

// Use high-performance configuration for large projects
PerformanceConfig config = PerformanceConfig.forLargeProjects();
detector.run(Paths.get("/path/to/project"), config);

// Custom configuration
PerformanceConfig customConfig = new PerformanceConfig();
customConfig.setSimilarityThreshold(0.8);
customConfig.setMaxParallelThreads(8);
customConfig.setBatchSize(2000);
detector.run(Paths.get("/path/to/project"), customConfig);
```

## Output and Reports

The tool generates both console output and detailed file reports.

### Console Output

The console displays a comprehensive analysis with emojis and formatting for easy reading:

```
=== Java Duplicate Detector - Detailed Analysis ===
🔍 Found 2 groups of similar methods requiring attention:

🚨 Group 1: Exact Code Duplication (100.0% similarity)
📋 Methods involved (2 methods across 2 classes):
   • DataProcessor.processData() in src/main/java/com/test/DataProcessor.java (lines 15-45)
   • ArrayProcessor.processData() in src/main/java/com/test/ArrayProcessor.java (lines 23-53)

⚠️  Cross-class duplication detected!
   Consider creating a shared utility class or service

🎯 Recommended Pattern: Extract Method + Strategy Pattern
💡 Refactoring Recommendations:
   • Extract the identical code into a shared utility method
   • Consider creating a common base class or interface
   • Apply the Template Method pattern if the methods follow similar algorithms
   • Use composition over inheritance to share common functionality

🔧 Code Quality Insights:
   • Methods have identical names - consider more descriptive naming
   • Methods are spread across 2 different classes

⚠️ Group 2: Signature Similarity (78.9% similarity)
📋 Methods involved (2 methods across 1 class):
   • DataProcessor.calculateSum() in src/main/java/com/test/DataProcessor.java (lines 47-67)
   • DataProcessor.calculateAverage() in src/main/java/com/test/DataProcessor.java (lines 69-89)

🎯 Recommended Pattern: Strategy Pattern + Interface Segregation
💡 Refactoring Recommendations:
   • Create a common interface defining the contract
   • Apply the Strategy pattern to handle different implementations
   • Consider using generics to make the methods more flexible
   • Extract common parameter validation logic

🔧 Code Quality Insights:
   • Method calculateSum is quite long (156 chars) - consider breaking it down
   • Method calculateAverage is quite long (158 chars) - consider breaking it down

📊 Analysis Insights:
   • Common parameter types detected: List<Integer>
🔗 Common Parameter Types: List<Integer>

📈 Summary:
   • Total duplicate methods: 4
   • High severity issues: 1
   • Cross-class duplications: 1
   • Groups requiring immediate attention: 1

🚨 Priority: Focus on high severity issues first!

💡 Next Steps:
   1. Review groups in order (highest similarity first)
   2. Focus on high severity duplications
   3. Apply recommended design patterns
   4. Consider creating shared utility classes
   5. Update unit tests after refactoring
   6. Run the tool again to verify improvements

=== End of Analysis ===
📄 Detailed report saved to: /path/to/project/reports/duplicate-detection-report_20250810_161016.txt
```

### File Reports

The tool automatically generates detailed text reports saved to a `reports/` directory in the project root. Reports include:

- **Header Information**: Generation timestamp, project path, and configuration used
- **Summary Statistics**: Total groups, methods, severity breakdown, and cross-class issues
- **Detailed Analysis**: Complete analysis for each duplicate group with:
  - Method details with file paths and line numbers
  - Severity assessment and recommendations
  - Code quality insights and architectural suggestions
  - Parameter pattern analysis
- **Recommendations**: Actionable next steps for refactoring
- **Footer**: Tool attribution and completion markers

**Report File Location**: `{project-root}/reports/duplicate-detection-report_YYYYMMDD_HHMMSS.txt`

**Example Report Structure**:
```
================================================================================
JAVA DUPLICATE DETECTOR - DETAILED ANALYSIS REPORT
================================================================================
Generated: 2025-08-10 16:10:16
Project: /path/to/project
Configuration: PerformanceConfig{...}

================================================================================

SUMMARY STATISTICS
----------------------------------------
Total duplicate groups: 3
Total duplicate methods: 6
High severity issues: 1
Cross-class duplications: 1

DETAILED ANALYSIS
================================================================================

Group 1: Exact Code Duplication (100.0 similarity)
------------------------------------------------------------
Severity: HIGH
Methods involved (2 methods across 2 classes):
  • RefactoringAnalyzer.extractParameterTypes() in src/main/java/.../RefactoringAnalyzer.java (lines 244-256)
  • SimilarityDetector.extractParameterTypes() in src/main/java/.../SimilarityDetector.java (lines 440-452)

⚠️  Cross-class duplication detected!
   Consider creating a shared utility class or service

Recommended Pattern: Extract Method + Strategy Pattern

Refactoring Recommendations:
  • Extract the identical code into a shared utility method
  • Consider creating a common base class or interface
  • Apply the Template Method pattern if the methods follow similar algorithms
  • Use composition over inheritance to share common functionality

Code Quality Insights:
  • Method extractParameterTypes is quite long (433 chars) - consider breaking it down
  • Methods have identical names - consider more descriptive naming
  • Methods are spread across 2 different classes

...

RECOMMENDATIONS AND NEXT STEPS
================================================================================
1. Review groups in order (highest similarity first)
2. Focus on high severity duplications
3. Apply recommended design patterns
4. Consider creating shared utility classes
5. Update unit tests after refactoring
6. Run the tool again to verify improvements

================================================================================
Report generated by Java Duplicate Detector
================================================================================
```

## Configuration

### Similarity Threshold

The default similarity threshold is 0.7 (70%). Methods with similarity scores below this threshold are not considered duplicates.

### Minimum Method Length

Methods shorter than 50 characters are automatically excluded from analysis to avoid false positives.

### Excluded Directories

The following directories are automatically excluded from scanning:
- `target/`, `build/`, `out/`, `bin/` (build outputs)
- `.git/`, `.svn/` (version control)
- `.idea/`, `.vscode/` (IDE files)

## Development

### Running Tests

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn jacoco:report

# Run specific test class
mvn test -Dtest=SimilarityDetectorTest
```

### Code Quality

The project follows strict coding standards:

- **TDD Approach**: All features developed with tests first
- **Clean Code**: Meaningful names, small methods, clear structure
- **SOLID Principles**: Single responsibility, dependency injection
- **Error Handling**: Comprehensive exception handling and logging
- **Documentation**: Javadoc for all public methods

### Adding New Similarity Algorithms

To add a new similarity algorithm:

1. Create a new method in `SimilarityDetector.java`
2. Add the algorithm to the `calculateSimilarity()` method
3. Update the weights in the weighted average calculation
4. Add corresponding unit tests

### Extending the Tool

The modular architecture makes it easy to extend:

- **New File Types**: Extend `ProjectScanner` to support other languages
- **Custom Filters**: Add new filtering logic to `MethodAnalyzer`
- **Output Formats**: Create new output formatters for different report types
- **Integration**: Add CI/CD integration or IDE plugins

## Performance Optimizations

The tool has been significantly optimized for large projects with thousands of methods:

### 🚀 Performance Improvements

- **Parallel Processing**: Multi-threaded execution for file scanning, method analysis, and similarity detection
- **Smart Caching**: Similarity calculations are cached to avoid recomputation
- **Early Filtering**: Methods are filtered early based on length, signature, and content to reduce comparisons
- **Batch Processing**: Large datasets are processed in configurable batches
- **Optimized Algorithms**: Fast similarity algorithms for large method bodies using character frequency analysis
- **Memory Management**: Configurable memory limits and garbage collection optimization

### 📊 Performance Configurations

The tool provides different performance profiles:

1. **Default**: Balanced performance and accuracy for most projects
2. **High Performance**: Optimized for large projects (>1000 methods)
3. **High Accuracy**: More thorough analysis for smaller projects
4. **Memory Constrained**: For environments with limited memory

### 🎯 Performance Metrics

- **Files processed per second**: 100-500 files/second (depending on size)
- **Methods analyzed per second**: 50-200 methods/second
- **Similarity comparisons per second**: 1000-5000 comparisons/second
- **Memory usage**: 50-200 MB for typical projects

### 🔧 Performance Tuning

Use the `PerformanceConfig` class to customize performance settings:

```java
// For large projects
PerformanceConfig config = PerformanceConfig.forLargeProjects();
config.setMaxParallelThreads(8);
config.setBatchSize(2000);

// For memory-constrained environments
PerformanceConfig config = PerformanceConfig.forMemoryConstrained();
config.setMaxMethodsInMemory(10000);
config.setEnableSimilarityCache(false);

// Custom configuration
PerformanceConfig config = new PerformanceConfig();
config.setSimilarityThreshold(0.8);
config.setMinMethodLength(100);
config.setMaxParallelThreads(4);
```

### 📈 Benchmarking

Run performance benchmarks to compare configurations:

```bash
mvn exec:java -Dexec.mainClass="com.duplicatedetector.PerformanceBenchmark" -Dexec.args="/path/to/project"
```

This will test all configurations and provide detailed performance metrics.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests first (TDD approach)
4. Implement the feature
5. Ensure all tests pass
6. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [JavaParser](https://javaparser.org/) for Java AST parsing
- [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/) for string utilities
- [JUnit 5](https://junit.org/junit5/) for testing framework
