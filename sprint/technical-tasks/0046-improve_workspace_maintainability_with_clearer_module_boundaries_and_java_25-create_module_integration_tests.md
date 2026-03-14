# Create Module Integration Tests

## Related User Story

User Story: improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25

## Objective

Create comprehensive integration tests that validate the modular architecture works correctly, modules communicate properly through defined interfaces, and the overall system maintains existing functionality while improving maintainability.

## Scope

- Create integration tests for module boundaries and communication
- Test cross-module workflows and event handling
- Validate that existing functionality is preserved
- Test module isolation and independence
- Create end-to-end workflow tests

## Out of Scope

- Performance testing of modular architecture
- Load testing across modules
- Security testing beyond basic authorization
- UI integration testing (covered separately)

## Clean Architecture Placement

testing

## Execution Dependencies

- 0045-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-create_module_integration_layer.md
- 0043-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-upgrade_to_java_25.md
- 0044-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-reorganize_frontend_module_structure.md

## Implementation Details

Create integration test structure:
```
backend/integration-tests/
├── src/test/java/com/rag/app/integration/
│   ├── modules/
│   │   ├── ModuleBoundaryTest.java
│   │   ├── ModuleCommunicationTest.java
│   │   └── ModuleIsolationTest.java
│   ├── workflows/
│   │   ├── DocumentUploadWorkflowTest.java
│   │   ├── ChatQueryWorkflowTest.java
│   │   └── UserManagementWorkflowTest.java
│   ├── events/
│   │   ├── EventBusIntegrationTest.java
│   │   └── CrossModuleEventTest.java
│   ├── api/
│   │   ├── RestApiIntegrationTest.java
│   │   └── ErrorHandlingIntegrationTest.java
│   └── system/
│       ├── EndToEndWorkflowTest.java
│       ├── SystemHealthTest.java
│       └── BackwardCompatibilityTest.java
├── src/test/resources/
│   ├── test-data/
│   └── application-test.properties
└── build.gradle
```

Module build.gradle:
```gradle
plugins {
    id 'java'
    id 'io.quarkus'
}

dependencies {
    // All application modules for integration testing
    testImplementation project(':shared-kernel')
    testImplementation project(':document-management')
    testImplementation project(':chat-system')
    testImplementation project(':user-management')
    testImplementation project(':application-integration')
    
    // Test dependencies
    testImplementation 'io.quarkus:quarkus-junit5'
    testImplementation 'io.rest-assured:rest-assured'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:weaviate'
    testImplementation 'org.awaitility:awaitility'
    testImplementation 'org.assertj:assertj-core'
}

test {
    systemProperty 'java.util.logging.manager', 'org.jboss.logmanager.LogManager'
    useJUnitPlatform()
    
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
}
```

Module Boundary Tests:
```java
@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
class ModuleBoundaryTest {
    
    @Test
    @Order(1)
    void shouldEnforceModuleBoundariesAtCompileTime() {
        // This test validates that modules cannot directly access each other
        // It uses reflection to check that no direct dependencies exist
        
        var documentModule = getModuleClasses("com.rag.app.document");
        var chatModule = getModuleClasses("com.rag.app.chat");
        var userModule = getModuleClasses("com.rag.app.user");
        
        // Document module should not directly reference chat or user modules
        assertThat(getDirectDependencies(documentModule))
            .noneMatch(dep -> dep.startsWith("com.rag.app.chat") || dep.startsWith("com.rag.app.user"));
        
        // Chat module should not directly reference document or user modules
        assertThat(getDirectDependencies(chatModule))
            .noneMatch(dep -> dep.startsWith("com.rag.app.document") || dep.startsWith("com.rag.app.user"));
        
        // User module should not directly reference document or chat modules
        assertThat(getDirectDependencies(userModule))
            .noneMatch(dep -> dep.startsWith("com.rag.app.document") || dep.startsWith("com.rag.app.chat"));
    }
    
    @Test
    @Order(2)
    void shouldOnlyAllowAccessThroughFacades() {
        // Verify that modules only expose their facade interfaces
        var documentFacade = DocumentManagementFacade.class;
        var chatFacade = ChatSystemFacade.class;
        var userFacade = UserManagementFacade.class;
        
        // Check that facades are the only public interfaces
        assertThat(documentFacade.isInterface()).isTrue();
        assertThat(chatFacade.isInterface()).isTrue();
        assertThat(userFacade.isInterface()).isTrue();
        
        // Verify internal classes are not accessible
        assertThatThrownBy(() -> Class.forName("com.rag.app.document.infrastructure.JdbcDocumentRepository"))
            .isInstanceOf(ClassNotFoundException.class);
    }
    
    private Set<String> getModuleClasses(String packageName) {
        // Implementation to scan for classes in package
        return Set.of(); // Simplified for example
    }
    
    private Set<String> getDirectDependencies(Set<String> classes) {
        // Implementation to analyze class dependencies
        return Set.of(); // Simplified for example
    }
}
```

