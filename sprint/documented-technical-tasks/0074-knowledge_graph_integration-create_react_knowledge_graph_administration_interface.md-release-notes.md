## Summary
Added a React knowledge graph administration interface with admin-only routes, API client support, graph browsing, detail exploration, and search pages.

## Changes
- `frontend/src/services/KnowledgeGraphApiClient.js`
- `frontend/src/services/KnowledgeGraphApiClient.test.js`
- `frontend/src/services/ApiClient.js`
- `frontend/src/api/knowledge-graph.js`
- `frontend/src/types/knowledge-graph.js`
- `frontend/src/components/knowledge-graph/KnowledgeGraphList.jsx`
- `frontend/src/components/knowledge-graph/KnowledgeGraphDetail.jsx`
- `frontend/src/components/knowledge-graph/KnowledgeGraphSearch.jsx`
- `frontend/src/components/knowledge-graph/KnowledgeGraphAccessDenied.jsx`
- `frontend/src/components/knowledge-graph/KnowledgeGraphList.test.jsx`
- `frontend/src/pages/knowledge-graph/KnowledgeGraphListPage.jsx`
- `frontend/src/pages/knowledge-graph/KnowledgeGraphDetailPage.jsx`
- `frontend/src/pages/knowledge-graph/KnowledgeGraphSearchPage.jsx`
- `frontend/src/routes/AppRoutes.js`
- `frontend/src/routes/AppRoutes.test.js`
- `frontend/src/modules/shared/components/Navigation.jsx`
- `frontend/src/modules/shared/constants/apiEndpoints.js`
- `frontend/src/modules/user-management/components/AdminPanel.jsx`
- `frontend/src/App.test.js`

## Impact
Admin users can now navigate to a dedicated knowledge graph area in the frontend, browse graph summaries, inspect graph details and node connections, run filtered graph searches, and see clear access-denied handling for non-admin users.

## Verification
- `CI=true npm test -- --watch=false`
- `npm run build`
- `CI=true npm test -- --watch=false && npm run build`

## Follow-ups
- Replace the simple table-based graph exploration UI with richer visualizations once graph rendering requirements are finalized.
- Add page-level integration tests with mocked API latency and backend error scenarios.
