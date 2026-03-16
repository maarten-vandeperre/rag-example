## Summary
Added a reusable answer detail side panel for chat responses so users can inspect generated answers, source snippets, and supporting document metadata in an accessible overlay.

## Changes
- `frontend/src/components/chat/AnswerDetailSideView.jsx`
- `frontend/src/components/chat/AnswerDetailSideView.module.css`
- `frontend/src/components/chat/AnswerDetailSideView.test.jsx`
- `frontend/src/components/chat/index.js`

## Impact
Introduces the frontend building block needed for answer-detail drill-down interactions, including loading, error, keyboard, overlay-close, and source-selection behaviors without requiring backend integration.

## Verification
- `./gradlew :frontend:testComponents :frontend:buildProd`
- `./gradlew :frontend:test :frontend:testComponents :frontend:buildProd`

## Follow-ups
- Wire the side view into chat answer click interactions.
- Connect source selection and full-document viewing to live data flows.
