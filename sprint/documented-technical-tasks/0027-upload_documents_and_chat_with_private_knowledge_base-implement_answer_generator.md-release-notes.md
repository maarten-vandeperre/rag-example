## Summary
Implemented an answer-generation pipeline that builds grounded prompts, synthesizes answers from retrieved chunks, validates grounding, and returns source references.

## Changes
Added `backend/src/main/java/com/rag/app/infrastructure/llm/LlmClient.java`.
Added `backend/src/main/java/com/rag/app/infrastructure/llm/PromptTemplate.java`.
Added `backend/src/main/java/com/rag/app/infrastructure/llm/ResponseValidator.java`.
Added `backend/src/main/java/com/rag/app/infrastructure/llm/HeuristicLlmClient.java`.
Added `backend/src/main/java/com/rag/app/infrastructure/llm/AnswerGeneratorImpl.java`.
Added `backend/src/test/java/com/rag/app/infrastructure/llm/PromptTemplateTest.java`.
Added `backend/src/test/java/com/rag/app/infrastructure/llm/ResponseValidatorTest.java`.
Added `backend/src/test/java/com/rag/app/infrastructure/llm/AnswerGeneratorImplTest.java`.

## Impact
Chat queries can now turn retrieved document chunks into validated, source-backed answers through the `AnswerGenerator` contract without depending on a remote LLM service during local development and testing.

## Verification
Ran `mvn -q -Dquarkus.platform.group-id=io.quarkus -Dquarkus.platform.artifact-id=quarkus-bom -Dquarkus.platform.version=2.16.5.Final -DskipTests compile`.
Ran `mvn -q -Dquarkus.platform.group-id=io.quarkus -Dquarkus.platform.artifact-id=quarkus-bom -Dquarkus.platform.version=2.16.5.Final test`.

## Follow-ups
Swap the heuristic client for a real provider-backed `LlmClient` implementation once API credentials, timeout policy, and deployment configuration are finalized.
