## Summary
Connected chat answers to the answer detail side view so users can open detailed answer/source context directly from the main chat interface.

## Changes
- `frontend/src/components/ChatWorkspace/ChatMessage.jsx`
- `frontend/src/components/ChatWorkspace/ChatWorkspace.jsx`
- `frontend/src/components/ChatWorkspace/ChatWorkspace.css`
- `frontend/src/components/ChatWorkspace/ChatWorkspace.test.jsx`

## Impact
Assistant answers in the chat are now keyboard-accessible interactive targets with selection state, detail drawer integration, source initialization, and close/reset behavior while preserving existing chat flows.

## Verification
- `./gradlew :frontend:testComponents :frontend:buildProd`
- `./gradlew :frontend:test :frontend:testComponents :frontend:buildProd`

## Follow-ups
- Replace the placeholder full-document action with routed document viewing.
- Consider adding source switching telemetry and selected-answer deep linking.
