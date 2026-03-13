## Summary
Added the chat message domain model and source citation value object for storing question/answer interactions with referenced documents.

## Changes
- `backend/src/main/java/com/rag/app/domain/entities/ChatMessage.java`
- `backend/src/main/java/com/rag/app/domain/valueobjects/DocumentReference.java`
- `backend/src/test/java/com/rag/app/domain/entities/ChatMessageTest.java`
- `backend/src/test/java/com/rag/app/domain/valueobjects/DocumentReferenceTest.java`

## Impact
The backend domain can now represent chat exchanges alongside source document references, enabling later retrieval and citation features.

## Verification
- `mvn -s maven-settings.xml -U compile`
- `mvn -s maven-settings.xml -U test`

## Follow-ups
- Add a dedicated chat session aggregate when conversation threading is introduced.
- Connect `DocumentReference` to richer citation metadata if page numbers or chunk identifiers become available.
