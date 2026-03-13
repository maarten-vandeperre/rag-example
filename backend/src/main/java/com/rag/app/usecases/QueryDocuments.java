package com.rag.app.usecases;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.usecases.interfaces.AnswerGenerator;
import com.rag.app.usecases.interfaces.SemanticSearch;
import com.rag.app.usecases.models.DocumentChunk;
import com.rag.app.usecases.models.QueryDocumentsInput;
import com.rag.app.usecases.models.QueryDocumentsOutput;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class QueryDocuments {
    private static final String NO_DOCUMENTS_MESSAGE = "No ready documents available for this query";
    private static final String NO_MATCHES_MESSAGE = "No relevant documents found for the question";

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final SemanticSearch semanticSearch;
    private final AnswerGenerator answerGenerator;
    private final Clock clock;

    public QueryDocuments(UserRepository userRepository,
                          DocumentRepository documentRepository,
                          SemanticSearch semanticSearch,
                          AnswerGenerator answerGenerator,
                          Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.semanticSearch = Objects.requireNonNull(semanticSearch, "semanticSearch must not be null");
        this.answerGenerator = Objects.requireNonNull(answerGenerator, "answerGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public QueryDocumentsOutput execute(QueryDocumentsInput input) {
        Objects.requireNonNull(input, "input must not be null");

        if (input.userId() == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (input.question() == null || input.question().isBlank()) {
            throw new IllegalArgumentException("question must not be null or empty");
        }
        if (input.maxResponseTimeMs() <= 0) {
            throw new IllegalArgumentException("maxResponseTimeMs must be positive");
        }

        long startedAt = clock.millis();
        User user = userRepository.findById(input.userId())
            .orElseThrow(() -> new IllegalArgumentException("user must exist"));

        List<Document> accessibleDocuments = resolveAccessibleDocuments(user);
        if (accessibleDocuments.isEmpty()) {
            return failure(NO_DOCUMENTS_MESSAGE, elapsed(startedAt));
        }

        List<DocumentChunk> chunks = semanticSearch.searchDocuments(
            input.question(),
            accessibleDocuments.stream().map(document -> document.documentId().toString()).toList()
        );

        if (chunks.isEmpty()) {
            return failure(NO_MATCHES_MESSAGE, elapsed(startedAt));
        }

        AnswerGenerator.GeneratedAnswer generatedAnswer = answerGenerator.generateAnswer(input.question(), chunks);
        int responseTimeMs = elapsed(startedAt);

        if (responseTimeMs > input.maxResponseTimeMs()) {
            return failure("Response time exceeded limit", responseTimeMs);
        }

        return new QueryDocumentsOutput(
            generatedAnswer.answer(),
            List.copyOf(generatedAnswer.documentReferences()),
            responseTimeMs,
            true,
            null
        );
    }

    private List<Document> resolveAccessibleDocuments(User user) {
        if (user.role() == UserRole.ADMIN) {
            return documentRepository.findByStatus(DocumentStatus.READY);
        }

        return documentRepository.findByUploadedBy(user.userId().toString()).stream()
            .filter(document -> document.status() == DocumentStatus.READY)
            .toList();
    }

    private QueryDocumentsOutput failure(String errorMessage, int responseTimeMs) {
        return new QueryDocumentsOutput(null, List.of(), responseTimeMs, false, errorMessage);
    }

    private int elapsed(long startedAt) {
        return (int) (clock.millis() - startedAt);
    }
}
