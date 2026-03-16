package com.rag.app.usecases;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.usecases.interfaces.DocumentChunkStore;
import com.rag.app.usecases.models.DocumentContentMetadata;
import com.rag.app.usecases.models.DocumentContentOutput;
import com.rag.app.usecases.models.DocumentChunk;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public final class GetDocumentContent {
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentChunkStore documentChunkStore;
    private final Map<UUID, DocumentContentOutput> cache;

    @Inject
    public GetDocumentContent(DocumentRepository documentRepository,
                              UserRepository userRepository,
                              DocumentChunkStore documentChunkStore) {
        this(documentRepository, userRepository, documentChunkStore, new ConcurrentHashMap<>());
    }

    GetDocumentContent(DocumentRepository documentRepository,
                       UserRepository userRepository,
                       DocumentChunkStore documentChunkStore,
                       Map<UUID, DocumentContentOutput> cache) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.documentChunkStore = Objects.requireNonNull(documentChunkStore, "documentChunkStore must not be null");
        this.cache = Objects.requireNonNull(cache, "cache must not be null");
    }

    public DocumentContentOutput execute(UUID documentId, UUID userId) {
        Objects.requireNonNull(documentId, "documentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user must exist"));
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new NoSuchElementException("document not found"));

        ensureAccessible(user, document);
        return cache.computeIfAbsent(documentId, ignored -> buildOutput(document));
    }

    private void ensureAccessible(User user, Document document) {
        if (user.role() == UserRole.ADMIN) {
            return;
        }
        if (!document.uploadedBy().equals(user.userId().toString())) {
            throw new SecurityException("document is not accessible for this user");
        }
    }

    private DocumentContentOutput buildOutput(Document document) {
        List<DocumentChunk> chunks = documentChunkStore.getDocumentChunks(document.documentId()).stream()
            .sorted(Comparator.comparingInt(DocumentChunk::chunkIndex))
            .toList();
        String content = chunks.stream().map(DocumentChunk::text).reduce((left, right) -> left + "\n\n" + right).orElse("");
        boolean available = document.status() == DocumentStatus.READY && !content.isBlank();

        return new DocumentContentOutput(
            document.documentId(),
            document.fileName(),
            document.fileType().name(),
            content,
            new DocumentContentMetadata(document.fileName(), document.uploadedBy(), document.uploadedAt(), document.fileSize(), chunks.size()),
            available
        );
    }
}
