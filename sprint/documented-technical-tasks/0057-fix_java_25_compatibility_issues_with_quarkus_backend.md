# Fix Java 25 Compatibility Issues with Quarkus Backend

## Related User Story

User Story: standardize_local_development_environment_with_podman

## Objective

Resolve Java 25 compatibility issues preventing Quarkus backend from starting in development mode due to unsupported class file major version 69.

## Scope

- Identify and fix Java version compatibility issues
- Update Quarkus and dependency versions to support Java 25
- Configure Gradle build to handle Java 25 properly
- Ensure development mode works correctly
- Update any deprecated Gradle configurations

## Out of Scope

- Production deployment configuration changes
- Frontend compatibility issues
- Database schema changes
- Complete framework migration

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0056-standardize_local_development_environment_with_podman-fix_weaviate_schema_initialization_issues.md

## Implementation Details

The error indicates that ASM (bytecode manipulation library used by Quarkus) doesn't support Java 25's class file format (major version 69). This requires:

1. **Update Quarkus Version**: Upgrade to a version that supports Java 25
2. **Update Gradle Configuration**: Fix deprecated configuration warnings
3. **Update Dependencies**: Ensure all dependencies support Java 25
4. **Configure Source Sets**: Fix integration test and native test configuration issues

### Key Changes Needed:

1. **Update backend/build.gradle**:
   - Upgrade Quarkus BOM to latest version
   - Fix deprecated configuration warnings
   - Ensure proper source set configuration

2. **Update gradle.properties**:
   - Set appropriate Java version compatibility
   - Configure Gradle JVM arguments for Java 25

3. **Update Gradle Wrapper** (if needed):
   - Ensure Gradle version supports Java 25

4. **Fix Source Set Configuration**:
   - Resolve integrationTest and nativeTest configuration conflicts
   - Ensure proper source set creation order

### Expected Quarkus Version Update:
- Current: Likely 3.x.x (based on error)
- Target: 3.6+ or latest stable version with Java 25 support

### Gradle Configuration Fixes:
```gradle
// Fix source set creation order
sourceSets {
    integrationTest {
        // Define before accessing configurations
    }
    nativeTest {
        // Define before accessing configurations  
    }
}

configurations {
    integrationTestImplementation.extendsFrom testImplementation
    integrationTestRuntimeOnly.extendsFrom testRuntimeOnly
    nativeTestImplementation.extendsFrom testImplementation
    nativeTestRuntimeOnly.extendsFrom testRuntimeOnly
}
```

## Files / Modules Impacted

- backend/build.gradle (Quarkus version and configuration)
- gradle.properties (Java version settings)
- gradle/wrapper/gradle-wrapper.properties (if Gradle update needed)
- backend/src/main/resources/application.properties (if configuration updates needed)

## Acceptance Criteria

Given the Java 25 compatibility fixes are applied
When ./gradlew :backend:quarkusDev is executed
Then Quarkus development mode should start successfully without ASM version errors

Given the Gradle configuration is fixed
When the build is executed
Then no deprecated configuration warnings should appear

Given the backend is running in development mode
When accessing the application endpoints
Then they should respond correctly

Given the development environment is fully started
When both backend and frontend are running
Then the complete RAG application should be functional

## Testing Requirements

- Test Quarkus development mode startup
- Test application endpoint accessibility
- Test hot reload functionality in development mode
- Test integration with development services (PostgreSQL, Weaviate, etc.)
- Verify no deprecated warnings in build output

## Dependencies / Preconditions

- Development services must be running (PostgreSQL, Weaviate, Keycloak, Redis)
- Java 25 must be installed and configured
- Gradle wrapper must be compatible with Java 25
- Network connectivity to Maven repositories for dependency updates

## Root Cause Analysis

The error occurs because:
1. Java 25 uses class file major version 69
2. ASM library version used by current Quarkus doesn't support version 69
3. Quarkus development mode tries to analyze class files and fails
4. Gradle configuration has deprecated patterns that need updating

## Recovery Steps

If the fix doesn't work:
1. Check Java version: `java -version`
2. Check Gradle compatibility: `./gradlew --version`
3. Verify Quarkus version supports Java 25
4. Consider temporary downgrade to Java 21 LTS if needed
5. Check Quarkus release notes for Java 25 support status