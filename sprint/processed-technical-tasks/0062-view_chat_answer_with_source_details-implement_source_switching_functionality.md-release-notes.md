## Summary
Added source switching inside the answer detail side view so users can load detailed sources, move between them, and see updated snippets with clear loading, error, and unavailable states.

## Changes
- `frontend/src/hooks/useAnswerSources.js`
- `frontend/src/components/chat/SourceSelector.jsx`
- `frontend/src/components/chat/SourceSelector.module.css`
- `frontend/src/components/chat/SourceSnippetDisplay.jsx`
- `frontend/src/components/chat/SourceSnippetDisplay.module.css`
- `frontend/src/components/chat/AnswerDetailSideView.jsx`
- `frontend/src/components/chat/index.js`
- `frontend/src/components/ChatWorkspace/ChatWorkspace.jsx`
- `frontend/src/services/ChatApiClient.js`
- `frontend/src/services/ApiClient.js`
- `frontend/src/components/chat/SourceSelector.test.jsx`
- `frontend/src/components/chat/SourceSnippetDisplay.test.jsx`
- `frontend/src/components/chat/AnswerDetailSideView.test.jsx`
- `frontend/src/components/ChatWorkspace/ChatWorkspace.test.jsx`
- `frontend/src/services/ChatApiClient.test.js`

## Impact
The frontend can now fetch answer-specific source details from the backend, auto-select an available source, switch between sources with keyboard support, and gracefully handle unavailable or failed source loads.

## Verification
- `./gradlew :frontend:testComponents :frontend:buildProd`
- `./gradlew :frontend:test :frontend:testComponents :frontend:buildProd`

## Follow-ups
- Wire full-document viewing to a richer document reader experience.
- Add caching across reopened answers to avoid repeated source requests.
