# Create Module Integration Layer

## Related User Story

User Story: improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25

## Objective

Create an integration layer that orchestrates communication between the separated product area modules while maintaining clear boundaries and preventing direct coupling.

## Scope

- Create application orchestration layer
- Define module communication contracts
- Implement event-driven communication between modules
- Create REST API integration layer
- Establish module lifecycle management

## Out of Scope

- Complex event sourcing implementation
- Advanced message queuing systems
- Distributed system patterns
- Performance optimization beyond basic integration

## Clean Architecture Placement

interface adapters, infrastructure

## Execution Dependencies

- 0039-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-create_document_management_module.md
- 0040-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-create_chat_system_module.md
- 0041-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-create_user_management_module.md
- 0042-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-create_shared_kernel_module.md

## Implementation Details

Create integration layer structure:
```
backend/application-integration/
├── src/main/java/com/rag/app/integration/
│   ├── orchestration/
│   │   ├── ApplicationOrchestrator.java
│   │   └── ModuleCoordinator.java
│   ├── events/
│   │   ├── EventBus.java
│   │   ├── EventHandler.java
│   │   └── events/
│   │       ├── DocumentProcessedEvent.java
│   │       ├── UserAuthenticatedEvent.java
│   │       └── ChatQuerySubmittedEvent.java
│   ├── api/
│   │   ├── controllers/
│   │   │   ├── DocumentController.java
│   │   │   ├── ChatController.java
│   │   │   └── UserController.java
│   │   └── dto/
│   │       ├── ApiResponse.java
│   │       └── ErrorResponse.java
│   └── config/
│       ├── ApplicationConfiguration.java
│       └── ModuleConfiguration.java
├── src/test/java/
└── build.gradle
```

Module build.gradle:
```gradle
plugins {
    id 'java-library'
    id 'io.quarkus'
}

dependencies {
    // Module dependencies
    implementation project(':shared-kernel')
    implementation project(':document-management')
    implementation project(':chat-system')
    implementation project(':user-management')
    
    // Quarkus dependencies
    implementation enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusVersion}")
    implementation 'io.quarkus:quarkus-resteasy-reactive-jackson'
    implementation 'io.quarkus:quarkus-smallrye-health'
    implementation 'io.quarkus:quarkus-micrometer'
    
    // Test dependencies
    testImplementation 'io.quarkus:quarkus-junit5'
    testImplementation 'io.rest-assured:rest-assured'
}
```

Application Orchestrator:
```java
@ApplicationScoped
public class ApplicationOrchestrator {
    
    private final DocumentManagementFacade documentManagement;
    private final ChatSystemFacade chatSystem;
    private final UserManagementFacade userManagement;
    private final EventBus eventBus;
    
    public ApplicationOrchestrator(
            DocumentManagementFacade documentManagement,
            ChatSystemFacade chatSystem,
            UserManagementFacade userManagement,
            EventBus eventBus) {
        this.documentManagement = documentManagement;
        this.chatSystem = chatSystem;
        this.userManagement = userManagement;
        this.eventBus = eventBus;
    }
    
    @EventHandler
    public void handleDocumentUploaded(DocumentUploadedEvent event) {
        // Trigger document processing
        var processInput = new ProcessDocumentInput(event.getDocumentId());
        documentManagement.processDocument(processInput);
    }
    
    @EventHandler
    public void handleDocumentProcessed(DocumentProcessedEvent event) {
        if (event.getStatus() == DocumentStatus.READY) {
            // Store document vectors for search
            chatSystem.storeDocumentVectors(
                event.getDocumentId(), 
                event.getExtractedContent()
            );
        }
    }
    
    @EventHandler
    public void handleUserAuthenticated(UserAuthenticatedEvent event) {
        // Initialize user session context
        // Could trigger welcome message or setup user preferences
    }
    
    public QueryDocumentsOutput processQuery(String userId, String question) {
        // Validate user authorization
        if (!userManagement.isAuthorized(new UserId(userId), "query_documents", "read")) {
            throw new UnauthorizedException("User not authorized to query documents");
        }
        
        // Get accessible documents for user
        var userRole = userManagement.getUserRole(new UserId(userId));
        var accessibleDocs = getAccessibleDocuments(userId, userRole);
        
        // Process query through chat system
        var queryInput = new QueryDocumentsInput(userId, question, accessibleDocs);
        return chatSystem.queryDocuments(queryInput);
    }
    
    private List<String> getAccessibleDocuments(String userId, UserRole role) {
        if (role == UserRole.ADMIN) {
            // Admin can access all documents
            return documentManagement.getAllDocumentIds();
        } else {
            // Standard user can only access own documents
            var userDocsInput = new GetUserDocumentsInput(userId, false);
            var userDocs = documentManagement.getUserDocuments(userDocsInput);
            return userDocs.getDocuments().stream()
                .map(doc -> doc.getDocumentId())
                .toList();
        }
    }
}
```

