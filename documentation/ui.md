# UI Walkthrough

## Current entry points

The root app in `frontend/src/App.js` currently renders:

- a routed shell via `AppRoutes`
- `/admin` for the admin panel when `REACT_APP_USER_ROLE=ADMIN`
- `/documents` for the document library for standard users
- `/chat` for the chat workspace
- `/profile` for user profile flows

Admin-only knowledge graph routes are also available:

- `/admin/knowledge-graph`
- `/admin/knowledge-graph/search`
- `/admin/knowledge-graph/:graphId`

## Document library

Where it appears:

- default main screen for non-admin users

User flow:

1. Open the document library
2. Select or drag a file into the upload area
3. Review local validation feedback
4. Start the upload
5. Watch progress and updated document status

Field validation:

- supported extensions: `.pdf`, `.md`, `.txt`
- maximum file size: `40 MB`
- upload requires a user id in the API client context

Expected results:

- the new file is added to the list immediately after upload
- documents are sorted by newest upload first
- status badges show `UPLOADED`, `PROCESSING`, `READY`, or `FAILED`
- the header cards update total, ready, and processing counts

Example session:

1. Upload `knowledge-base.md`
2. Wait for the list row to move from `UPLOADED` to `READY`
3. Confirm the ready counter increases

## Chat workspace

Where it appears:

- available as `frontend/src/components/ChatWorkspace/ChatWorkspace.jsx`
- not mounted as the default root page today

User flow:

1. Open a screen that renders `ChatWorkspace`
2. Enter a question
3. Submit the question
4. Wait for the temporary `Working on your answer...` assistant state
5. Review the final answer and source references
6. Click a completed assistant answer to open the answer detail side view
7. Close the detail view to return to the normal chat layout

Field validation:

- the question cannot be blank
- the default frontend timeout is `20000` ms

Expected results:

- an intro assistant message is always shown first
- a pending assistant message appears while the request is in flight
- successful answers include document references when available
- no-answer, timeout, and request failures are shown as user-facing error messages
- completed assistant answers act as keyboard-accessible interactive targets
- opening a detail view highlights the selected answer and initializes its first source state

Example session:

1. Ask `What does the retention policy say?`
2. Wait for the assistant answer to appear
3. Click the answer bubble
4. Confirm the side view opens with that answer selected
5. Close the side view and continue the conversation

## Answer detail side view

Where it appears:

- implemented in `frontend/src/components/chat/AnswerDetailSideView.jsx`
- rendered from `ChatWorkspace` when a user selects an assistant answer
- reusable from other chat-style screens that want an answer-inspection drawer

User flow:

1. Open a screen that renders the side view with `isOpen=true`
2. Select an assistant answer from chat
3. Pass the selected answer plus one or more source snippets
4. Review the answer content in the top section
5. Review the selected source snippet and metadata in the source section
6. Close the panel with the close button, overlay click, or keyboard escape flow

Field validation and states:

- the component accepts `loading` and `error` states for deferred data loading
- it can render with a single selected source or a list of sources for switching
- keyboard users can reach the close action and source actions through normal tab order
- when multiple sources are present, the first available source is selected automatically

Expected results:

- the panel opens as an accessible overlay instead of replacing the chat layout
- answer content stays readable and visually separated from source context
- the source area can switch between available sources without leaving the drawer
- loading and error states keep the rest of the chat UI usable

### Source switching inside answer details

Where it appears:

- inside `AnswerDetailSideView`
- implemented with `SourceSelector` and `SourceSnippetDisplay`

User flow:

1. Open an answer that has multiple sources
2. Wait for source details to load from `/api/chat/answers/{answerId}/sources`
3. Review the auto-selected first available source
4. Click another source tab or selector option
5. Review the updated snippet, metadata, and availability state

Field validation and states:

- unavailable sources are visibly marked and cannot be selected
- loading states appear while answer-specific sources are being fetched
- keyboard users can move through selectable sources and activate them without a mouse
- warning components explain missing, partial, and unavailable-source states without crashing the drawer

Expected results:

- switching sources updates the snippet and metadata in place
- unavailable sources remain visible for transparency but do not replace the current valid source
- reopening the same answer triggers the source-loading flow again through the chat API client

### Answer detail warnings and recovery

Where it appears:

- inside the answer detail drawer above the source content
- in source-specific fallback states when a source cannot be loaded

User flow:

1. Open answer details for a response with missing or partial source data
2. Review the warning shown at the top of the drawer
3. If the warning is retryable, click the retry action
4. Continue with any available sources or dismiss the warning

Expected results:

- `No source information available` explains empty source lists clearly
- partial-source warnings keep working sources visible while flagging unavailable ones
- retryable loading failures stay actionable and are logged for frontend debugging

### Full document viewer

Where it appears:

- launched from the source section inside `AnswerDetailSideView`
- rendered as a modal document viewer above the chat workspace

User flow:

1. Open answer details for a chat response
2. Select a source snippet
3. Click `View Full Document`
4. Wait for the viewer to load document content from `/api/documents/{documentId}/content`
5. Review the full document with the source snippet highlighted in context
6. Search inside the document if needed
7. Close the modal to return to answer details

Field validation and states:

- the viewer shows loading, unsupported-type, and retryable error states
- text and markdown documents render inline with snippet highlighting
- PDF documents use page navigation and open on the relevant page when page metadata exists

Expected results:

- the document viewer opens without leaving the chat workflow
- previously loaded documents can be reused through client-side caching
- search results update in the viewer without closing the modal

Example component usage:

1. Render `AnswerDetailSideView` with a selected answer
2. Pass one source snippet from a mocked chat response
3. Confirm the drawer shows the answer and snippet together
4. Close the drawer and return focus to the parent screen

## Admin progress

Where it appears:

- default main screen for admin users

User flow:

1. Open the admin progress screen as an admin user
2. Review aggregate processing statistics
3. Inspect failed documents and active processing work
4. Click `Refresh data` to reload current state

Field validation and access:

- the screen sends `X-User-Id` on fetch requests
- non-admin users see an explicit access denied state instead of the dashboard

Expected results:

- statistics show total, uploaded, processing, ready, and failed counts
- failed documents include failure reasons
- processing documents include upload and processing start timestamps

## Knowledge graph administration

Where it appears:

- available through `/admin/knowledge-graph`
- linked from the shared navigation and admin panel for admin users only

User flow:

1. Sign in or run the app as an admin user
2. Open `Knowledge Graphs` from the navigation
3. Review the graph list summary cards and graph table
4. Filter graphs by name
5. Open a graph detail page
6. Inspect nodes, relationships, and node detail information
7. Open the search page to query across graphs

Field validation and access:

- non-admin users are shown an access-denied screen instead of the knowledge graph pages
- the list page supports a graph-name filter and paginated browsing
- the search page accepts a free-text query plus optional node-type and relationship-type filters
- all requests use the configured API base URL and send `X-User-ID`

Expected results:

- admins can browse graph summaries with total node and relationship counts
- graph detail pages show metadata, tabular nodes, tabular relationships, and inspectable node detail
- graph search shows matching graphs, nodes, and relationships in one results page

Example session:

1. Open `/admin/knowledge-graph`
2. Filter by `main`
3. Click `Open graph`
4. Click `Inspect` on a node row
5. Open `/admin/knowledge-graph/search`
6. Search for `neo4j` and review matching graph results
