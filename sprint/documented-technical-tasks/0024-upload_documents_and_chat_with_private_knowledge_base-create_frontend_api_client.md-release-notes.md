## Summary
Added a reusable frontend API client layer for document uploads, document retrieval, admin progress, and chat queries, then wired the existing React views to use it.

## Changes
`frontend/src/services/ApiClient.js`
`frontend/src/services/ChatApiClient.js`
`frontend/src/services/DocumentApiClient.js`
`frontend/src/services/ErrorHandler.js`
`frontend/src/utils/HttpClient.js`
`frontend/src/components/DocumentLibrary/DocumentLibrary.jsx`
`frontend/src/components/ChatWorkspace/ChatWorkspace.jsx`
`frontend/src/App.test.js`
`frontend/src/components/ChatWorkspace/ChatWorkspace.test.jsx`
`frontend/src/services/ApiClient.test.js`
`frontend/src/services/ChatApiClient.test.js`
`frontend/src/services/DocumentApiClient.test.js`
`frontend/src/services/ErrorHandler.test.js`

## Impact
Frontend data access now flows through shared clients with consistent headers, timeout handling, upload progress support, cancellation hooks, and user-friendly error mapping.

## Verification
`CI=true npm test -- --runInBand`
`npm run build`

## Follow-ups
Replace the temporary user-id provider with real authentication state once frontend auth token management is implemented.
