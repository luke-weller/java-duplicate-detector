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

The tool will prompt you to enter the project path.

## Example Output

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
   1. Review high severity duplications first
   2. Apply recommended design patterns
   3. Consider creating shared utility classes
   4. Update unit tests after refactoring
   5. Run the tool again to verify improvements

=== End of Analysis ===
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

## Performance

The tool is optimized for large projects:

- **Efficient Parsing**: Uses JavaParser for fast AST analysis
- **Streaming Processing**: Processes files incrementally to minimize memory usage
- **Smart Filtering**: Early filtering reduces computational complexity
- **Parallel Processing**: Ready for future parallel execution implementation

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
