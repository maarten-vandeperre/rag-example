# Create Document Library React component

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create React component for the document library screen that displays uploaded documents with their status and provides file upload functionality.

## Scope

- Create DocumentLibrary React component
- Implement file upload with drag-and-drop support
- Display document list with status indicators
- Add file size and type validation on frontend
- Show upload progress and error messages

## Out of Scope

- Document content preview
- Document deletion functionality
- Advanced file management features
- Real-time status updates

## Clean Architecture Placement

frontend UI

## Execution Dependencies

- 0012-upload_documents_and_chat_with_private_knowledge_base-create_document_upload_rest_controller.md
- 0013-upload_documents_and_chat_with_private_knowledge_base-create_document_library_rest_controller.md

## Implementation Details

Create DocumentLibrary component with:
- File upload area with drag-and-drop support
- Document list table with columns: name, size, type, status, upload date
- Status indicators with different colors/icons for each status
- Error message display for upload failures
- Loading states for uploads and document fetching

Create FileUpload component with:
- Drag-and-drop zone
- File selection button
- File validation (size <= 40MB, type in [PDF, MD, TXT])
- Upload progress indicator
- Error message display

Create DocumentList component with:
- Table or list view of documents
- Status badges (UPLOADED, PROCESSING, READY, FAILED)
- Sorting by upload date (newest first)
- File size formatting (bytes to MB/KB)

Status indicators:
- UPLOADED: blue badge with "Uploaded" text
- PROCESSING: yellow badge with "Processing" text and spinner
- READY: green badge with "Ready" text and checkmark
- FAILED: red badge with "Failed" text and error icon

File validation rules:
- Maximum size: 40MB (41,943,040 bytes)
- Supported types: PDF (.pdf), Markdown (.md), Plain text (.txt)
- Display validation errors immediately on file selection

## Files / Modules Impacted

- frontend/src/components/DocumentLibrary/DocumentLibrary.jsx
- frontend/src/components/DocumentLibrary/FileUpload.jsx
- frontend/src/components/DocumentLibrary/DocumentList.jsx
- frontend/src/components/DocumentLibrary/StatusBadge.jsx
- frontend/src/components/DocumentLibrary/DocumentLibrary.css

## Acceptance Criteria

Given a user opens the document library
When the component loads
Then the user's documents should be displayed with current status

Given a user drags a valid file to the upload area
When the file is dropped
Then the upload should start and progress should be shown

Given a user selects a file larger than 40MB
When the file is selected
Then an error message should be displayed without uploading

Given a user uploads a supported file successfully
When the upload completes
Then the document should appear in the list with UPLOADED status

## Testing Requirements

- Unit tests for DocumentLibrary component
- Unit tests for FileUpload component
- Unit tests for file validation logic
- Integration tests for upload functionality
- Tests for error handling and display

## Dependencies / Preconditions

- React application must be set up
- Document upload and library REST endpoints must be available
- Frontend API client must be implemented