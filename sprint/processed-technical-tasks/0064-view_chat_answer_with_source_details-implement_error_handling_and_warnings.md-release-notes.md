## Summary
Added targeted warnings, retryable error states, and lightweight error logging for the answer detail flow so users get clearer feedback when source details are missing, partially unavailable, or fail to load.

## Changes
- `frontend/src/components/chat/AnswerDetailSideView.jsx`
- `frontend/src/components/chat/ErrorWarningDisplay.jsx`
- `frontend/src/components/chat/warnings/NoSourcesWarning.jsx`
- `frontend/src/components/chat/warnings/PartialSourceFailureWarning.jsx`
- `frontend/src/components/chat/warnings/SourceUnavailableWarning.jsx`
- `frontend/src/components/chat/warnings/warnings.module.css`
- `frontend/src/hooks/useAnswerSources.js`
- `frontend/src/hooks/useErrorHandling.js`
- `frontend/src/utils/errorLogger.js`
- `frontend/src/utils/errorTypes.js`
- `frontend/src/components/chat/AnswerDetailSideView.test.jsx`
- `frontend/src/components/chat/ErrorWarningDisplay.test.jsx`
- `frontend/src/hooks/useErrorHandling.test.js`
- `frontend/src/utils/errorLogger.test.js`

## Impact
The answer detail side view now degrades more gracefully when sources are missing or inaccessible, distinguishes retryable source-loading failures, and records structured frontend diagnostics for debugging and monitoring workflows.

## Verification
- `./gradlew :frontend:testComponents :frontend:buildProd`
- `./gradlew :frontend:test :frontend:testComponents :frontend:buildProd`

## Follow-ups
- Reduce remaining async React act warnings around document viewer tests.
- Extend the same warning patterns to document-viewer-specific loading failures if more granular recovery is needed.
