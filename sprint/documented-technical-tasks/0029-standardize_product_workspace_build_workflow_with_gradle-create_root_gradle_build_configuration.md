# Create Root Gradle Build Configuration

## Related User Story

User Story: standardize_product_workspace_build_workflow_with_gradle

## Objective

Create the root Gradle build configuration that orchestrates the entire product workspace with multi-project setup for backend and frontend modules.

## Scope

- Create root build.gradle with multi-project configuration
- Set up gradle.properties with workspace-wide settings
- Configure Gradle wrapper for consistent build environment
- Define common plugins and dependencies management
- Set up project structure for backend and frontend modules

## Out of Scope

- Module-specific build configurations
- CI/CD pipeline integration
- IDE-specific configurations
- Advanced Gradle optimization

## Clean Architecture Placement

infrastructure

## Execution Dependencies

None

## Implementation Details

Create root build.gradle with:
- Multi-project setup for backend and frontend
- Common plugin management (Java, Quarkus, Node.js)
- Dependency version management
- Common repositories configuration
- Workspace-wide tasks (clean, build, test)

Create gradle.properties with:
- Java version configuration
- Gradle JVM settings
- Build optimization flags
- Version properties for dependencies

Create gradle/wrapper/ with:
- Gradle wrapper jar and properties
- Consistent Gradle version across team
- Platform-independent build execution

Project structure:
```
root/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew / gradlew.bat
├── gradle/wrapper/
├── backend/
│   └── build.gradle
└── frontend/
    └── build.gradle
```

Root build.gradle configuration:
```gradle
plugins {
    id 'java' apply false
    id 'io.quarkus' version '3.6.0' apply false
    id 'com.github.node-gradle.node' version '7.0.1' apply false
}

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    test {
        useJUnitPlatform()
    }
}
```

## Files / Modules Impacted

- build.gradle (root)
- settings.gradle
- gradle.properties
- gradlew / gradlew.bat
- gradle/wrapper/gradle-wrapper.properties
- gradle/wrapper/gradle-wrapper.jar

## Acceptance Criteria

Given the root Gradle configuration is created
When ./gradlew projects is executed
Then all subprojects should be listed correctly

Given the Gradle wrapper is configured
When ./gradlew --version is executed
Then the correct Gradle version should be displayed

Given the multi-project setup is configured
When ./gradlew clean is executed
Then all subprojects should be cleaned

Given common settings are applied
When subproject builds are executed
Then consistent Java version and repositories should be used

## Testing Requirements

- Test Gradle wrapper execution
- Test multi-project structure recognition
- Test common plugin application
- Test dependency resolution
- Test workspace-wide task execution

## Dependencies / Preconditions

- Gradle 8.x must be available for wrapper generation
- Java 17 must be available for compilation