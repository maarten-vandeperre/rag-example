# Upgrade to Java 25

## Related User Story

User Story: improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25

## Objective

Upgrade the entire workspace from Java 17 to Java 25, updating all build configurations, dependencies, and code to leverage Java 25 features while maintaining backward compatibility and existing functionality.

## Scope

- Update all Gradle build configurations to use Java 25
- Update Docker configurations and container images
- Update CI/CD configurations for Java 25
- Verify and update all dependencies for Java 25 compatibility
- Update code to leverage Java 25 features where beneficial
- Update documentation and setup scripts

## Out of Scope

- Major refactoring to use advanced Java 25 features
- Performance optimization specific to Java 25
- Breaking changes to existing APIs
- Migration of external dependencies that don't support Java 25

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0042-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-create_shared_kernel_module.md

## Implementation Details

Update root Gradle configuration:
```gradle
// Root build.gradle
allprojects {
    group = 'com.rag.app'
    version = '1.0.0-SNAPSHOT'
    
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

subprojects {
    apply plugin: 'java'
    
    java {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }
    
    tasks.withType(JavaCompile) {
        options.release = 25
        options.compilerArgs += [
            '--enable-preview',  // If using preview features
            '-Xlint:all',
            '-Xlint:-serial'
        ]
    }
    
    test {
        useJUnitPlatform()
        jvmArgs += [
            '--enable-preview'  // If using preview features
        ]
    }
}
```

Update gradle.properties:
```properties
# Java 25 configuration
org.gradle.java.home=/path/to/java25
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g --enable-preview

# Build optimization
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true

# Java 25 specific flags
systemProp.java.version=25
systemProp.java.specification.version=25
```

Update backend Dockerfile:
```dockerfile
# Build stage
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Copy gradle files
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
COPY */build.gradle ./

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source and build
COPY . .
RUN ./gradlew build --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Install required packages
RUN apk add --no-cache curl

# Create application user
RUN addgroup -g 1001 -S appuser && \
    adduser -S appuser -u 1001 -G appuser

# Copy application
COPY --from=build /app/backend/build/quarkus-app/ ./
RUN chown -R appuser:appuser /app

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/q/health || exit 1

# JVM arguments for Java 25
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 --enable-preview"

EXPOSE 8080
CMD ["java", "-jar", "quarkus-run.jar"]
```

Update dependency versions for Java 25 compatibility:
```gradle
// Backend dependencies (backend/build.gradle)
ext {
    quarkusVersion = '3.15.0'  // Latest version with Java 25 support
    postgresqlVersion = '42.7.0'
    pdfboxVersion = '3.0.1'
    testcontainersVersion = '1.19.0'
}

dependencies {
    implementation enforcedPlatform("io.quarkus:quarkus-bom:${quarkusVersion}")
    implementation 'io.quarkus:quarkus-resteasy-reactive-jackson'
    implementation 'io.quarkus:quarkus-jdbc-postgresql'
    implementation 'io.quarkus:quarkus-smallrye-health'
    implementation "org.postgresql:postgresql:${postgresqlVersion}"
    implementation "org.apache.pdfbox:pdfbox:${pdfboxVersion}"
    
    testImplementation 'io.quarkus:quarkus-junit5'
    testImplementation 'io.rest-assured:rest-assured'
    testImplementation "org.testcontainers:postgresql:${testcontainersVersion}"
    testImplementation "org.testcontainers:junit-jupiter:${testcontainersVersion}"
}
```

Update application configuration for Java 25:
```properties
# application.properties
# Java 25 specific configurations
quarkus.native.java-version=25
quarkus.jib.base-jvm-image=eclipse-temurin:25-jre-alpine

# JVM configuration
quarkus.jvm.args=--enable-preview,-XX:+UseG1GC,-XX:MaxRAMPercentage=75.0

# Logging configuration
quarkus.log.level=INFO
quarkus.log.category."com.rag.app".level=DEBUG
```

