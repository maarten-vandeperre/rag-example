# Upload documents and chat with private knowledge base

## User Story

As a user who wants to upload information and ask questions about it  
I want to upload documents and chat with their contents  
So that I can quickly retrieve answers from my own knowledge base.

## Context

Users need a simple way to add their own documents to the application and then ask questions based on those documents. For the first release, the scope focuses on document upload, import visibility, role-based access, and conversational querying. Standard users must only be able to view and query their own documents, while admin users must be able to view and query all uploaded documents. Answers must include references to the source document and the relevant paragraph or section so users can verify the response.

## Screens / User Journey

Screen: Document Library  
User action: The user opens the document library and uploads one or more supported files.  
Expected result: The uploaded files appear in the document list with a visible import status.

Screen: Document Library  
User action: The user reviews the list of uploaded documents and their current status.  
Expected result: Each document shows whether it is uploaded, processing, ready, or failed.

Screen: Chat Workspace  
User action: The user asks a question about the uploaded documents.  
Expected result: The application returns an answer grounded in the available documents, including references to the document name and relevant paragraph or section.

Screen: Admin Progress Screen  
User action: An admin opens the progress view to monitor document imports, including failures.  
Expected result: The admin can see the processing state of all documents and identify files that could not be processed or contained no usable content.

Screen: Chat Workspace  
User action: A standard user starts a chat session.  
Expected result: The user can only query and receive answers from documents they uploaded.

Screen: Chat Workspace  
User action: An admin user starts a chat session.  
Expected result: The admin can query and receive answers from all uploaded documents.

## Functional Requirements

- Users can access a document library screen and upload documents.
- The system supports PDF, Markdown, and plain text files.
- The system rejects files larger than 40 MB.
- Uploaded documents appear in the application with a visible import status.
- Import statuses include at least uploaded, processing, ready, and failed.
- Users can open a chat workspace and ask questions about available documents.
- The system provides an answer based on the uploaded documents.
- Each answer includes a reference to the source document and the relevant paragraph or section.
- Standard users can only view and query their own uploaded documents.
- Admin users can view and query all uploaded documents.
- Files that cannot be processed or contain no usable content are visible in the admin progress screen.
- The application returns a response to a chat question within 20 seconds under normal operating conditions.

## Acceptance Criteria

Given a user uploads a supported file under 40 MB  
When the upload is accepted  
Then the document should appear in the document library with an import status.

Given a user uploads a file larger than 40 MB  
When the upload is submitted  
Then the system should reject the file and inform the user that the maximum file size has been exceeded.

Given a user uploads a PDF, Markdown, or plain text file  
When the system processes the file successfully  
Then the document status should change to ready.

Given a file cannot be processed or contains no usable content  
When processing completes  
Then the document status should show as failed and the failed item should be visible in the admin progress screen.

Given a standard user has uploaded one or more ready documents  
When the user asks a question in the chat workspace  
Then the system should return an answer using only that user's documents.

Given an admin user accesses the chat workspace  
When the admin asks a question  
Then the system should be able to return an answer using any uploaded document.

Given the system returns an answer in chat  
When the answer is displayed  
Then it should include the source document name and the relevant paragraph or section reference.

Given a user asks a question in the chat workspace  
When the system can answer from available documents  
Then the response should be returned within 20 seconds under normal operating conditions.

## Functional Test Scenarios

### Test: Upload a supported document successfully

Steps:

1. Open the document library.
2. Upload a PDF, Markdown, or plain text file smaller than 40 MB.
3. Observe the import status.

Expected result:

- The file appears in the document library.
- The status is visible.
- The status eventually changes to ready if processing succeeds.

### Test: Reject an oversized file

Steps:

1. Open the document library.
2. Upload a file larger than 40 MB.

Expected result:

- The upload is rejected.
- The user is informed that the file exceeds the maximum allowed size.

### Test: Chat with own documents as a standard user

Steps:

1. Sign in as a standard user.
2. Upload one or more supported documents.
3. Wait until the documents are ready.
4. Open the chat workspace.
5. Ask a question related to the uploaded content.

Expected result:

- The system returns an answer based on the user's documents.
- The answer includes a source document reference and paragraph or section reference.
- No content from other users' documents is included.

### Test: Chat across all documents as an admin user

Steps:

1. Sign in as an admin user.
2. Open the chat workspace.
3. Ask a question related to documents uploaded by different users.

Expected result:

- The system returns an answer based on all available documents.
- The answer includes source references.

### Test: Show failed imports in admin progress view

Steps:

1. Upload a file that cannot be processed or contains no usable content.
2. Sign in as an admin user.
3. Open the admin progress screen.

Expected result:

- The failed document is visible in the progress view.
- Its status is clearly shown as failed.

### Test: Meet chat response expectation

Steps:

1. Open the chat workspace with ready documents available.
2. Ask a question that can be answered from the uploaded content.
3. Measure the time until the answer is shown.

Expected result:

- The answer is returned within 20 seconds under normal operating conditions.
- The answer includes source references.

## Edge Cases

- A user uploads an unsupported file type.
- A user uploads a file that is exactly 40 MB.
- A file uploads successfully but contains no readable or usable content.
- A document is still processing when the user asks a question.
- A standard user has no ready documents and opens the chat workspace.
- Multiple uploaded documents contain conflicting information.
- A question cannot be answered from the available documents.
- A user attempts to access documents that belong to another standard user.
