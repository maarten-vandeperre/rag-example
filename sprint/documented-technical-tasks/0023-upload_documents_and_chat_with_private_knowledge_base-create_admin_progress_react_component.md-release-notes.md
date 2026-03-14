## Summary
Added an admin progress React experience that fetches document processing metrics, renders failure and in-flight document tables, and blocks non-admin access.

## Changes
- `frontend/src/components/AdminProgress/AdminProgress.jsx`
- `frontend/src/components/AdminProgress/ProcessingStatistics.jsx`
- `frontend/src/components/AdminProgress/FailedDocumentsList.jsx`
- `frontend/src/components/AdminProgress/ProcessingDocumentsList.jsx`
- `frontend/src/components/AdminProgress/AdminProgress.css`
- `frontend/src/components/AdminProgress/AdminProgress.test.jsx`
- `frontend/src/App.js`
- `frontend/src/App.css`
- `frontend/src/App.test.js`

## Impact
Admin users now get a dedicated frontend progress dashboard backed by the admin progress API, while non-admin users receive an explicit access-denied state instead of the admin view.

## Verification
- `npm test -- --watch=false`
- `npm run build`
- `CI=true npm test -- --watch=false && npm run build`

## Follow-ups
- Connect the admin dashboard to navigation and authenticated runtime user state once frontend routing and auth context are introduced.
