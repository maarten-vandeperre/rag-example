# Create Gradle Integration Tests

## Related User Story

User Story: standardize_product_workspace_build_workflow_with_gradle

## Objective

Create comprehensive integration tests that validate the complete Gradle workflow functionality including development mode, testing, building, and release preparation.

## Scope

- Create integration tests for Gradle workflow validation
- Test development mode startup and functionality
- Test build and packaging processes
- Test error handling and failure scenarios
- Validate workflow consistency across different environments

## Out of Scope

- Application functionality testing (covered by application tests)
- Performance testing of build processes
- CI/CD pipeline testing
- Cross-platform compatibility testing

## Clean Architecture Placement

testing

## Execution Dependencies

- 0032-standardize_product_workspace_build_workflow_with_gradle-create_workspace_development_tasks.md
- 0033-standardize_product_workspace_build_workflow_with_gradle-configure_gradle_test_execution.md
- 0034-standardize_product_workspace_build_workflow_with_gradle-configure_gradle_build_and_packaging.md
- 0035-standardize_product_workspace_build_workflow_with_gradle-configure_gradle_error_handling_and_reporting.md
- 0036-standardize_product_workspace_build_workflow_with_gradle-create_gradle_wrapper_and_environment_setup.md

## Implementation Details

Create integration test structure:
```
src/test/integration/
├── gradle/
│   ├── WorkflowIntegrationTest.java
│   ├── BuildProcessTest.java
│   ├── DevelopmentModeTest.java
│   └── ErrorHandlingTest.java
└── resources/
    ├── test-projects/
    └── expected-outputs/
```

WorkflowIntegrationTest.java:
```java
@TestMethodOrder(OrderAnnotation.class)
class WorkflowIntegrationTest {
    
    private static final Path PROJECT_ROOT = Paths.get("").toAbsolutePath();
    private static final String GRADLEW = System.getProperty("os.name").toLowerCase().contains("windows") 
        ? "gradlew.bat" : "./gradlew";
    
    @Test
    @Order(1)
    void shouldValidateEnvironmentSetup() throws Exception {
        ProcessResult result = executeGradleTask("healthCheck");
        
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.output()).contains("Health Check Passed");
    }
    
    @Test
    @Order(2)
    void shouldInstallFrontendDependencies() throws Exception {
        ProcessResult result = executeGradleTask(":frontend:npmInstall");
        
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(PROJECT_ROOT.resolve("frontend/node_modules")).exists();
    }
    
    @Test
    @Order(3)
    void shouldRunAllTests() throws Exception {
        ProcessResult result = executeGradleTask("testAll");
        
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.output()).contains("BUILD SUCCESSFUL");
    }
    
    @Test
    @Order(4)
    void shouldBuildWorkspace() throws Exception {
        ProcessResult result = executeGradleTask("buildWorkspace");
        
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(PROJECT_ROOT.resolve("backend/build/quarkus-app/quarkus-run.jar")).exists();
        assertThat(PROJECT_ROOT.resolve("frontend/build/index.html")).exists();
    }
    
    @Test
    @Order(5)
    void shouldPrepareRelease() throws Exception {
        ProcessResult result = executeGradleTask("prepareRelease");
        
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.output()).contains("Release preparation completed");
    }
    
    @Test
    @Order(6)
    void shouldPackageRelease() throws Exception {
        ProcessResult result = executeGradleTask("packageRelease");
        
        assertThat(result.exitCode()).isEqualTo(0);
        
        Path releasePackage = PROJECT_ROOT.resolve("build/distributions")
            .resolve("rag-example-1.0.0-SNAPSHOT.zip");
        assertThat(releasePackage).exists();
    }
    
    private ProcessResult executeGradleTask(String task) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(GRADLEW, task);
        pb.directory(PROJECT_ROOT.toFile());
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        
        return new ProcessResult(exitCode, output);
    }
    
    record ProcessResult(int exitCode, String output) {}
}
```

DevelopmentModeTest.java:
```java
class DevelopmentModeTest {
    
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(3);
    private static final String BACKEND_URL = "http://localhost:8080";
    private static final String FRONTEND_URL = "http://localhost:3000";
    
    @Test
    void shouldStartDevelopmentMode() throws Exception {
        // Start development mode in background
        ProcessBuilder pb = new ProcessBuilder("./gradlew", "dev");
        pb.directory(Paths.get("").toAbsolutePath().toFile());
        Process devProcess = pb.start();
        
        try {
            // Wait for backend to start
            await().atMost(STARTUP_TIMEOUT)
                .pollInterval(Duration.ofSeconds(5))
                .until(() -> isServiceAvailable(BACKEND_URL + "/q/health"));
            
            // Wait for frontend to start
            await().atMost(STARTUP_TIMEOUT)
                .pollInterval(Duration.ofSeconds(5))
                .until(() -> isServiceAvailable(FRONTEND_URL));
            
            // Verify services are responding
            assertThat(getHttpResponse(BACKEND_URL + "/q/health")).contains("UP");
            assertThat(getHttpResponse(FRONTEND_URL)).contains("<title>");
            
        } finally {
            // Clean shutdown
            devProcess.destroyForcibly();
            devProcess.waitFor(30, TimeUnit.SECONDS);
        }
    }
    
    private boolean isServiceAvailable(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            return connection.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String getHttpResponse(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        try (InputStream is = connection.getInputStream()) {
            return new String(is.readAllBytes());
        }
    }
}
```