Leverage Java 25 features in code:
```java
// Use pattern matching for instanceof (if not already using)
public String processDocument(Object document) {
    return switch (document) {
        case PdfDocument pdf -> processPdf(pdf);
        case MarkdownDocument md -> processMarkdown(md);
        case TextDocument txt -> processText(txt);
        case null -> throw new IllegalArgumentException("Document cannot be null");
        default -> throw new UnsupportedOperationException("Unsupported document type: " + document.getClass());
    };
}

// Use records for DTOs (if not already using)
public record DocumentSummaryDto(
    String documentId,
    String fileName,
    long fileSize,
    String fileType,
    String status,
    String uploadedBy,
    Instant uploadedAt
) {}

// Use text blocks for SQL queries
public class DocumentQueries {
    public static final String FIND_BY_USER = """
        SELECT document_id, file_name, file_size, file_type, status, 
               uploaded_by, uploaded_at, last_updated
        FROM documents 
        WHERE uploaded_by = ? 
        ORDER BY uploaded_at DESC
        """;
    
    public static final String GET_PROCESSING_STATISTICS = """
        SELECT 
            COUNT(*) as total_documents,
            COUNT(CASE WHEN status = 'UPLOADED' THEN 1 END) as uploaded_count,
            COUNT(CASE WHEN status = 'PROCESSING' THEN 1 END) as processing_count,
            COUNT(CASE WHEN status = 'READY' THEN 1 END) as ready_count,
            COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_count
        FROM documents
        """;
}

// Use sealed classes for domain modeling (if beneficial)
public sealed interface DocumentStatus 
    permits DocumentStatus.Uploaded, DocumentStatus.Processing, 
            DocumentStatus.Ready, DocumentStatus.Failed {
    
    record Uploaded() implements DocumentStatus {}
    record Processing(Instant startedAt) implements DocumentStatus {}
    record Ready(Instant completedAt) implements DocumentStatus {}
    record Failed(String reason, Instant failedAt) implements DocumentStatus {}
}
```

Update setup scripts for Java 25:
```bash
# setup.sh
#!/bin/bash
set -e

echo "=== RAG Application Development Environment Setup (Java 25) ==="

# Check Java version
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    echo "Java version detected: $JAVA_VERSION"
    
    if [ "$JAVA_VERSION" != "25" ]; then
        echo "ERROR: Java 25 is required, but found Java $JAVA_VERSION"
        echo "Please install Java 25 before continuing."
        echo "Download from: https://jdk.java.net/25/"
        exit 1
    fi
else
    echo "ERROR: Java not found. Please install Java 25."
    exit 1
fi

# Check JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
    echo "WARNING: JAVA_HOME is not set. Please set it to your Java 25 installation."
fi

# Continue with rest of setup...
```

Update IDE configurations:
```json
// .vscode/settings.json
{
    "java.configuration.runtimes": [
        {
            "name": "JavaSE-25",
            "path": "/path/to/java25"
        }
    ],
    "java.compile.nullAnalysis.mode": "automatic",
    "java.configuration.updateBuildConfiguration": "automatic"
}
```

## Files / Modules Impacted

- build.gradle (root - Java 25 configuration)
- gradle.properties (Java 25 settings)
- */build.gradle (all module build files)
- backend/Dockerfile (Java 25 base image)
- frontend/Dockerfile (if using Java for build tools)
- docker-compose.yml (Java 25 images)
- setup.sh / setup.bat (Java 25 validation)
- .vscode/settings.json (Java 25 IDE configuration)
- .idea/misc.xml (IntelliJ Java 25 configuration)
- backend/src/main/resources/application.properties

## Acceptance Criteria

Given the workspace is upgraded to Java 25
When the application is built and run
Then it should work successfully on Java 25 runtime

Given Java 25 features are leveraged
When appropriate code patterns are used
Then the code should be more maintainable and readable

Given all dependencies are updated
When the application is built
Then all dependencies should be compatible with Java 25

Given the development environment is updated
When developers set up the workspace
Then they should be guided to use Java 25

## Testing Requirements

- Test application startup on Java 25
- Test all existing functionality works unchanged
- Test build process with Java 25
- Test Docker containers with Java 25
- Test development workflow with Java 25

## Dependencies / Preconditions

- Java 25 must be available for development and runtime
- All dependencies must support Java 25
- Docker base images with Java 25 must be available
- Module structure should be established before upgrade