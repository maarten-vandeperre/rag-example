# Implement Document Content Extractor

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Implement the DocumentContentExtractor interface to extract text content from PDF, Markdown, and plain text files for document processing.

## Scope

- Implement DocumentContentExtractor interface
- Add PDF text extraction using PDF library
- Add Markdown text extraction (strip formatting)
- Add plain text file handling
- Handle extraction errors and empty content

## Out of Scope

- OCR for scanned PDFs
- Complex document formatting preservation
- Image extraction from documents
- Document metadata extraction

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0005-upload_documents_and_chat_with_private_knowledge_base-create_process_document_usecase.md

## Implementation Details

Implement DocumentContentExtractorImpl with:
- extractText(byte[] fileContent, FileType fileType) method
- PDF text extraction using Apache PDFBox or similar
- Markdown text extraction (remove markdown syntax)
- Plain text handling (UTF-8 encoding)
- Error handling for corrupted files

PDF extraction:
- Use PDFBox library for PDF text extraction
- Handle password-protected PDFs (return error)
- Extract text from all pages
- Handle empty or image-only PDFs

Markdown extraction:
- Remove markdown syntax (headers, links, formatting)
- Preserve paragraph structure
- Handle code blocks appropriately
- Convert to plain text

Plain text extraction:
- Handle different encodings (UTF-8, UTF-16, etc.)
- Detect and handle BOM (Byte Order Mark)
- Validate text content is readable

Content validation:
- Check if extracted text is not empty
- Minimum content length threshold (e.g., 10 characters)
- Filter out documents with only whitespace
- Handle special characters and encoding issues

Error handling:
- Corrupted file format
- Unsupported PDF features
- Encoding issues
- Empty or unreadable content

## Files / Modules Impacted

- backend/infrastructure/document/DocumentContentExtractorImpl.java
- backend/infrastructure/document/PdfTextExtractor.java
- backend/infrastructure/document/MarkdownTextExtractor.java
- backend/infrastructure/document/PlainTextExtractor.java

## Acceptance Criteria

Given a valid PDF file
When extractText() is called
Then readable text content should be returned

Given a Markdown file with formatting
When extractText() is called
Then plain text without markdown syntax should be returned

Given a plain text file
When extractText() is called
Then the file content should be returned as-is

Given a corrupted or empty file
When extractText() is called
Then an appropriate error should be thrown

## Testing Requirements

- Unit tests for DocumentContentExtractorImpl
- Unit tests for each file type extractor
- Tests with sample PDF, Markdown, and text files
- Tests for error handling with corrupted files
- Tests for empty content detection

## Dependencies / Preconditions

- DocumentContentExtractor interface must be defined
- PDF processing library (PDFBox) must be available
- File type enumeration must exist