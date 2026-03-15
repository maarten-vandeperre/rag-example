package com.rag.app.integration.support;

import com.rag.app.chat.domain.entities.ChatMessage;
import com.rag.app.chat.domain.valueobjects.DocumentReference;
import com.rag.app.chat.interfaces.ChatSystemFacade;
import com.rag.app.chat.usecases.models.DocumentChunk;
import com.rag.app.chat.usecases.models.GetChatHistoryInput;
import com.rag.app.chat.usecases.models.GetChatHistoryOutput;
import com.rag.app.chat.usecases.models.QueryDocumentsInput;
import com.rag.app.chat.usecases.models.QueryDocumentsOutput;
import com.rag.app.document.domain.entities.Document;
import com.rag.app.document.domain.valueobjects.DocumentMetadata;
import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.domain.valueobjects.FileType;
import com.rag.app.document.interfaces.DocumentManagementFacade;
import com.rag.app.document.usecases.models.GetAdminProgressInput;
import com.rag.app.document.usecases.models.GetAdminProgressOutput;
import com.rag.app.document.usecases.models.GetUserDocumentsInput;
import com.rag.app.document.usecases.models.GetUserDocumentsOutput;
import com.rag.app.document.usecases.models.ProcessDocumentInput;
import com.rag.app.document.usecases.models.ProcessDocumentOutput;
import com.rag.app.document.usecases.models.UploadDocumentInput;
import com.rag.app.document.usecases.models.UploadDocumentOutput;
import com.rag.app.integration.events.EventBus;
import com.rag.app.integration.orchestration.ApplicationOrchestrator;
import com.rag.app.integration.orchestration.ModuleCoordinator;
import com.rag.app.user.domain.entities.User;
import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.domain.valueobjects.UserRole;
import com.rag.app.user.interfaces.UserManagementFacade;
import com.rag.app.user.usecases.models.AuthenticationRequest;
import com.rag.app.user.usecases.models.AuthenticationResult;
import com.rag.app.user.usecases.models.GetUserProfileInput;
import com.rag.app.user.usecases.models.GetUserProfileOutput;
import com.rag.app.user.usecases.models.ManageUserRolesInput;
import com.rag.app.user.usecases.models.ManageUserRolesOutput;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class IntegrationTestFixtures {
    private IntegrationTestFixtures() {
    }

    public static ApplicationOrchestrator orchestrator(EventBus eventBus) {
        ModuleCoordinator coordinator = new ModuleCoordinator();
        coordinator.register("document-management", "1.0.0", () -> true);
        coordinator.register("chat-system", "1.0.0", () -> true);
        coordinator.register("user-management", "1.0.0", () -> true);
        return new ApplicationOrchestrator(new StubDocumentManagementFacade(), new StubChatSystemFacade(), new StubUserManagementFacade(), eventBus, coordinator);
    }

    public static final class StubDocumentManagementFacade implements DocumentManagementFacade {
        public final UUID documentId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        @Override
        public UploadDocumentOutput uploadDocument(UploadDocumentInput input) {
            return new UploadDocumentOutput(documentId, DocumentStatus.UPLOADED, "uploaded");
        }

        @Override
        public ProcessDocumentOutput processDocument(ProcessDocumentInput input) {
            return new ProcessDocumentOutput(input.documentId(), DocumentStatus.READY, 42, null);
        }

        @Override
        public GetUserDocumentsOutput getUserDocuments(GetUserDocumentsInput input) {
            return new GetUserDocumentsOutput(List.of(new com.rag.app.document.usecases.models.DocumentSummary(
                documentId,
                "guide.md",
                42L,
                FileType.MARKDOWN,
                DocumentStatus.READY,
                input.userId(),
                Instant.parse("2026-03-14T10:00:00Z"),
                Instant.parse("2026-03-14T10:00:00Z")
            )), 1);
        }

        @Override
        public GetAdminProgressOutput getAdminProgress(GetAdminProgressInput input) {
            return null;
        }

        @Override
        public Optional<Document> findDocumentById(String documentId) {
            return Optional.of(new Document(this.documentId, new DocumentMetadata("guide.md", 42L, FileType.MARKDOWN, "hash"), "admin-id", Instant.now(), Instant.now(), DocumentStatus.READY));
        }

        @Override
        public List<Document> findDocumentsByUser(String userId) {
            return List.of(findDocumentById(documentId.toString()).orElseThrow());
        }
    }

    public static final class StubChatSystemFacade implements ChatSystemFacade {
        public String storedVectorDocumentId;

        @Override
        public QueryDocumentsOutput queryDocuments(QueryDocumentsInput input) {
            return new QueryDocumentsOutput("Integrated answer", List.of(new DocumentReference(UUID.randomUUID(), "guide.md", "paragraph-1", 0.9d)), 10, true, null);
        }

        @Override
        public GetChatHistoryOutput getChatHistory(GetChatHistoryInput input) {
            return new GetChatHistoryOutput(List.<ChatMessage>of());
        }

        @Override
        public void storeDocumentVectors(String documentId, String content) {
            this.storedVectorDocumentId = documentId;
        }

        @Override
        public void removeDocumentVectors(String documentId) {
        }

        @Override
        public List<DocumentChunk> searchSimilarContent(String query, List<String> documentIds) {
            return List.of();
        }
    }

    public static final class StubUserManagementFacade implements UserManagementFacade {
        public final User user = new User(new UserId(UUID.fromString("22222222-2222-2222-2222-222222222222")), "admin", "admin@example.com", UserRole.ADMIN, Instant.now(), true);

        @Override
        public AuthenticationResult authenticateUser(AuthenticationRequest request) {
            return new AuthenticationResult(true, "session", user, null);
        }

        @Override
        public void invalidateSession(String sessionToken) {
        }

        @Override
        public boolean isAuthorized(UserId userId, String resource, String action) {
            return true;
        }

        @Override
        public UserRole getUserRole(UserId userId) {
            return UserRole.ADMIN;
        }

        @Override
        public GetUserProfileOutput getUserProfile(GetUserProfileInput input) {
            return null;
        }

        @Override
        public Optional<User> findUserById(UserId userId) {
            return Optional.of(user);
        }

        @Override
        public boolean isActiveUser(UserId userId) {
            return true;
        }

        @Override
        public ManageUserRolesOutput manageUserRoles(ManageUserRolesInput input) {
            return null;
        }

        @Override
        public List<User> getAllUsers() {
            return List.of(user);
        }
    }
}
