# UI Walkthrough

## Current entry points

The root app in `frontend/src/App.js` currently renders:

- `AdminProgress` when `REACT_APP_USER_ROLE=ADMIN`
- `DocumentLibrary` for other roles

The `ChatWorkspace` component is implemented and documented here, but it is not the default root screen today.

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

Field validation:

- the question cannot be blank
- the default frontend timeout is `20000` ms

Expected results:

- an intro assistant message is always shown first
- a pending assistant message appears while the request is in flight
- successful answers include document references when available
- no-answer, timeout, and request failures are shown as user-facing error messages

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