BuildProcessTest.java:
```java
class BuildProcessTest {
    
    @Test
    void shouldBuildBackendSuccessfully() throws Exception {
        ProcessResult result = executeGradleTask(":backend:build");
        
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.output()).contains("BUILD SUCCESSFUL");
        
        // Verify JAR file exists and is valid
        Path jarFile = Paths.get("backend/build/quarkus-app/quarkus-run.jar");
        assertThat(jarFile).exists();
        assertThat(Files.size(jarFile)).isGreaterThan(1024); // At least 1KB
    }
    
    @Test
    void shouldBuildFrontendSuccessfully() throws Exception {
        ProcessResult result = executeGradleTask(":frontend:buildProd");
        
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.output()).contains("Frontend build completed");
        
        // Verify build artifacts
        Path buildDir = Paths.get("frontend/build");
        assertThat(buildDir.resolve("index.html")).exists();
        assertThat(buildDir.resolve("static")).exists();
    }
    
    @Test
    void shouldRunTestsWithReporting() throws Exception {
        ProcessResult result = executeGradleTask("testAllWithReport");
        
        assertThat(result.exitCode()).isEqualTo(0);
        
        // Verify test reports exist
        Path testReport = Paths.get("build/reports/allTests/index.html");
        assertThat(testReport).exists();
    }
    
    private ProcessResult executeGradleTask(String task) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("./gradlew", task);
        pb.directory(Paths.get("").toAbsolutePath().toFile());
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        
        return new ProcessResult(exitCode, output);
    }
    
    record ProcessResult(int exitCode, String output) {}
}
```

ErrorHandlingTest.java:
```java
class ErrorHandlingTest {
    
    @Test
    void shouldProvideHelpfulErrorMessageOnFailure() throws Exception {
        // Simulate a build failure by temporarily corrupting a file
        Path buildFile = Paths.get("backend/build.gradle");
        String originalContent = Files.readString(buildFile);
        
        try {
            // Introduce syntax error
            Files.writeString(buildFile, "invalid gradle syntax");
            
            ProcessResult result = executeGradleTask(":backend:build");
            
            assertThat(result.exitCode()).isNotEqualTo(0);
            assertThat(result.output()).contains("BUILD FAILED");
            assertThat(result.output()).contains("For escalation to lead architect");
            assertThat(result.output()).contains("Gradle version:");
            assertThat(result.output()).contains("Java version:");
            
        } finally {
            // Restore original file
            Files.writeString(buildFile, originalContent);
        }
    }
    
    @Test
    void shouldCollectDiagnosticInformation() throws Exception {
        ProcessResult result = executeGradleTask("diagnostics");
        
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.output()).contains("Diagnostic Information");
        assertThat(result.output()).contains("Gradle Version:");
        assertThat(result.output()).contains("Java Version:");
        assertThat(result.output()).contains("Available Tasks:");
    }
    
    private ProcessResult executeGradleTask(String task) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("./gradlew", task);
        pb.directory(Paths.get("").toAbsolutePath().toFile());
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        
        return new ProcessResult(exitCode, output);
    }
    
    record ProcessResult(int exitCode, String output) {}
}
```

Integration test configuration (extend root build.gradle):
```gradle
// Integration test configuration
sourceSets {
    integrationTest {
        java {
            srcDir 'src/test/integration'
        }
        resources {
            srcDir 'src/test/integration/resources'
        }
        compileClasspath += sourceSets.main.output + sourceSets.test.output
        runtimeClasspath += sourceSets.main.output + sourceSets.test.output
    }
}

configurations {
    integrationTestImplementation.extendsFrom testImplementation
    integrationTestRuntimeOnly.extendsFrom testRuntimeOnly
}

task integrationTest(type: Test) {
    group = 'verification'
    description = 'Runs Gradle workflow integration tests'
    testClassesDirs = sourceSets.integrationTest.output.classesDirs
    classpath = sourceSets.integrationTest.runtimeClasspath
    
    shouldRunAfter test
    
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
}

dependencies {
    integrationTestImplementation 'org.junit.jupiter:junit-jupiter'
    integrationTestImplementation 'org.assertj:assertj-core'
    integrationTestImplementation 'org.awaitility:awaitility'
}
```

## Files / Modules Impacted

- src/test/integration/gradle/WorkflowIntegrationTest.java
- src/test/integration/gradle/DevelopmentModeTest.java
- src/test/integration/gradle/BuildProcessTest.java
- src/test/integration/gradle/ErrorHandlingTest.java
- build.gradle (root - add integration test configuration)

## Acceptance Criteria

Given the Gradle integration tests are implemented
When ./gradlew integrationTest is executed
Then all workflow scenarios should be validated

Given development mode is tested
When the development mode test runs
Then both backend and frontend should start successfully

Given build processes are tested
When build tests run
Then all expected artifacts should be produced

Given error handling is tested
When failure scenarios are simulated
Then appropriate error information should be provided

## Testing Requirements

- Test complete workflow from setup to release
- Test development mode startup and shutdown
- Test build artifact generation
- Test error handling and reporting
- Test cross-module dependencies

## Dependencies / Preconditions

- All Gradle configuration tasks must be completed
- Test framework dependencies must be available
- Development environment must be properly set up
- All application modules must be in working state