Module Communication Tests:
```java
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModuleCommunicationTest {
    
    @Inject
    ApplicationOrchestrator orchestrator;
    
    @Inject
    EventBus eventBus;
    
    @Inject
    DocumentManagementFacade documentManagement;
    
    @Inject
    ChatSystemFacade chatSystem;
    
    @Inject
    UserManagementFacade userManagement;
    
    @Test
    void shouldCommunicateThroughEvents() {
        // Test that modules communicate through events, not direct calls
        var eventReceived = new AtomicBoolean(false);
        
        // Register test event handler
        eventBus.register(DocumentProcessedEvent.class, event -> {
            eventReceived.set(true);
        });
        
        // Trigger document processing
        var uploadInput = createTestUploadInput();
        var uploadResult = documentManagement.uploadDocument(uploadInput);
        
        var processInput = new ProcessDocumentInput(uploadResult.getDocumentId());
        documentManagement.processDocument(processInput);
        
        // Verify event was published
        await().atMost(Duration.ofSeconds(5))
            .until(() -> eventReceived.get());
    }
    
    @Test
    void shouldMaintainDataConsistencyAcrossModules() {
        // Test that data remains consistent when accessed through different modules
        
        // Create user through user management
        var user = createTestUser();
        var userResult = userManagement.createUser(user);
        
        // Upload document through document management
        var uploadInput = createTestUploadInput(userResult.getUserId());
        var documentResult = documentManagement.uploadDocument(uploadInput);
        
        // Query through chat system
        var queryInput = new QueryDocumentsInput(
            userResult.getUserId(), 
            "test question", 
            20000
        );
        var queryResult = chatSystem.queryDocuments(queryInput);
        
        // Verify data consistency
        assertThat(queryResult.getSuccess()).isTrue();
        assertThat(documentResult.getDocumentId()).isNotNull();
    }
    
    @Test
    void shouldHandleModuleFailuresGracefully() {
        // Test that failure in one module doesn't break others
        
        // Simulate chat system failure
        var invalidQuery = new QueryDocumentsInput("invalid-user", "", -1);
        
        assertThatThrownBy(() -> chatSystem.queryDocuments(invalidQuery))
            .isInstanceOf(ValidationException.class);
        
        // Verify other modules still work
        var healthyUpload = createTestUploadInput();
        var result = documentManagement.uploadDocument(healthyUpload);
        
        assertThat(result.getDocumentId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
    }
}
```

End-to-End Workflow Tests:
```java
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class EndToEndWorkflowTest {
    
    @Inject
    ApplicationOrchestrator orchestrator;
    
    private String testUserId;
    private String testDocumentId;
    
    @Test
    @Order(1)
    void shouldCompleteFullDocumentUploadWorkflow() {
        // 1. Create user
        var userInput = createTestUserInput();
        var userResult = orchestrator.createUser(userInput);
        testUserId = userResult.getUserId();
        
        assertThat(testUserId).isNotNull();
        
        // 2. Upload document
        var uploadInput = createTestUploadInput(testUserId);
        var uploadResult = orchestrator.uploadDocument(uploadInput);
        testDocumentId = uploadResult.getDocumentId();
        
        assertThat(testDocumentId).isNotNull();
        assertThat(uploadResult.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        
        // 3. Process document (should happen automatically via events)
        await().atMost(Duration.ofSeconds(30))
            .until(() -> {
                var doc = orchestrator.getDocument(testDocumentId);
                return doc.isPresent() && doc.get().getStatus() == DocumentStatus.READY;
            });
    }
    
    @Test
    @Order(2)
    void shouldCompleteFullChatQueryWorkflow() {
        // Ensure document is ready from previous test
        assertThat(testDocumentId).isNotNull();
        
        // Submit chat query
        var queryResult = orchestrator.processQuery(testUserId, "What is in the document?");
        
        assertThat(queryResult.getSuccess()).isTrue();
        assertThat(queryResult.getAnswer()).isNotEmpty();
        assertThat(queryResult.getDocumentReferences()).isNotEmpty();
        assertThat(queryResult.getResponseTimeMs()).isLessThan(20000);
        
        // Verify document reference points to our uploaded document
        var hasOurDocument = queryResult.getDocumentReferences().stream()
            .anyMatch(ref -> ref.getDocumentId().equals(testDocumentId));
        assertThat(hasOurDocument).isTrue();
    }
    
    @Test
    @Order(3)
    void shouldMaintainRoleBasedAccessControl() {
        // Create admin user
        var adminInput = createTestAdminUserInput();
        var adminResult = orchestrator.createUser(adminInput);
        
        // Admin should see all documents
        var adminDocs = orchestrator.getUserDocuments(adminResult.getUserId(), true);
        assertThat(adminDocs.getDocuments()).hasSizeGreaterThanOrEqualTo(1);
        
        // Standard user should only see own documents
        var userDocs = orchestrator.getUserDocuments(testUserId, false);
        assertThat(userDocs.getDocuments()).hasSize(1);
        assertThat(userDocs.getDocuments().get(0).getDocumentId()).isEqualTo(testDocumentId);
    }
    
    @Test
    @Order(4)
    void shouldPreserveExistingFunctionality() {
        // Test that all original functionality still works after modularization
        
        // Test document library functionality
        var userDocs = orchestrator.getUserDocuments(testUserId, false);
        assertThat(userDocs.getTotalCount()).isEqualTo(1);
        
        // Test admin progress functionality
        var adminProgress = orchestrator.getAdminProgress(testUserId);
        assertThat(adminProgress.getProcessingStatistics().getTotalDocuments()).isGreaterThan(0);
        
        // Test chat functionality with source references
        var chatResult = orchestrator.processQuery(testUserId, "Summarize the content");
        assertThat(chatResult.getAnswer()).isNotEmpty();
        assertThat(chatResult.getDocumentReferences()).isNotEmpty();
    }
}
```

