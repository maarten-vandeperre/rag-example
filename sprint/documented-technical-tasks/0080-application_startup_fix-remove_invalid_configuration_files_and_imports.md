# Remove invalid configuration files and imports

## Related User Story

Application Startup Fix

## Objective

Remove the invalid configuration files and imports that are causing compilation errors and preventing the application from starting. Clean up the codebase to ensure all imports are valid and all dependencies are properly resolved.

## Scope

- Remove DocumentProcessingConfiguration.java with invalid imports
- Remove DocumentProcessingResource.java with invalid imports
- Clean up any other files with unresolvable imports
- Ensure all remaining code compiles successfully

## Out of Scope

- Implementing new functionality
- Modifying working components
- Changing application architecture

## Clean Architecture Placement

- infrastructure (configuration cleanup)

## Execution Dependencies

None

## Implementation Details

### Remove Invalid Configuration Files
- Delete `/Users/maartenvandeperre/workspace/rag-example/backend/src/main/java/com/rag/app/config/DocumentProcessingConfiguration.java`
- Delete `/Users/maartenvandeperre/workspace/rag-example/backend/src/main/java/com/rag/app/api/DocumentProcessingResource.java`
- These files contain imports that cannot be resolved and are preventing compilation

### Verify Compilation
- Run `./gradlew :backend:compileJava` to ensure no compilation errors
- Check that all imports in remaining files are valid
- Verify that CDI beans are properly configured

### Clean Up Import Statements
- Review all Java files for unresolvable imports
- Remove or fix any imports that reference non-existent classes
- Ensure all dependencies are properly declared in build.gradle

## Files / Modules Impacted

- `backend/src/main/java/com/rag/app/config/DocumentProcessingConfiguration.java` (DELETE)
- `backend/src/main/java/com/rag/app/api/DocumentProcessingResource.java` (DELETE)
- Any other files with invalid imports

## Acceptance Criteria

**Given** the invalid configuration files are removed
**When** running `./gradlew :backend:compileJava`
**Then** the compilation should succeed without errors

**Given** all invalid imports are cleaned up
**When** the backend starts
**Then** there should be no ClassNotFoundException or import errors

**Given** the cleanup is complete
**When** checking the codebase
**Then** all remaining imports should be valid and resolvable

## Testing Requirements

- Compile backend successfully
- Start backend without import errors
- Verify existing functionality still works
- Test that CDI beans are properly injected

## Dependencies / Preconditions

- Backend build system is functional
- Gradle dependencies are properly configured