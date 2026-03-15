# Create Chat System Module

## Related User Story

User Story: improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25

## Objective

Create a dedicated chat system module that encapsulates all chat and query functionality including semantic search, LLM integration, and answer generation with clear boundaries.

## Scope

- Create chat-system module structure
- Move chat-related domain entities to the module
- Move query and search use cases to the module
- Move vector store and LLM integrations to the module
- Define clear module interfaces and boundaries

## Out of Scope

- Java 25 migration (handled separately)
- Frontend module reorganization
- Document management integration (handled through interfaces)
- Performance optimization

## Clean Architecture Placement

domain, usecases, infrastructure

## Execution Dependencies

- 0038-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-analyze_current_module_structure.md
- 0039-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-create_document_management_module.md

## Implementation Details

Create chat-system module structure:
```
backend/chat-system/
├── src/main/java/com/rag/app/chat/
│   ├── domain/
│   │   ├── entities/
│   │   │   └── ChatMessage.java
│   │   ├── valueobjects/
│   │   │   ├── DocumentReference.java
│   │   │   └── QueryContext.java
│   │   └── services/
│   │       └── ChatDomainService.java
│   ├── usecases/
│   │   ├── QueryDocuments.java
│   │   ├── GenerateAnswer.java
│   │   └── GetChatHistory.java
│   ├── interfaces/
│   │   ├── ChatMessageRepository.java
│   │   ├── SemanticSearch.java
│   │   ├── AnswerGenerator.java
│   │   └── VectorStore.java
│   └── infrastructure/
│       ├── persistence/
│       │   └── JdbcChatMessageRepository.java
│       ├── search/
│       │   └── WeaviateVectorStore.java
│       └── llm/
│           └── OllamaAnswerGenerator.java
├── src/test/java/
└── build.gradle
```

Module build.gradle:
```gradle
plugins {
    id 'java-library'
}

dependencies {
    // Only shared-kernel dependencies allowed
    api project(':shared-kernel')
    
    // Infrastructure dependencies
    implementation 'org.postgresql:postgresql'
    implementation 'io.weaviate:client:4.0.0'
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    
    // Test dependencies
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.mockito:mockito-core'
}

// Enforce module boundaries
configurations.all {
    resolutionStrategy.eachDependency { details ->
        if (details.requested.group == 'com.rag.app' && 
            details.requested.name != 'shared-kernel') {
            throw new GradleException(
                "Chat system module cannot depend on other application modules: ${details.requested}"
            )
        }
    }
}
```

Module interface definition (ChatSystemFacade.java):
```java
public interface ChatSystemFacade {
    // Query operations
    QueryDocumentsOutput queryDocuments(QueryDocumentsInput input);
    
    // Chat history operations
    GetChatHistoryOutput getChatHistory(GetChatHistoryInput input);
    
    // Vector operations
    void storeDocumentVectors(String documentId, String content);
    void removeDocumentVectors(String documentId);
    
    // Search operations
    List<DocumentChunk> searchSimilarContent(String query, List<String> documentIds);
}
```

Module configuration:
```java
@Configuration
@ComponentScan(basePackages = "com.rag.app.chat")
public class ChatSystemConfiguration {
    
    @Bean
    public ChatSystemFacade chatSystemFacade(
            QueryDocuments queryDocuments,
            GetChatHistory getChatHistory,
            VectorStore vectorStore,
            SemanticSearch semanticSearch) {
        return new ChatSystemFacadeImpl(
            queryDocuments, getChatHistory, 
            vectorStore, semanticSearch
        );
    }
    
    @Bean
    @ConditionalOnProperty(name = "app.llm.provider", havingValue = "ollama")
    public AnswerGenerator ollamaAnswerGenerator() {
        return new OllamaAnswerGenerator();
    }
    
    @Bean
    @ConditionalOnProperty(name = "app.vectorstore.provider", havingValue = "weaviate")
    public VectorStore weaviateVectorStore() {
        return new WeaviateVectorStore();
    }
}
```

Domain model refinements:
```java
// ChatMessage entity with clear boundaries
public class ChatMessage {
    private final MessageId messageId;
    private final UserId userId;
    private final Question question;
    private final Answer answer;
    private final List<DocumentReference> documentReferences;
    private final Timestamp createdAt;
    private final Duration responseTime;
    
    // Domain methods for chat-specific logic
    public boolean isAnswered() { return answer != null; }
    public boolean hasDocumentReferences() { return !documentReferences.isEmpty(); }
    public boolean isWithinResponseTimeLimit(Duration limit) { return responseTime.compareTo(limit) <= 0; }
}

// DocumentReference value object
public class DocumentReference {
    private final DocumentId documentId;
    private final DocumentName documentName;
    private final ParagraphReference paragraphReference;
    private final RelevanceScore relevanceScore;
    
    // Value object methods
    public boolean isHighlyRelevant() { return relevanceScore.isAbove(0.8); }
}
```

Integration interfaces:
```java
// Interface for document access (no direct dependency)
public interface DocumentAccessService {
    List<DocumentSummary> getAccessibleDocuments(UserId userId, UserRole role);
    boolean isDocumentAccessible(DocumentId documentId, UserId userId, UserRole role);
}

// Interface for user context (no direct dependency)
public interface UserContextService {
    UserRole getUserRole(UserId userId);
    boolean isActiveUser(UserId userId);
}
```

## Files / Modules Impacted

- backend/chat-system/build.gradle
- backend/chat-system/src/main/java/com/rag/app/chat/domain/entities/ChatMessage.java
- backend/chat-system/src/main/java/com/rag/app/chat/usecases/QueryDocuments.java
- backend/chat-system/src/main/java/com/rag/app/chat/interfaces/ChatSystemFacade.java
- backend/chat-system/src/main/java/com/rag/app/chat/infrastructure/ChatSystemFacadeImpl.java
- backend/settings.gradle (add module)

## Acceptance Criteria

Given the chat system module is created
When the module is built independently
Then it should compile and test successfully without other modules

Given chat functionality is moved to the module
When query operations are performed
Then they should work through the module facade

Given module boundaries are enforced
When attempting to add dependencies to other modules
Then the build should fail with clear error messages

Given integration interfaces are defined
When the module needs external data
Then it should only access it through defined interfaces

## Testing Requirements

- Unit tests for all moved components
- Integration tests for module facade
- Module boundary enforcement tests
- LLM integration tests with mocks
- Vector store integration tests

## Dependencies / Preconditions

- Current module structure analysis must be completed
- Shared-kernel module must be defined
- Document management module structure must be established
- Understanding of chat and query functionality