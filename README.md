# Java Duplicate Detector

A powerful tool to scan Java projects and identify duplicate or similar logic that can be refactored. Built with a focus on scalability, readability, and maintainability using Test-Driven Development (TDD).

## Features

- **Comprehensive Scanning**: Scans entire Java projects recursively, excluding build directories
- **Multiple Similarity Algorithms**: Uses structural, content, and signature-based similarity detection
- **Configurable Thresholds**: Adjustable similarity thresholds for different detection sensitivity
- **Detailed Reporting**: Provides detailed console output with method locations and similarity scores
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
=== Java Duplicate Detector ===
This tool scans Java projects to find duplicate or similar methods.

Enter the path to your Java project: /path/to/project

=== Duplicate Detection Results ===
Found 2 groups of similar methods:

Group 1 (Similarity: 85.2%):
  - DataProcessor.processData() in src/main/java/com/test/DataProcessor.java (lines 15-45)
  - ArrayProcessor.processData() in src/main/java/com/test/ArrayProcessor.java (lines 23-53)
  Suggested refactoring: Extract common logic into a shared method

Group 2 (Similarity: 78.9%):
  - DataProcessor.calculateSum() in src/main/java/com/test/DataProcessor.java (lines 47-67)
  - ArrayProcessor.calculateSum() in src/main/java/com/test/ArrayProcessor.java (lines 55-75)
  Suggested refactoring: Extract common logic into a shared method

=== End of Results ===
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
