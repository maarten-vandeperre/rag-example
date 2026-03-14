# Configure Gradle Error Handling and Reporting

## Related User Story

User Story: standardize_product_workspace_build_workflow_with_gradle

## Objective

Configure comprehensive error handling and reporting in Gradle builds to provide clear failure information that can be escalated to the lead architect.

## Scope

- Configure detailed error reporting for build failures
- Set up logging and diagnostic information collection
- Create failure analysis and troubleshooting guidance
- Add build health checks and validation
- Configure escalation information for complex failures

## Out of Scope

- Automated error resolution
- Integration with external monitoring systems
- Advanced debugging tools integration
- Performance profiling configuration

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0029-standardize_product_workspace_build_workflow_with_gradle-create_root_gradle_build_configuration.md
- 0032-standardize_product_workspace_build_workflow_with_gradle-create_workspace_development_tasks.md

## Implementation Details

Root build error handling (extend root build.gradle):
```gradle
// Global error handling configuration
gradle.taskGraph.whenReady { taskGraph ->
    taskGraph.allTasks.each { task ->
        task.doFirst {
            logger.info("Starting task: ${task.path}")
        }
        
        task.doLast {
            logger.info("Completed task: ${task.path}")
        }
    }
}

// Build failure handler
gradle.buildFinished { result ->
    if (result.failure) {
        println "\n" + "=".repeat(80)
        println "BUILD FAILED"
        println "=".repeat(80)
        
        def failure = result.failure
        println "Failure: ${failure.message}"
        
        if (failure.cause) {
            println "Cause: ${failure.cause.message}"
        }
        
        println "\nFor escalation to lead architect, include:"
        println "1. This error message"
        println "2. Gradle version: ${gradle.gradleVersion}"
        println "3. Java version: ${System.getProperty('java.version')}"
        println "4. OS: ${System.getProperty('os.name')} ${System.getProperty('os.version')}"
        println "5. Command executed: ${gradle.startParameter.taskNames.join(' ')}"
        println "6. Build scan URL (if available)"
        
        println "\nTroubleshooting steps:"
        println "1. Run with --stacktrace for detailed error information"
        println "2. Run with --info or --debug for verbose logging"
        println "3. Check individual module build logs"
        println "4. Verify all prerequisites are installed"
        println "=".repeat(80)
    } else {
        println "\nBUILD SUCCESSFUL"
        println "All tasks completed successfully."
    }
}

// Health check task
task healthCheck {
    group = 'verification'
    description = 'Performs comprehensive health check of the build environment'
    
    doLast {
        println "=== Build Environment Health Check ==="
        
        // Java version check
        def javaVersion = System.getProperty('java.version')
        println "Java Version: ${javaVersion}"
        if (!javaVersion.startsWith('17')) {
            throw new GradleException("Java 17 is required, but found: ${javaVersion}")
        }
        
        // Gradle version check
        println "Gradle Version: ${gradle.gradleVersion}"
        
        // Memory check
        def runtime = Runtime.getRuntime()
        def maxMemory = runtime.maxMemory() / 1024 / 1024
        println "Max Memory: ${maxMemory} MB"
        if (maxMemory < 1024) {
            logger.warn("Low memory detected. Consider increasing heap size with -Xmx")
        }
        
        // Module structure check
        def expectedModules = ['backend', 'frontend']
        expectedModules.each { module ->
            def moduleDir = file(module)
            if (!moduleDir.exists()) {
                throw new GradleException("Required module directory not found: ${module}")
            }
            println "Module '${module}': OK"
        }
        
        // Dependency resolution check
        try {
            configurations.each { config ->
                if (config.canBeResolved) {
                    config.resolve()
                }
            }
            println "Dependency Resolution: OK"
        } catch (Exception e) {
            throw new GradleException("Dependency resolution failed: ${e.message}")
        }
        
        println "=== Health Check Passed ==="
    }
}

// Diagnostic information task
task diagnostics {
    group = 'help'
    description = 'Collects diagnostic information for troubleshooting'
    
    doLast {
        println "=== Diagnostic Information ==="
        println "Gradle Version: ${gradle.gradleVersion}"
        println "Java Version: ${System.getProperty('java.version')}"
        println "Java Vendor: ${System.getProperty('java.vendor')}"
        println "OS: ${System.getProperty('os.name')} ${System.getProperty('os.version')}"
        println "Architecture: ${System.getProperty('os.arch')}"
        println "User: ${System.getProperty('user.name')}"
        println "Working Directory: ${System.getProperty('user.dir')}"
        println "Gradle User Home: ${gradle.gradleUserHomeDir}"
        
        println "\nEnvironment Variables:"
        ['JAVA_HOME', 'GRADLE_OPTS', 'GRADLE_USER_HOME'].each { envVar ->
            def value = System.getenv(envVar)
            println "${envVar}: ${value ?: 'Not set'}"
        }
        
        println "\nProject Properties:"
        project.properties.findAll { it.key.startsWith('org.gradle') }.each { key, value ->
            println "${key}: ${value}"
        }
        
        println "\nAvailable Tasks:"
        tasks.matching { it.group != null }.each { task ->
            println "${task.group}:${task.name} - ${task.description ?: 'No description'}"
        }
        
        println "=== End Diagnostic Information ==="
    }
}
```