Event Bus Implementation:
```java
@ApplicationScoped
public class EventBus {
    
    private final Map<Class<? extends DomainEvent>, List<EventHandler>> handlers = new ConcurrentHashMap<>();
    private final ExecutorService eventExecutor = Executors.newCachedThreadPool();
    
    public void register(Class<? extends DomainEvent> eventType, EventHandler handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }
    
    public void publish(DomainEvent event) {
        var eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            eventHandlers.forEach(handler -> 
                eventExecutor.submit(() -> {
                    try {
                        handler.handle(event);
                    } catch (Exception e) {
                        // Log error but don't fail the original operation
                        System.err.println("Error handling event: " + e.getMessage());
                    }
                })
            );
        }
    }
    
    @PreDestroy
    public void shutdown() {
        eventExecutor.shutdown();
    }
}

@FunctionalInterface
public interface EventHandler {
    void handle(DomainEvent event);
}
```

Domain Events:
```java
// Document events
public class DocumentUploadedEvent extends DomainEvent {
    private final String documentId;
    private final String fileName;
    private final String uploadedBy;
    
    public DocumentUploadedEvent(String documentId, String fileName, String uploadedBy) {
        super("DocumentUploaded");
        this.documentId = documentId;
        this.fileName = fileName;
        this.uploadedBy = uploadedBy;
    }
    
    // Getters...
}

public class DocumentProcessedEvent extends DomainEvent {
    private final String documentId;
    private final DocumentStatus status;
    private final String extractedContent;
    
    public DocumentProcessedEvent(String documentId, DocumentStatus status, String extractedContent) {
        super("DocumentProcessed");
        this.documentId = documentId;
        this.status = status;
        this.extractedContent = extractedContent;
    }
    
    // Getters...
}

// User events
public class UserAuthenticatedEvent extends DomainEvent {
    private final String userId;
    private final UserRole role;
    
    public UserAuthenticatedEvent(String userId, UserRole role) {
        super("UserAuthenticated");
        this.userId = userId;
        this.role = role;
    }
    
    // Getters...
}

// Chat events
public class ChatQuerySubmittedEvent extends DomainEvent {
    private final String userId;
    private final String question;
    private final String messageId;
    
    public ChatQuerySubmittedEvent(String userId, String question, String messageId) {
        super("ChatQuerySubmitted");
        this.userId = userId;
        this.question = question;
        this.messageId = messageId;
    }
    
    // Getters...
}
```

REST API Controllers:
```java
@Path("/api/documents")
@ApplicationScoped
public class DocumentController {
    
    private final ApplicationOrchestrator orchestrator;
    private final DocumentManagementFacade documentManagement;
    
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadDocument(@MultipartForm UploadRequest request) {
        try {
            var input = new UploadDocumentInput(
                request.file.fileName(),
                request.file.size(),
                determineFileType(request.file.fileName()),
                request.file.uploadedFile(),
                request.userId
            );
            
            var result = documentManagement.uploadDocument(input);
            
            // Publish event for processing
            orchestrator.publishEvent(new DocumentUploadedEvent(
                result.getDocumentId(),
                request.file.fileName(),
                request.userId
            ));
            
            return Response.status(201).entity(result).build();
        } catch (ValidationException e) {
            return Response.status(400)
                .entity(new ErrorResponse("VALIDATION_ERROR", e.getMessage()))
                .build();
        }
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserDocuments(@QueryParam("includeAll") boolean includeAll,
                                   @Context SecurityContext securityContext) {
        var userId = securityContext.getUserPrincipal().getName();
        var input = new GetUserDocumentsInput(userId, includeAll);
        var result = documentManagement.getUserDocuments(input);
        
        return Response.ok(result).build();
    }
}

@Path("/api/chat")
@ApplicationScoped
public class ChatController {
    
    private final ApplicationOrchestrator orchestrator;
    
    @POST
    @Path("/query")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response submitQuery(ChatQueryRequest request,
                              @Context SecurityContext securityContext) {
        var userId = securityContext.getUserPrincipal().getName();
        
        try {
            var result = orchestrator.processQuery(userId, request.question());
            
            // Publish event for analytics/logging
            orchestrator.publishEvent(new ChatQuerySubmittedEvent(
                userId,
                request.question(),
                result.getMessageId()
            ));
            
            return Response.ok(result).build();
        } catch (UnauthorizedException e) {
            return Response.status(403)
                .entity(new ErrorResponse("UNAUTHORIZED", e.getMessage()))
                .build();
        }
    }
}
```

