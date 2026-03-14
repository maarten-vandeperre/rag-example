# Create Admin Progress React component

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create React component for the admin progress screen that displays document processing statistics and failed document information for admin users.

## Scope

- Create AdminProgress React component with admin-only access
- Display processing statistics with visual indicators
- Show failed documents list with failure reasons
- Add currently processing documents view
- Implement role-based component rendering

## Out of Scope

- Document retry mechanisms
- Real-time progress updates
- Document management actions
- Advanced filtering or search

## Clean Architecture Placement

frontend UI

## Execution Dependencies

- 0013-upload_documents_and_chat_with_private_knowledge_base-create_document_library_rest_controller.md

## Implementation Details

Create AdminProgress component with:
- Role-based access control (admin only)
- Processing statistics dashboard
- Failed documents table
- Currently processing documents table
- Refresh functionality

Create ProcessingStatistics component with:
- Total documents count
- Status breakdown (uploaded, processing, ready, failed)
- Visual progress indicators (progress bars or charts)
- Percentage calculations

Create FailedDocumentsList component with:
- Table with columns: filename, uploaded by, upload date, failure reason
- Sorting by failure date (newest first)
- Expandable failure reason details
- File size information

Create ProcessingDocumentsList component with:
- Table with columns: filename, uploaded by, processing started, duration
- Real-time duration calculation
- Processing time indicators

Access control:
- Check user role before rendering component
- Redirect non-admin users to appropriate page
- Show "Access Denied" message for unauthorized access

Statistics display:
- Total documents: large number display
- Status counts: cards or badges with counts
- Progress bars showing percentage of each status
- Color coding: green (ready), yellow (processing), red (failed), blue (uploaded)

## Files / Modules Impacted

- frontend/src/components/AdminProgress/AdminProgress.jsx
- frontend/src/components/AdminProgress/ProcessingStatistics.jsx
- frontend/src/components/AdminProgress/FailedDocumentsList.jsx
- frontend/src/components/AdminProgress/ProcessingDocumentsList.jsx
- frontend/src/components/AdminProgress/AdminProgress.css

## Acceptance Criteria

Given an admin user opens the admin progress screen
When the component loads
Then processing statistics and failed documents should be displayed

Given a non-admin user tries to access admin progress
When the component loads
Then access should be denied with appropriate message

Given there are failed documents
When the admin progress screen loads
Then failed documents should be listed with failure reasons

Given there are documents currently processing
When the admin progress screen loads
Then processing documents should be shown with timing information

## Testing Requirements

- Unit tests for AdminProgress component
- Unit tests for role-based access control
- Unit tests for statistics display
- Unit tests for failed documents list
- Tests for processing time calculations

## Dependencies / Preconditions

- React application must be set up
- Admin progress REST endpoint must be available
- User role information must be available in frontend
- Frontend API client must be implemented