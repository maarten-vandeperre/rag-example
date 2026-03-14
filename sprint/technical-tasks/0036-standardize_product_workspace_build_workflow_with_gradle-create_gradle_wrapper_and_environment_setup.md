# Create Gradle Wrapper and Environment Setup

## Related User Story

User Story: standardize_product_workspace_build_workflow_with_gradle

## Objective

Create Gradle wrapper configuration and environment setup scripts to ensure consistent build environment across all developer machines and CI/CD systems.

## Scope

- Generate and configure Gradle wrapper for consistent Gradle version
- Create environment validation and setup scripts
- Configure IDE integration files for common IDEs
- Add developer onboarding documentation
- Set up environment variable configuration

## Out of Scope

- CI/CD pipeline configuration
- Advanced IDE customization
- Operating system specific optimizations
- Docker-based development environment

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0029-standardize_product_workspace_build_workflow_with_gradle-create_root_gradle_build_configuration.md

## Implementation Details

Generate Gradle wrapper:
```bash
gradle wrapper --gradle-version 8.5 --distribution-type all
```

Configure gradle/wrapper/gradle-wrapper.properties:
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-all.zip
networkTimeout=10000
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

Create setup scripts:

setup.sh (Unix/Linux/macOS):
```bash
#!/bin/bash
set -e

echo "=== RAG Application Development Environment Setup ==="

# Check Java version
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    echo "Java version detected: $JAVA_VERSION"
    
    if [ "$JAVA_VERSION" != "17" ]; then
        echo "WARNING: Java 17 is required, but found Java $JAVA_VERSION"
        echo "Please install Java 17 before continuing."
        exit 1
    fi
else
    echo "ERROR: Java not found. Please install Java 17."
    exit 1
fi

# Check Node.js version
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version | cut -d'v' -f2 | cut -d'.' -f1)
    echo "Node.js version detected: $NODE_VERSION"
    
    if [ "$NODE_VERSION" -lt "18" ]; then
        echo "WARNING: Node.js 18+ is recommended, but found Node.js $NODE_VERSION"
    fi
else
    echo "ERROR: Node.js not found. Please install Node.js 18+."
    exit 1
fi

# Make gradlew executable
chmod +x gradlew

# Run health check
echo "Running environment health check..."
./gradlew healthCheck

# Install frontend dependencies
echo "Installing frontend dependencies..."
./gradlew :frontend:npmInstall

echo "=== Setup Complete ==="
echo "You can now run:"
echo "  ./gradlew dev      - Start development environment"
echo "  ./gradlew testAll  - Run all tests"
echo "  ./gradlew build    - Build the application"
```

setup.bat (Windows):
```batch
@echo off
echo === RAG Application Development Environment Setup ===

REM Check Java version
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java not found. Please install Java 17.
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
echo Java version detected: %JAVA_VERSION%

REM Check Node.js version
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Node.js not found. Please install Node.js 18+.
    exit /b 1
)

for /f %%i in ('node --version') do set NODE_VERSION=%%i
echo Node.js version detected: %NODE_VERSION%

REM Run health check
echo Running environment health check...
gradlew.bat healthCheck
if %errorlevel% neq 0 (
    echo Health check failed. Please review the errors above.
    exit /b 1
)

REM Install frontend dependencies
echo Installing frontend dependencies...
gradlew.bat :frontend:npmInstall

echo === Setup Complete ===
echo You can now run:
echo   gradlew.bat dev      - Start development environment
echo   gradlew.bat testAll  - Run all tests
echo   gradlew.bat build    - Build the application
```

IDE configuration files:

.vscode/settings.json (VS Code):
```json
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "java.gradle.buildServer.enabled": "on",
    "java.import.gradle.enabled": true,
    "java.import.gradle.wrapper.enabled": true,
    "files.exclude": {
        "**/build": true,
        "**/node_modules": true,
        "**/.gradle": true
    },
    "java.format.settings.url": ".vscode/eclipse-formatter.xml"
}
```

.vscode/launch.json (VS Code debug configuration):
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Debug Quarkus App",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005,
            "preLaunchTask": "startQuarkusDev"
        }
    ]
}
```

.vscode/tasks.json (VS Code tasks):
```json
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "startQuarkusDev",
            "type": "shell",
            "command": "./gradlew",
            "args": [":backend:quarkusDev"],
            "group": "build",
            "isBackground": true
        },
        {
            "label": "runTests",
            "type": "shell",
            "command": "./gradlew",
            "args": ["testAll"],
            "group": "test"
        }
    ]
}
```

.idea/gradle.xml (IntelliJ IDEA):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="GradleSettings">
    <option name="linkedExternalProjectsSettings">
      <GradleProjectSettings>
        <option name="distributionType" value="WRAPPER" />
        <option name="externalProjectPath" value="$PROJECT_DIR$" />
        <option name="gradleJvm" value="17" />
        <option name="modules">
          <set>
            <option value="$PROJECT_DIR$" />
            <option value="$PROJECT_DIR$/backend" />
            <option value="$PROJECT_DIR$/frontend" />
          </set>
        </option>
      </GradleProjectSettings>
    </option>
  </component>
</project>
```

Environment configuration (.env.example):
```bash
# Java Configuration
JAVA_HOME=/path/to/java17
GRADLE_OPTS=-Xmx2g -XX:MaxMetaspaceSize=512m

# Development Configuration
QUARKUS_DEV_PORT=8080
REACT_DEV_PORT=3000

# Database Configuration (for development)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=rag_app
DB_USER=rag_user
DB_PASSWORD=rag_password

# Build Configuration
GRADLE_PARALLEL=true
GRADLE_CACHING=true
```

## Files / Modules Impacted

- gradlew / gradlew.bat (Gradle wrapper scripts)
- gradle/wrapper/gradle-wrapper.properties
- gradle/wrapper/gradle-wrapper.jar
- setup.sh / setup.bat (environment setup scripts)
- .vscode/settings.json, launch.json, tasks.json
- .idea/gradle.xml
- .env.example
- README.md (update with setup instructions)

## Acceptance Criteria

Given the Gradle wrapper is configured
When ./gradlew --version is executed on any machine
Then the same Gradle version should be used consistently

Given the setup script is executed
When a new developer runs the setup
Then the environment should be validated and configured automatically

Given IDE configuration files are provided
When developers open the project in VS Code or IntelliJ
Then the project should be properly configured for development

Given environment variables are documented
When developers need to customize configuration
Then clear examples should be available

## Testing Requirements

- Test Gradle wrapper execution on different platforms
- Test setup scripts on Unix/Linux/macOS and Windows
- Test IDE configuration import
- Test environment validation
- Test developer onboarding workflow

## Dependencies / Preconditions

- Gradle must be available for initial wrapper generation
- Java 17 must be available on target systems
- Node.js 18+ must be available for frontend development
- Git must be available for version control