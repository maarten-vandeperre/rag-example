# View chat answer with source details

## User Story

As an end user  
I want to open a detailed side view from a chat answer  
So that I can review the answer together with its supporting source snippets and verify the information more easily.

## Context

Users need a simple way to validate and understand the information provided in chat answers. Today, the chat experience should be extended without regression so that, from the main chat screen, a user can open a detailed side view for a selected answer. This detail view must show the selected answer and the related supporting source snippets, while also allowing the user to access the full source documents if needed. The goal is to improve trust and make information lookup faster and easier.

## Screens / User Journey

Screen: Main chat screen

User action:  
The user clicks a chat answer.

Expected result:  
A side view opens with the details of the selected answer.

Screen: Answer detail side view

User action:  
The user reviews the selected answer and its available source information.

Expected result:  
The side view shows the answer content and a relevant snippet from a selected source.

Screen: Source selection in answer detail side view

User action:  
The user switches between available sources for the selected answer.

Expected result:  
The displayed source snippet updates to match the selected source.

Screen: Full source access from answer detail side view

User action:  
The user chooses to open the full source document.

Expected result:  
The user can access the full source document from the detail view.

Screen: Missing source handling in main chat screen

User action:  
The user interacts with an answer that has no available source information or whose source cannot be loaded.

Expected result:  
The chat shows a warning to inform the user.

## Functional Requirements

- End users must be able to click an answer from the main chat screen.
- Clicking an answer must open a detailed side view without breaking the existing chat experience.
- The detail side view must show the selected answer.
- The detail side view must show the relevant snippet for a source linked to the selected answer.
- If an answer has multiple sources, the user must be able to switch between them from the detail view.
- When multiple sources are available, one source snippet must be shown by default.
- The user must be able to access the full document for a selected source from the detail view.
- If an answer has no sources or a source cannot be loaded, the user must see a warning in the chat.
- The user must be able to return easily to the main chat after reviewing answer details.

## Acceptance Criteria

Given an end user is on the main chat screen  
When the user clicks a chat answer  
Then a side view should open showing the selected answer details.

Given the answer detail side view is open  
When the selected answer has at least one source  
Then the user should see the answer content and a relevant snippet from one source.

Given the selected answer has multiple sources  
When the user chooses a different source from the available options  
Then the displayed source snippet should update to match that source.

Given the answer detail side view shows a source snippet  
When the user wants more context  
Then the user should be able to open the full source document.

Given an answer has no sources  
When the user interacts with that answer  
Then the chat should show a warning indicating that source information is not available.

Given a source cannot be loaded  
When the user tries to view the source details  
Then the chat should show a warning indicating that the source could not be loaded.

Given the user is reviewing an answer in the detail side view  
When the user closes or leaves the detail view  
Then the user should return easily to the main chat screen.

## Functional Test Scenarios

### Test: Open answer detail from the chat

Steps:

1. Open the main chat screen.
2. Click a chat answer.

Expected result:

- A side view opens.
- The selected answer is shown in the detail view.
- The main chat remains available for return.

### Test: Review source snippet for a selected answer

Steps:

1. Open the detail side view for an answer with source information.
2. Review the displayed answer and source content.

Expected result:

- The answer content is visible.
- A relevant snippet from one source is visible.
- The displayed information helps the user verify the answer.

### Test: Switch between multiple sources

Steps:

1. Open the detail side view for an answer with multiple sources.
2. Use the source selector to choose another source.

Expected result:

- All available sources can be selected.
- The displayed snippet changes based on the selected source.

### Test: Open full source document

Steps:

1. Open the detail side view for an answer with source information.
2. Select a source.
3. Open the full document from the detail view.

Expected result:

- The user can access the full source document.
- The user can still understand which answer the source belongs to.

### Test: Warn when source information is unavailable

Steps:

1. Open the main chat screen.
2. Interact with an answer that has no sources or whose source cannot be loaded.

Expected result:

- A warning is shown in the chat.
- The user is informed that source details are unavailable.

## Edge Cases

- The selected answer has no linked sources.
- One or more sources exist, but only some can be loaded.
- The answer has multiple sources and the user needs to switch repeatedly between them.
- The snippet is available, but the full document is not accessible.
- The user opens the detail side view and then wants to immediately return to the chat.
- Extending the answer experience must not remove or break the current chat behavior.
