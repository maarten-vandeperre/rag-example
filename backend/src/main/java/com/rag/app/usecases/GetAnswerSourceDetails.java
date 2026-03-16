package com.rag.app.usecases;

import com.rag.app.domain.entities.ChatMessage;
import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.AnswerSourceReference;
import com.rag.app.domain.valueobjects.DocumentReference;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.SourceSnippet;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.usecases.interfaces.AnswerSourceChunkStore;
import com.rag.app.usecases.interfaces.DocumentChunkStore;
import com.rag.app.usecases.models.AnswerSourceDetail;
import com.rag.app.usecases.models.AnswerSourceDetailsOutput;
import com.rag.app.usecases.models.AnswerSourceMetadata;
import com.rag.app.usecases.models.DocumentChunk;
import com.rag.app.usecases.repositories.ChatMessageRepository;
import com.rag.app.usecases.repositories.AnswerSourceReferenceRepository;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public final class GetAnswerSourceDetails {
    private static final Logger LOG = Logger.getLogger(GetAnswerSourceDetails.class);
    private final ChatMessageRepository chatMessageRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentChunkStore documentChunkStore;
    private final AnswerSourceChunkStore answerSourceChunkStore;
    private final AnswerSourceReferenceRepository answerSourceReferenceRepository;
    private final Map<UUID, AnswerSourceDetailsOutput> cache;

    @Inject
    public GetAnswerSourceDetails(ChatMessageRepository chatMessageRepository,
                                  DocumentRepository documentRepository,
                                  UserRepository userRepository,
                                  AnswerSourceReferenceRepository answerSourceReferenceRepository,
                                  AnswerSourceChunkStore answerSourceChunkStore,
                                  DocumentChunkStore documentChunkStore) {
        this(chatMessageRepository, documentRepository, userRepository, answerSourceReferenceRepository, answerSourceChunkStore, documentChunkStore, new ConcurrentHashMap<>());
    }

    GetAnswerSourceDetails(ChatMessageRepository chatMessageRepository,
                           DocumentRepository documentRepository,
                           UserRepository userRepository,
                           AnswerSourceReferenceRepository answerSourceReferenceRepository,
                           AnswerSourceChunkStore answerSourceChunkStore,
                           DocumentChunkStore documentChunkStore,
                           Map<UUID, AnswerSourceDetailsOutput> cache) {
        this.chatMessageRepository = Objects.requireNonNull(chatMessageRepository, "chatMessageRepository must not be null");
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.answerSourceReferenceRepository = Objects.requireNonNull(answerSourceReferenceRepository, "answerSourceReferenceRepository must not be null");
        this.answerSourceChunkStore = Objects.requireNonNull(answerSourceChunkStore, "answerSourceChunkStore must not be null");
        this.documentChunkStore = Objects.requireNonNull(documentChunkStore, "documentChunkStore must not be null");
        this.cache = Objects.requireNonNull(cache, "cache must not be null");
    }

    public AnswerSourceDetailsOutput execute(UUID answerId, UUID userId) {
        Objects.requireNonNull(answerId, "answerId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user must exist"));
        ChatMessage message = chatMessageRepository.findById(answerId)
            .orElseThrow(() -> new NoSuchElementException("answer not found"));

        ensureAccessible(user, message);
        return cache.computeIfAbsent(answerId, ignored -> buildOutput(message));
    }

    private void ensureAccessible(User user, ChatMessage message) {
        if (user.role() == UserRole.ADMIN) {
            return;
        }
        if (!message.userId().equals(user.userId())) {
            throw new SecurityException("answer is not accessible for this user");
        }
    }

    private AnswerSourceDetailsOutput buildOutput(ChatMessage message) {
        List<AnswerSourceReference> persistedSources = answerSourceReferenceRepository.findByAnswerId(message.messageId());
        if (!persistedSources.isEmpty()) {
            LOG.debugf("Loaded %d persisted source references for answer %s", persistedSources.size(), message.messageId());
            return buildOutputFromPersistedSources(message, persistedSources);
        }

        List<AnswerSourceDetail> sources = new ArrayList<>();
        List<DocumentChunk> answerChunks = answerSourceChunkStore.getChunks(message.messageId());

        LOG.debugf("Resolving source details for answer %s with %d answer-scoped chunks and %d references",
            message.messageId(), answerChunks.size(), message.documentReferences().size());

        for (int index = 0; index < message.documentReferences().size(); index++) {
            DocumentReference reference = message.documentReferences().get(index);
            sources.add(buildSourceDetail(message.messageId(), index, reference, answerChunks));
        }

        int availableSources = (int) sources.stream().filter(AnswerSourceDetail::available).count();
        return new AnswerSourceDetailsOutput(message.messageId(), sources, sources.size(), availableSources);
    }

    private AnswerSourceDetailsOutput buildOutputFromPersistedSources(ChatMessage message,
                                                                      List<AnswerSourceReference> persistedSources) {
        List<AnswerSourceDetail> sources = persistedSources.stream()
            .map(this::toAnswerSourceDetail)
            .toList();

        int availableSources = (int) sources.stream().filter(AnswerSourceDetail::available).count();
        return new AnswerSourceDetailsOutput(message.messageId(), sources, sources.size(), availableSources);
    }

    private AnswerSourceDetail toAnswerSourceDetail(AnswerSourceReference sourceReference) {
        Document document = documentRepository.findById(sourceReference.documentId()).orElse(null);
        boolean snippetAvailable = sourceReference.snippetContent() != null && !sourceReference.snippetContent().isBlank();

        String fileName = sourceReference.documentFilename() != null && !sourceReference.documentFilename().isBlank()
            ? sourceReference.documentFilename()
            : document == null ? "Unknown source" : document.fileName();
        String fileType = sourceReference.documentFileType() != null && !sourceReference.documentFileType().isBlank()
            ? sourceReference.documentFileType()
            : document == null ? "UNKNOWN" : document.fileType().name();
        String title = sourceReference.documentTitle() != null && !sourceReference.documentTitle().isBlank()
            ? sourceReference.documentTitle()
            : fileName;

        SourceSnippet snippet = snippetAvailable
            ? new SourceSnippet(
                sourceReference.snippetContent(),
                sourceReference.startPosition() == null ? 0 : sourceReference.startPosition(),
                sourceReference.endPosition() == null ? sourceReference.snippetContent().length() : sourceReference.endPosition(),
                sourceReference.snippetContext() == null ? sourceReference.snippetContent() : sourceReference.snippetContext()
            )
            : null;

        return new AnswerSourceDetail(
            sourceReference.chunkId(),
            sourceReference.documentId(),
            fileName,
            fileType,
            snippet,
            new AnswerSourceMetadata(
                title,
                document == null ? null : document.uploadedBy(),
                document == null ? sourceReference.createdAt() : document.uploadedAt(),
                sourceReference.pageNumber(),
                sourceReference.chunkIndex()
            ),
            sourceReference.relevanceScore(),
            snippetAvailable
        );
    }

    private AnswerSourceDetail buildSourceDetail(UUID answerId,
                                                 int index,
                                                 DocumentReference reference,
                                                 List<DocumentChunk> answerChunks) {
        Document document = documentRepository.findById(reference.documentId()).orElse(null);
        if (document == null) {
            LOG.warnf("Document %s for answer %s source %d is missing", reference.documentId(), answerId, index);
            return new AnswerSourceDetail(
                answerId + "-source-" + index,
                reference.documentId(),
                reference.documentName(),
                null,
                null,
                new AnswerSourceMetadata(reference.documentName(), null, null, null, null),
                reference.relevanceScore(),
                false
            );
        }

        List<DocumentChunk> scopedChunks = answerChunks.stream()
            .filter(chunk -> reference.documentId().equals(chunk.documentId()))
            .sorted(Comparator.comparingInt(DocumentChunk::chunkIndex))
            .toList();
        List<DocumentChunk> documentChunks = sortedChunks(reference.documentId());

        List<DocumentChunk> preferredChunks = scopedChunks.isEmpty() ? documentChunks : scopedChunks;
        DocumentChunk matchedChunk = resolveChunk(reference, preferredChunks)
            .orElseGet(() -> resolveChunk(reference, documentChunks).orElseGet(() -> fallbackChunk(preferredChunks, documentChunks)));

        boolean available = document.status() == DocumentStatus.READY && matchedChunk != null;

        if (matchedChunk == null) {
            LOG.warnf("No chunk could be resolved for answer %s source %d (%s / %s)",
                answerId,
                index,
                reference.documentName(),
                reference.paragraphReference());
        } else {
            LOG.debugf("Resolved chunk %s for answer %s source %d using reference %s",
                matchedChunk.chunkId(),
                answerId,
                index,
                reference.paragraphReference());
        }

        return new AnswerSourceDetail(
            matchedChunk == null ? answerId + "-source-" + index : matchedChunk.chunkId(),
            reference.documentId(),
            document.fileName(),
            document.fileType().name(),
            available ? createSnippet(contextChunks(preferredChunks, documentChunks), matchedChunk) : null,
            new AnswerSourceMetadata(document.fileName(), document.uploadedBy(), document.uploadedAt(), null,
                matchedChunk == null ? null : matchedChunk.chunkIndex()),
            reference.relevanceScore(),
            available
        );
    }

    private List<DocumentChunk> contextChunks(List<DocumentChunk> preferredChunks, List<DocumentChunk> fallbackChunks) {
        return preferredChunks.isEmpty() ? fallbackChunks : preferredChunks;
    }

    private List<DocumentChunk> sortedChunks(UUID documentId) {
        return documentChunkStore.getDocumentChunks(documentId).stream()
            .sorted(Comparator.comparingInt(DocumentChunk::chunkIndex))
            .toList();
    }

    private java.util.Optional<DocumentChunk> resolveChunk(DocumentReference reference, List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return java.util.Optional.empty();
        }

        java.util.Optional<DocumentChunk> exactMatch = chunks.stream()
            .filter(chunk -> reference.paragraphReference() != null
                && (reference.paragraphReference().equalsIgnoreCase(chunk.paragraphReference())
                || reference.paragraphReference().equalsIgnoreCase(chunk.chunkId())))
            .findFirst();
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        OptionalInt paragraphIndex = parseChunkIndex(reference.paragraphReference());
        if (paragraphIndex.isPresent()) {
            int zeroBasedIndex = Math.max(0, paragraphIndex.getAsInt() - 1);
            java.util.Optional<DocumentChunk> indexedMatch = chunks.stream()
                .filter(chunk -> chunk.chunkIndex() == zeroBasedIndex)
                .findFirst();
            if (indexedMatch.isPresent()) {
                return indexedMatch;
            }
        }

        return java.util.Optional.empty();
    }

    private OptionalInt parseChunkIndex(String referenceLabel) {
        if (referenceLabel == null || referenceLabel.isBlank()) {
            return OptionalInt.empty();
        }

        StringBuilder trailingDigits = new StringBuilder();
        for (int index = 0; index < referenceLabel.length(); index++) {
            char character = referenceLabel.charAt(index);
            if (Character.isDigit(character)) {
                trailingDigits.append(character);
            }
        }

        if (trailingDigits.isEmpty()) {
            return OptionalInt.empty();
        }

        try {
            return OptionalInt.of(Integer.parseInt(trailingDigits.toString()));
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }

    private DocumentChunk fallbackChunk(List<DocumentChunk> preferredChunks, List<DocumentChunk> fallbackChunks) {
        if (!preferredChunks.isEmpty()) {
            return preferredChunks.stream()
                .sorted(Comparator.comparingDouble(DocumentChunk::relevanceScore).reversed()
                    .thenComparingInt(DocumentChunk::chunkIndex))
                .findFirst()
                .orElse(null);
        }

        return fallbackChunks.isEmpty() ? null : fallbackChunks.get(0);
    }

    private SourceSnippet createSnippet(List<DocumentChunk> sortedChunks, DocumentChunk matchedChunk) {
        int index = sortedChunks.indexOf(matchedChunk);
        String fullContent = joinChunkTexts(sortedChunks);
        int startPosition = 0;
        for (int current = 0; current < index; current++) {
            startPosition += sortedChunks.get(current).text().length();
            startPosition += 2;
        }
        int endPosition = startPosition + matchedChunk.text().length();

        StringBuilder context = new StringBuilder();
        if (index > 0) {
            context.append(sortedChunks.get(index - 1).text()).append("\n\n");
        }
        context.append(matchedChunk.text());
        if (index < sortedChunks.size() - 1) {
            context.append("\n\n").append(sortedChunks.get(index + 1).text());
        }

        int boundedEnd = Math.min(endPosition, fullContent.length());
        return new SourceSnippet(matchedChunk.text(), startPosition, boundedEnd, context.toString());
    }

    private String joinChunkTexts(List<DocumentChunk> chunks) {
        return chunks.stream().map(DocumentChunk::text).reduce((left, right) -> left + "\n\n" + right).orElse("");
    }
}