Module Configuration:
```java
@ApplicationScoped
public class ModuleConfiguration {
    
    @Produces
    @ApplicationScoped
    public ApplicationOrchestrator applicationOrchestrator(
            DocumentManagementFacade documentManagement,
            ChatSystemFacade chatSystem,
            UserManagementFacade userManagement,
            EventBus eventBus) {
        return new ApplicationOrchestrator(
            documentManagement, chatSystem, userManagement, eventBus
        );
    }
    
    @Produces
    @ApplicationScoped
    public EventBus eventBus() {
        return new EventBus();
    }
    
    @PostConstruct
    public void registerEventHandlers() {
        // Register cross-module event handlers
        var eventBus = eventBus();
        var orchestrator = applicationOrchestrator(null, null, null, eventBus);
        
        eventBus.register(DocumentUploadedEvent.class, orchestrator::handleDocumentUploaded);
        eventBus.register(DocumentProcessedEvent.class, orchestrator::handleDocumentProcessed);
        eventBus.register(UserAuthenticatedEvent.class, orchestrator::handleUserAuthenticated);
    }
}
```

Health Check Integration:
```java
@ApplicationScoped
public class ModuleHealthCheck implements HealthCheck {
    
    private final DocumentManagementFacade documentManagement;
    private final ChatSystemFacade chatSystem;
    private final UserManagementFacade userManagement;
    
    @Override
    public HealthCheckResponse call() {
        var builder = HealthCheckResponse.named("modules");
        
        try {
            boolean allHealthy = documentManagement.isHealthy() &&
                               chatSystem.isHealthy() &&
                               userManagement.isHealthy();
            
            if (allHealthy) {
                builder.up();
            } else {
                builder.down()
                    .withData("document-management", documentManagement.isHealthy())
                    .withData("chat-system", chatSystem.isHealthy())
                    .withData("user-management", userManagement.isHealthy());
            }
        } catch (Exception e) {
            builder.down().withData("error", e.getMessage());
        }
        
        return builder.build();
    }
}
```

## Files / Modules Impacted

- backend/application-integration/build.gradle
- backend/application-integration/src/main/java/com/rag/app/integration/orchestration/ApplicationOrchestrator.java
- backend/application-integration/src/main/java/com/rag/app/integration/events/EventBus.java
- backend/application-integration/src/main/java/com/rag/app/integration/api/controllers/DocumentController.java
- backend/application-integration/src/main/java/com/rag/app/integration/api/controllers/ChatController.java
- backend/settings.gradle (add module)

## Acceptance Criteria

Given the integration layer is implemented
When modules need to communicate
Then they should only do so through the orchestrator or events

Given events are published between modules
When cross-module operations occur
Then they should be handled asynchronously without direct coupling

Given REST APIs are implemented
When external clients make requests
Then they should be routed through the integration layer

Given health checks are implemented
When system health is queried
Then the status of all modules should be reported

## Testing Requirements

- Integration tests for module communication
- Event handling tests
- REST API integration tests
- Health check tests
- Error handling and resilience tests

## Dependencies / Preconditions

- All product area modules must be implemented
- Shared kernel must be available
- Event handling patterns must be established
- REST API contracts must be defined