## Summary
Added a full document viewer flow so users can open complete source documents from answer details, search within the content, and keep source snippets highlighted in context.

## Changes
- `frontend/src/components/chat/DocumentViewer.jsx`
- `frontend/src/components/chat/DocumentViewerModal.jsx`
- `frontend/src/components/chat/DocumentContentRenderer.jsx`
- `frontend/src/components/chat/PDFViewer.jsx`
- `frontend/src/components/chat/TextViewer.jsx`
- `frontend/src/components/chat/DocumentSearch.jsx`
- `frontend/src/components/chat/DocumentViewer.module.css`
- `frontend/src/components/chat/DocumentViewerModal.module.css`
- `frontend/src/hooks/useDocumentContent.js`
- `frontend/src/utils/documentCache.js`
- `frontend/src/utils/textHighlight.js`
- `frontend/src/components/chat/AnswerDetailSideView.jsx`
- `frontend/src/components/chat/AnswerDetailSideView.test.jsx`
- `frontend/src/components/ChatWorkspace/ChatWorkspace.jsx`
- `frontend/src/components/ChatWorkspace/ChatWorkspace.test.jsx`

## Impact
Users can now launch a modal document viewer from source snippets, load full backend document content with caching, navigate search matches, and review extracted PDF or text-based documents without leaving the chat workflow.

## Verification
- `./gradlew :frontend:testComponents :frontend:buildProd`
- `./gradlew :frontend:test :frontend:testComponents :frontend:buildProd`

## Follow-ups
- Reduce test-time React act warnings around async document loading.
- Add richer markdown rendering and deeper PDF navigation if the product needs file-specific viewers later.