Module-specific error handling:

Backend error handling (extend backend/build.gradle):
```gradle
// Backend-specific error handling
tasks.withType(JavaCompile) {
    options.compilerArgs += ['-Xlint:all', '-Werror']
    
    doFirst {
        logger.info("Compiling Java sources...")
    }
    
    doLast {
        logger.info("Java compilation completed successfully")
    }
}

// Quarkus dev mode error handling
quarkusDev {
    doFirst {
        println "Starting Quarkus development mode..."
        println "If startup fails, check:"
        println "1. Database connection (PostgreSQL running?)"
        println "2. Port 8080 availability"
        println "3. Application configuration"
    }
}

// Test failure reporting
test {
    testLogging {
        events "failed"
        exceptionFormat "full"
        showCauses true
        showExceptions true
        showStackTraces true
    }
    
    afterSuite { desc, result ->
        if (!desc.parent) {
            println "\nTest Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
        }
    }
}
```

Frontend error handling (extend frontend/build.gradle):
```gradle
// Frontend-specific error handling
tasks.withType(NpmTask) {
    doFirst {
        logger.info("Executing npm command: ${npmCommand.join(' ')}")
    }
    
    execOverrides {
        it.ignoreExitValue = false
        it.standardOutput = new ByteArrayOutputStream()
        it.errorOutput = new ByteArrayOutputStream()
        
        it.doLast {
            if (it.exitValue != 0) {
                println "NPM command failed with exit code: ${it.exitValue}"
                println "Standard Output:"
                println it.standardOutput.toString()
                println "Error Output:"
                println it.errorOutput.toString()
                
                throw new GradleException("NPM task failed. See output above for details.")
            }
        }
    }
}

// Node.js environment validation
npmInstall {
    doFirst {
        def nodeVersion = providers.exec {
            commandLine 'node', '--version'
        }.standardOutput.asText.get().trim()
        
        println "Node.js version: ${nodeVersion}"
        
        if (!nodeVersion.startsWith('v18')) {
            logger.warn("Node.js 18 is recommended, but found: ${nodeVersion}")
        }
    }
}
```

## Files / Modules Impacted

- build.gradle (root - add error handling and diagnostics)
- backend/build.gradle (extend with backend-specific error handling)
- frontend/build.gradle (extend with frontend-specific error handling)
- gradle.properties (add error reporting configuration)

## Acceptance Criteria

Given error handling is configured
When a build fails
Then clear error information should be displayed with escalation guidance

Given health check is implemented
When ./gradlew healthCheck is executed
Then the build environment should be validated

Given diagnostics are configured
When ./gradlew diagnostics is executed
Then comprehensive system information should be collected

Given build failures occur
When troubleshooting information is needed
Then sufficient detail should be available for lead architect escalation

## Testing Requirements

- Test error reporting for various failure scenarios
- Test health check validation
- Test diagnostic information collection
- Test escalation guidance display
- Test logging and output formatting

## Dependencies / Preconditions

- Root Gradle configuration must exist
- Module-specific build configurations must exist
- Logging framework must be properly configured
- Error scenarios must be testable