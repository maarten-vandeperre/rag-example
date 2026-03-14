# Implement Answer Generator

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Implement the AnswerGenerator interface to generate answers from document chunks using LLM integration with proper source references.

## Scope

- Implement AnswerGenerator interface for LLM-based answer generation
- Create prompt templates for question answering
- Handle source reference extraction and formatting
- Implement response validation and error handling

## Out of Scope

- LLM model training or fine-tuning
- Advanced prompt engineering
- Multi-turn conversation support
- Answer quality scoring

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0006-upload_documents_and_chat_with_private_knowledge_base-create_query_documents_usecase.md
- 0020-upload_documents_and_chat_with_private_knowledge_base-implement_vector_store.md

## Implementation Details

Implement AnswerGeneratorImpl with:
- generateAnswer(String question, List<DocumentChunk> context) method
- LLM integration for answer generation
- Prompt template construction
- Source reference extraction
- Response validation

Create prompt template:
```
Based on the following document excerpts, answer the user's question. 
If the answer cannot be found in the provided context, say "I cannot find relevant information in the provided documents."

Context:
[Document chunks with source information]

Question: {user_question}

Instructions:
- Provide a clear, concise answer based only on the provided context
- Include specific references to source documents and sections
- If information is unclear or missing, acknowledge this limitation
- Do not make assumptions beyond what is stated in the context

Answer:
```

Source reference handling:
- Extract document names and chunk references from context
- Map generated answer back to source chunks
- Create DocumentReference objects with relevance scores
- Handle cases where answer spans multiple sources

LLM integration:
- Use OpenAI API, Anthropic Claude, or local LLM
- Handle API rate limits and errors
- Implement request timeout (within 20-second limit)
- Parse and validate LLM responses

Response validation:
- Check if answer is relevant to question
- Validate that answer is grounded in provided context
- Filter out hallucinated information
- Ensure source references are accurate

Error handling:
- LLM API failures
- Invalid or empty responses
- Timeout errors
- Context too large for model

## Files / Modules Impacted

- backend/infrastructure/llm/AnswerGeneratorImpl.java
- backend/infrastructure/llm/PromptTemplate.java
- backend/infrastructure/llm/LlmClient.java
- backend/infrastructure/llm/ResponseValidator.java

## Acceptance Criteria

Given a question and relevant document chunks
When generateAnswer() is called
Then a grounded answer with source references should be returned

Given a question with no relevant context
When generateAnswer() is called
Then "no relevant information" message should be returned

Given document chunks from multiple sources
When generateAnswer() is called
Then the answer should include references to all relevant sources

Given LLM API fails
When generateAnswer() is called
Then an appropriate error should be thrown

## Testing Requirements

- Unit tests for AnswerGeneratorImpl
- Unit tests for prompt template construction
- Unit tests for source reference extraction
- Integration tests with LLM API
- Tests for error handling and timeouts

## Dependencies / Preconditions

- AnswerGenerator interface must be defined
- LLM API access must be configured
- DocumentChunk and DocumentReference models must exist
- Vector store implementation must be available