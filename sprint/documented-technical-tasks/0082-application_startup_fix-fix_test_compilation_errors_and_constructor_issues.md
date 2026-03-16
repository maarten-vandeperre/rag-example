# Fix test compilation errors and constructor issues

## Related User Story

Application Startup Fix

## Objective

Fix the test compilation errors caused by constructor signature mismatches and missing dependencies in test classes. Ensure all tests can compile and run without preventing the application from starting.

## Scope

- Fix DocumentUploadControllerTest constructor issues
- Fix ChatSystemFacadeTest constructor issues
- Update test classes to match current implementation signatures
- Ensure all test dependencies are properly mocked or provided

## Out of Scope

- Changing production code to match tests
- Implementing new test functionality
- Modifying working production components

## Clean Architecture Placement

- testing (test infrastructure and mocking)

## Execution Dependencies

- 0081-application_startup_fix-fix_module_dependencies_and_build_path_issues.md

## Implementation Details

### Fix DocumentUploadControllerTest
- Update UploadDocument constructor calls in test to match current implementation
- Check current UploadDocument constructor signature in production code
- Update test mocks and dependencies to provide correct parameters
- Ensure InMemoryDocumentRepository and InMemoryUserRepository are properly configured

### Fix ChatSystemFacadeTest
- Update ChatSystemFacadeImpl constructor calls to match current implementation
- Fix QueryDocuments constructor calls with correct parameters
- Update test mocks for WeaviateVectorStore, GenerateAnswer, and other dependencies
- Ensure all test dependencies are properly injected

### Update Test Infrastructure
- Review and update test base classes and utilities
- Ensure test mocks provide all required dependencies
- Fix any missing test dependencies or configuration
- Update test data setup to match current domain model

### Verify Test Compilation
- Compile all test classes individually to identify specific issues
- Fix any remaining constructor or dependency issues
- Ensure tests can run without affecting application startup
- Verify that test failures don't prevent application compilation

## Files / Modules Impacted

- `backend/src/test/java/com/rag/app/api/DocumentUploadControllerTest.java`
- `backend/chat-system/src/test/java/com/rag/app/chat/ChatSystemFacadeTest.java`
- `backend/src/test/java/integration/IntegrationTestSupport.java`
- Any other test classes with constructor or dependency issues

## Acceptance Criteria

**Given** the test constructor issues are fixed
**When** compiling DocumentUploadControllerTest
**Then** the compilation should succeed without constructor errors

**Given** the ChatSystemFacadeTest is updated
**When** compiling the chat-system module tests
**Then** all constructor calls should match the current implementation

**Given** all test compilation errors are fixed
**When** running `./gradlew compileTestJava`
**Then** all test classes should compile successfully

**Given** the test fixes are complete
**When** starting the application
**Then** test compilation errors should not prevent startup

## Testing Requirements

- Compile all test classes successfully
- Run a sample test to verify test infrastructure works
- Ensure tests don't interfere with application startup
- Verify that test mocks are properly configured

## Dependencies / Preconditions

- Module dependencies are properly configured
- Production code compiles successfully
- Test infrastructure and mocking libraries are available