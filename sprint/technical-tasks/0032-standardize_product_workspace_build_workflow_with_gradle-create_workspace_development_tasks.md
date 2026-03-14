# Create Workspace Development Tasks

## Related User Story

User Story: standardize_product_workspace_build_workflow_with_gradle

## Objective

Create workspace-wide Gradle tasks that orchestrate development mode for both backend and frontend simultaneously, providing a unified developer experience.

## Scope

- Create composite tasks for full-stack development
- Configure parallel execution of backend and frontend development servers
- Add workspace-wide test execution tasks
- Create unified build and verification tasks
- Add proper task dependencies and ordering

## Out of Scope

- Production deployment tasks
- CI/CD pipeline integration
- Performance monitoring tasks
- Advanced parallel execution optimization

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0030-standardize_product_workspace_build_workflow_with_gradle-create_backend_gradle_build_configuration.md
- 0031-standardize_product_workspace_build_workflow_with_gradle-create_frontend_gradle_build_configuration.md

## Implementation Details

Add to root build.gradle:
```gradle
// Workspace-wide development task
task dev {
    group = 'application'
    description = 'Starts both backend and frontend in development mode'
    dependsOn ':backend:dev', ':frontend:dev'
    
    doFirst {
        println "Starting full-stack development environment..."
        println "Backend will be available at: http://localhost:8080"
        println "Frontend will be available at: http://localhost:3000"
    }
}

// Workspace-wide test task
task testAll {
    group = 'verification'
    description = 'Runs all tests across backend and frontend'
    dependsOn ':backend:test', ':frontend:test'
    
    doLast {
        println "All tests completed. Check individual module reports for details."
    }
}

// Workspace-wide build task
task buildAll {
    group = 'build'
    description = 'Builds both backend and frontend'
    dependsOn ':backend:build', ':frontend:build'
    
    doLast {
        println "Full workspace build completed successfully."
    }
}

// Workspace-wide verification task
task verifyAll {
    group = 'verification'
    description = 'Runs all verification tasks across the workspace'
    dependsOn ':backend:verify', ':frontend:verify'
}

// Clean all modules
task cleanAll {
    group = 'build'
    description = 'Cleans all modules in the workspace'
    dependsOn ':backend:clean', ':frontend:clean'
}

// Quick development setup task
task setup {
    group = 'setup'
    description = 'Sets up the development environment'
    dependsOn ':frontend:npmInstall'
    
    doLast {
        println "Development environment setup complete."
        println "Run './gradlew dev' to start development mode."
    }
}

// Release preparation task
task prepareRelease {
    group = 'release'
    description = 'Prepares release-ready outputs'
    dependsOn 'verifyAll', 'buildAll'
    
    doLast {
        println "Release preparation completed."
        println "Backend JAR: backend/build/quarkus-app/"
        println "Frontend build: frontend/build/"
    }
}
```

Task execution configuration:
- Parallel execution where possible
- Proper dependency ordering
- Clear progress reporting
- Error handling and reporting

Development workflow tasks:
- `./gradlew setup` - Initial environment setup
- `./gradlew dev` - Start full development environment
- `./gradlew testAll` - Run all tests
- `./gradlew buildAll` - Build everything
- `./gradlew verifyAll` - Run all verification
- `./gradlew prepareRelease` - Prepare release outputs

Error handling:
- Clear failure messages
- Module-specific error reporting
- Escalation guidance for failures
- Dependency validation

## Files / Modules Impacted

- build.gradle (root) - extend existing
- gradle.properties (add task configuration)

## Acceptance Criteria

Given the workspace development tasks are configured
When ./gradlew dev is executed
Then both backend and frontend should start in development mode

Given the test tasks are configured
When ./gradlew testAll is executed
Then all tests across both modules should run

Given the build tasks are configured
When ./gradlew buildAll is executed
Then both modules should build successfully

Given the release task is configured
When ./gradlew prepareRelease is executed
Then release-ready outputs should be produced

## Testing Requirements

- Test parallel task execution
- Test task dependency resolution
- Test error handling and reporting
- Test workspace-wide operations
- Test release preparation workflow

## Dependencies / Preconditions

- Backend and frontend Gradle configurations must exist
- All module-specific tasks must be properly defined
- Task dependencies must be correctly configured