System Health Tests:
```java
@QuarkusTest
class SystemHealthTest {
    
    @Test
    void shouldReportHealthySystemWhenAllModulesAreWorking() {
        given()
            .when().get("/q/health")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"))
            .body("checks.find { it.name == 'modules' }.status", equalTo("UP"));
    }
    
    @Test
    void shouldReportModuleSpecificHealth() {
        given()
            .when().get("/q/health")
            .then()
            .statusCode(200)
            .body("checks.find { it.name == 'modules' }.data.'document-management'", equalTo(true))
            .body("checks.find { it.name == 'modules' }.data.'chat-system'", equalTo(true))
            .body("checks.find { it.name == 'modules' }.data.'user-management'", equalTo(true));
    }
}
```

Backward Compatibility Tests:
```java
@QuarkusTest
class BackwardCompatibilityTest {
    
    @Test
    void shouldMaintainExistingApiContracts() {
        // Test that existing API endpoints still work
        
        // Document upload API
        given()
            .multiPart("file", "test.txt", "test content".getBytes())
            .multiPart("userId", "test-user")
            .when().post("/api/documents/upload")
            .then()
            .statusCode(201)
            .body("documentId", notNullValue())
            .body("status", equalTo("UPLOADED"));
        
        // Chat query API
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "question": "What is in the document?",
                    "maxResponseTimeMs": 20000
                }
                """)
            .when().post("/api/chat/query")
            .then()
            .statusCode(200)
            .body("answer", notNullValue())
            .body("success", equalTo(true));
    }
    
    @Test
    void shouldMaintainExistingDataStructures() {
        // Verify that data structures haven't changed in breaking ways
        
        var response = given()
            .when().get("/api/documents")
            .then()
            .statusCode(200)
            .extract().response();
        
        var documents = response.jsonPath().getList("documents");
        if (!documents.isEmpty()) {
            var firstDoc = response.jsonPath().getMap("documents[0]");
            
            // Verify expected fields are present
            assertThat(firstDoc).containsKeys(
                "documentId", "fileName", "fileSize", 
                "fileType", "status", "uploadedBy", "uploadedAt"
            );
        }
    }
}
```

Test Configuration:
```properties
# application-test.properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=test
quarkus.datasource.password=test
quarkus.datasource.jdbc.url=jdbc:tc:postgresql:15:///test

# Test-specific configurations
app.test.cleanup-after-test=true
app.test.use-embedded-services=true

# Logging for tests
quarkus.log.category."com.rag.app".level=DEBUG
quarkus.log.category."org.testcontainers".level=INFO
```

## Files / Modules Impacted

- backend/integration-tests/build.gradle
- backend/integration-tests/src/test/java/com/rag/app/integration/modules/ModuleBoundaryTest.java
- backend/integration-tests/src/test/java/com/rag/app/integration/workflows/EndToEndWorkflowTest.java
- backend/integration-tests/src/test/java/com/rag/app/integration/system/SystemHealthTest.java
- backend/integration-tests/src/test/java/com/rag/app/integration/system/BackwardCompatibilityTest.java
- backend/integration-tests/src/test/resources/application-test.properties
- backend/settings.gradle (add integration-tests module)

## Acceptance Criteria

Given the modular architecture is implemented
When integration tests are executed
Then all module boundaries should be enforced correctly

Given cross-module workflows are tested
When end-to-end scenarios are executed
Then they should complete successfully with proper module communication

Given existing functionality is tested
When backward compatibility tests run
Then all original features should work unchanged

Given system health is tested
When health checks are performed
Then all modules should report healthy status

## Testing Requirements

- Module boundary enforcement tests
- Cross-module communication tests
- End-to-end workflow tests
- Backward compatibility tests
- System health and resilience tests

## Dependencies / Preconditions

- All modules must be implemented and integrated
- Integration layer must be complete
- Test infrastructure must be set up
- Java 25 upgrade must be complete
- Frontend modularization should be complete