## Summary
Implemented document content extraction for PDF, Markdown, and plain text files with validation for unreadable or too-short content.

## Changes
- `backend/pom.xml`
- `backend/src/main/java/com/rag/app/infrastructure/document/DocumentContentExtractorImpl.java`
- `backend/src/main/java/com/rag/app/infrastructure/document/PdfTextExtractor.java`
- `backend/src/main/java/com/rag/app/infrastructure/document/MarkdownTextExtractor.java`
- `backend/src/main/java/com/rag/app/infrastructure/document/PlainTextExtractor.java`
- `backend/src/test/java/com/rag/app/infrastructure/document/DocumentContentExtractorImplTest.java`
- `backend/src/test/java/com/rag/app/infrastructure/document/PdfTextExtractorTest.java`
- `backend/src/test/java/com/rag/app/infrastructure/document/MarkdownTextExtractorTest.java`
- `backend/src/test/java/com/rag/app/infrastructure/document/PlainTextExtractorTest.java`

## Impact
The backend processing pipeline now has a concrete extractor that can normalize supported document types into readable text before vectorization.

## Verification
- `mvn -s maven-settings.xml -U compile`
- `mvn -s maven-settings.xml -U test`

## Follow-ups
- Add password-protected PDF fixtures once encrypted document support requirements are clearer.
- Consider richer charset detection for plain text files beyond BOM-based handling if more sources are expected.
