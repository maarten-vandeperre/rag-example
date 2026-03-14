# Configure Gradle Test Execution

## Related User Story

User Story: standardize_product_workspace_build_workflow_with_gradle

## Objective

Configure comprehensive test execution through Gradle with proper reporting, parallel execution, and integration with existing test suites.

## Scope

- Configure JUnit 5 test execution for backend
- Configure Jest test execution for frontend
- Set up test reporting and result aggregation
- Configure parallel test execution where appropriate
- Add test categorization (unit, integration, e2e)

## Out of Scope

- Test case implementation
- Test data management
- Performance testing configuration
- Security testing integration

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0030-standardize_product_workspace_build_workflow_with_gradle-create_backend_gradle_build_configuration.md
- 0031-standardize_product_workspace_build_workflow_with_gradle-create_frontend_gradle_build_configuration.md

## Implementation Details

Backend test configuration (extend backend/build.gradle):
```gradle
test {
    useJUnitPlatform()
    
    systemProperty 'java.util.logging.manager', 'org.jboss.logmanager.LogManager'
    
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
        showStandardStreams = false
    }
    
    reports {
        html.required = true
        junitXml.required = true
    }
    
    finalizedBy jacocoTestReport
}

// Integration tests
task integrationTest(type: Test) {
    group = 'verification'
    description = 'Runs integration tests'
    useJUnitPlatform {
        includeTags 'integration'
    }
    
    shouldRunAfter test
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
}

// Unit tests only
task unitTest(type: Test) {
    group = 'verification'
    description = 'Runs unit tests only'
    useJUnitPlatform {
        excludeTags 'integration'
    }
}

// Code coverage
jacoco {
    toolVersion = "0.8.8"
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
    
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/dto/**',
                '**/config/**'
            ])
        }))
    }
}
```

Frontend test configuration (extend frontend/build.gradle):
```gradle
// Unit tests with coverage
task test(type: NpmTask) {
    group = 'verification'
    description = 'Runs React unit tests with coverage'
    dependsOn 'npmInstall'
    npmCommand = ['test', '--', '--coverage', '--watchAll=false', '--testResultsProcessor=jest-junit']
    
    inputs.dir('src')
    outputs.dir('coverage')
    outputs.file('junit.xml')
}

// Component tests
task testComponents(type: NpmTask) {
    group = 'verification'
    description = 'Runs React component tests'
    dependsOn 'npmInstall'
    npmCommand = ['test', '--', '--testPathPattern=components', '--watchAll=false']
}

// E2E tests (if using Cypress or similar)
task testE2E(type: NpmTask) {
    group = 'verification'
    description = 'Runs end-to-end tests'
    dependsOn 'npmInstall'
    npmCommand = ['run', 'test:e2e']
}
```

Root test aggregation (extend root build.gradle):
```gradle
// Test report aggregation
task testReport(type: TestReport) {
    group = 'verification'
    description = 'Aggregates test reports from all modules'
    destinationDir = file("${buildDir}/reports/allTests")
    
    reportOn subprojects*.test
}

// All tests with reporting
task testAllWithReport {
    group = 'verification'
    description = 'Runs all tests and generates aggregated report'
    dependsOn 'testAll', 'testReport'
    
    doLast {
        println "Test execution completed."
        println "Aggregated report available at: ${buildDir}/reports/allTests/index.html"
    }
}

// Continuous testing
task testContinuous {
    group = 'verification'
    description = 'Runs tests continuously on file changes'
    dependsOn ':backend:test', ':frontend:dev'
}
```

Test categorization with JUnit 5 tags:
```java
// Example test annotations
@Test
@Tag("unit")
void shouldValidateDocumentSize() { }

@Test
@Tag("integration")
void shouldUploadDocumentSuccessfully() { }
```

## Files / Modules Impacted

- backend/build.gradle (extend test configuration)
- frontend/build.gradle (extend test configuration)
- build.gradle (root - add test aggregation)
- backend/src/test/resources/junit-platform.properties
- frontend/jest.config.js (if needed)

## Acceptance Criteria

Given the test configuration is complete
When ./gradlew test is executed
Then all unit tests should run with proper reporting

Given integration tests are configured
When ./gradlew integrationTest is executed
Then integration tests should run separately from unit tests

Given test reporting is configured
When ./gradlew testAllWithReport is executed
Then an aggregated test report should be generated

Given test categorization is implemented
When specific test categories are executed
Then only tests with matching tags should run

## Testing Requirements

- Test JUnit 5 platform configuration
- Test Jest integration with Gradle
- Test report generation and aggregation
- Test parallel execution capabilities
- Test failure handling and reporting

## Dependencies / Preconditions

- Backend and frontend build configurations must exist
- JUnit 5 and Jest must be properly configured
- Test source code must exist with proper annotations
- Test dependencies must be available