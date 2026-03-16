# Create simple document processing endpoint for stuck uploads

## Related User Story

Application Startup Fix

## Objective

Create a simple, working endpoint to handle stuck document uploads without introducing complex dependencies or imports that cause compilation errors. Provide a basic solution for the document processing issue while keeping the codebase stable.

## Scope

- Create a simple REST endpoint to list and manage stuck documents
- Use only existing, working dependencies and imports
- Provide basic functionality to mark stuck documents as failed
- Ensure the endpoint compiles and works with current architecture

## Out of Scope

- Complex event processing system implementation
- Advanced document reprocessing capabilities
- Modifying existing working components
- Implementing new domain classes

## Clean Architecture Placement

- interface adapters (REST API)

## Execution Dependencies

- 0082-application_startup_fix-fix_test_compilation_errors_and_constructor_issues.md

## Implementation Details

### Create Simple Admin Endpoint
- Create a basic REST resource using only existing, working imports
- Use simple JDBC queries to find and update stuck documents
- Avoid complex domain objects or unresolvable dependencies
- Keep implementation minimal and focused

### Implement Basic Operations
- List documents with UPLOADED status (stuck documents)
- Mark stuck documents as FAILED with explanatory message
- Provide simple JSON responses for frontend integration
- Use direct database operations to avoid dependency issues

### Use Working Dependencies Only
- Use only javax.sql.DataSource for database operations
- Use standard JAX-RS annotations for REST endpoints
- Avoid importing classes that cause compilation errors
- Keep all imports simple and resolvable

### Provide Clear Error Messages
- Return helpful error messages for stuck documents
- Explain that documents need to be re-uploaded
- Provide guidance on preventing the issue
- Include document metadata in responses

## Files / Modules Impacted

- `backend/src/main/java/com/rag/app/api/StuckDocumentResource.java` (CREATE)
- No modifications to existing working files

## Acceptance Criteria

**Given** the simple endpoint is created
**When** compiling the backend
**Then** there should be no compilation errors from the new endpoint

**Given** documents are stuck in UPLOADED status
**When** calling the stuck documents endpoint
**Then** it should return a list of stuck documents

**Given** a stuck document exists
**When** calling the cleanup endpoint
**Then** the document should be marked as FAILED with explanation

**Given** the endpoint is working
**When** the frontend queries stuck documents
**Then** it should receive proper JSON responses

## Testing Requirements

- Test endpoint compilation
- Test basic endpoint functionality
- Verify database operations work correctly
- Test JSON response format
- Ensure no impact on existing functionality

## Dependencies / Preconditions

- Backend compiles successfully
- Database connection is working
- JAX-RS framework is available
- DataSource is properly configured