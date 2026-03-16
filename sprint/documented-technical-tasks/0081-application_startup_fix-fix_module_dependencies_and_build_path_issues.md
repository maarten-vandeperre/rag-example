# Fix module dependencies and build path issues

## Related User Story

Application Startup Fix

## Objective

Fix the module dependency issues and build path problems that are preventing the application from compiling and starting. Ensure all modules can find their required dependencies and that the build system properly resolves all classes.

## Scope

- Fix missing GraphId class in shared-kernel module
- Resolve document-management module build path issues
- Ensure proper module dependency resolution
- Fix any circular dependency issues between modules

## Out of Scope

- Changing application architecture
- Implementing new functionality
- Modifying working components

## Clean Architecture Placement

- infrastructure (build system and module configuration)
- domain (missing domain classes)

## Execution Dependencies

- 0080-application_startup_fix-remove_invalid_configuration_files_and_imports.md

## Implementation Details

### Fix Missing GraphId Class
- Check if `com.rag.app.shared.domain.knowledge.valueobjects.GraphId` exists in shared-kernel
- If missing, create the GraphId value object class
- Ensure it follows the same pattern as other value objects (DocumentId, UserId, etc.)
- Add proper validation and immutability

### Fix Module Build Path Issues
- Review document-management module dependencies in build.gradle
- Ensure shared-kernel module is properly included as a dependency
- Check that all required modules are listed in settings.gradle
- Verify module compilation order

### Resolve Dependency Issues
- Check main backend build.gradle for all required module dependencies
- Ensure proper dependency versions are specified
- Fix any missing or incorrect module references
- Verify that all modules can access their required dependencies

### Test Module Compilation
- Compile each module individually to identify specific issues
- Fix any module-specific compilation errors
- Ensure all modules build successfully before building main backend

## Files / Modules Impacted

- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/GraphId.java` (CREATE if missing)
- `backend/document-management/build.gradle` (dependency fixes)
- `backend/build.gradle` (module dependency fixes)
- `settings.gradle` (module inclusion verification)
- Any other modules with dependency issues

## Acceptance Criteria

**Given** the GraphId class is properly implemented
**When** compiling the shared-kernel module
**Then** the compilation should succeed without missing class errors

**Given** module dependencies are properly configured
**When** compiling the document-management module
**Then** it should find all required classes from shared-kernel

**Given** all module dependencies are fixed
**When** running `./gradlew build`
**Then** all modules should compile successfully

**Given** the build path issues are resolved
**When** starting the application
**Then** there should be no ClassNotFoundException errors

## Testing Requirements

- Compile each module individually
- Run full project build
- Verify no missing class errors
- Test that all CDI beans can be instantiated
- Ensure application starts without dependency errors

## Dependencies / Preconditions

- Invalid configuration files have been removed
- Gradle build system is functional
- All required modules are present in the project