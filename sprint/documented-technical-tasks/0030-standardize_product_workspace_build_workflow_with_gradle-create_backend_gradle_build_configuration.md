# Create Backend Gradle Build Configuration

## Related User Story

User Story: standardize_product_workspace_build_workflow_with_gradle

## Objective

Create Gradle build configuration for the Quarkus backend module with development mode, testing, and build tasks.

## Scope

- Create backend/build.gradle with Quarkus plugin configuration
- Configure development mode task for live reload
- Set up test execution with proper classpath
- Configure build and packaging tasks
- Add dependency management for Quarkus and related libraries

## Out of Scope

- Frontend build integration
- Docker image building through Gradle
- Database migration tasks
- Production deployment configuration

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0029-standardize_product_workspace_build_workflow_with_gradle-create_root_gradle_build_configuration.md

## Implementation Details

Create backend/build.gradle with:
- Quarkus plugin application and configuration
- Java compilation settings
- Dependency declarations for Quarkus, JDBC, testing
- Custom tasks for development workflow
- Test configuration with proper resources

Backend build.gradle configuration:
```gradle
plugins {
    id 'java'
    id 'io.quarkus'
}

dependencies {
    implementation enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusVersion}")
    implementation 'io.quarkus:quarkus-resteasy-reactive-jackson'
    implementation 'io.quarkus:quarkus-jdbc-postgresql'
    implementation 'io.quarkus:quarkus-smallrye-health'
    implementation 'org.apache.pdfbox:pdfbox:3.0.0'
    
    testImplementation 'io.quarkus:quarkus-junit5'
    testImplementation 'io.rest-assured:rest-assured'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:junit-jupiter'
}

quarkus {
    buildNative {
        enabled = false
    }
}

// Development mode task
task dev {
    group = 'application'
    description = 'Runs the application in development mode'
    dependsOn 'quarkusDev'
}

// Enhanced test task
test {
    systemProperty 'java.util.logging.manager', 'org.jboss.logmanager.LogManager'
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
}

// Build verification task
task verify {
    group = 'verification'
    description = 'Runs all verification tasks'
    dependsOn 'test', 'build'
}
```

Development mode configuration:
- Live reload enabled
- Hot deployment for code changes
- Automatic test execution on change
- Debug port configuration

Test configuration:
- JUnit 5 platform
- Testcontainers for integration tests
- Proper logging configuration
- Test resource management

## Files / Modules Impacted

- backend/build.gradle
- backend/gradle.properties (if needed)
- backend/src/main/resources/application.properties (Gradle-specific config)

## Acceptance Criteria

Given the backend Gradle configuration is created
When ./gradlew :backend:dev is executed
Then the Quarkus application should start in development mode

Given the test configuration is set up
When ./gradlew :backend:test is executed
Then all backend tests should run and report results

Given the build configuration is complete
When ./gradlew :backend:build is executed
Then the backend should compile and package successfully

Given development mode is running
When source code is modified
Then the application should automatically reload

## Testing Requirements

- Test Quarkus development mode startup
- Test automatic reload functionality
- Test test execution and reporting
- Test build and packaging process
- Test dependency resolution

## Dependencies / Preconditions

- Root Gradle configuration must exist
- Quarkus plugin must be available
- Java 17 must be configured
- Backend source code structure must exist