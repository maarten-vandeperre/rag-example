package com.rag.app.api;

import com.rag.app.api.dto.ChatQueryRequest;
import com.rag.app.api.dto.ChatQueryResponse;
import com.rag.app.api.dto.DocumentReferenceDto;
import com.rag.app.domain.entities.ChatMessage;
import com.rag.app.domain.valueobjects.AnswerSourceReference;
import com.rag.app.domain.valueobjects.DocumentReference;
import com.rag.app.infrastructure.persistence.JdbcAnswerPersistence;
import com.rag.app.usecases.QueryDocuments;
import com.rag.app.usecases.interfaces.AnswerSourceChunkStore;
import com.rag.app.usecases.interfaces.AnswerPersistence;
import com.rag.app.infrastructure.vector.InMemoryAnswerSourceChunkStore;
import com.rag.app.usecases.models.QueryDocumentsInput;
import com.rag.app.usecases.models.QueryDocumentsOutput;
import com.rag.app.usecases.repositories.ChatMessageRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.logging.Logger;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Path("/api/chat")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ChatController {
    private static final Logger LOG = Logger.getLogger(ChatController.class);
    private static final String NO_ANSWER_FOUND_MESSAGE = "no answer found";
    private static final String QUERY_TIMEOUT_MESSAGE = "Query exceeded the allowed response time";
    private static final String GENERIC_ERROR_MESSAGE = "Unable to process chat query";

    private final QueryDocuments queryDocuments;
    private final Executor executor;
    private final AnswerPersistence answerPersistence;
    private final AnswerSourceChunkStore answerSourceChunkStore;

    @Inject
    public ChatController(QueryDocuments queryDocuments,
                          JdbcAnswerPersistence answerPersistence,
                          AnswerSourceChunkStore answerSourceChunkStore) {
        this(queryDocuments, createDaemonExecutor(), answerPersistence, answerSourceChunkStore);
    }

    public ChatController(QueryDocuments queryDocuments) {
        this(queryDocuments, createDaemonExecutor(), defaultInMemoryPersistence(), new InMemoryAnswerSourceChunkStore());
    }

    ChatController(QueryDocuments queryDocuments, Executor executor) {
        this(queryDocuments, executor, defaultInMemoryPersistence(), new InMemoryAnswerSourceChunkStore());
    }

    ChatController(QueryDocuments queryDocuments, Executor executor, ChatMessageRepository chatMessageRepository) {
        this(queryDocuments, executor, new InMemoryAnswerPersistence(chatMessageRepository), new InMemoryAnswerSourceChunkStore());
    }

    ChatController(QueryDocuments queryDocuments,
                   Executor executor,
                   AnswerPersistence answerPersistence,
                   AnswerSourceChunkStore answerSourceChunkStore) {
        this.queryDocuments = Objects.requireNonNull(queryDocuments, "queryDocuments must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.answerPersistence = Objects.requireNonNull(answerPersistence, "answerPersistence must not be null");
        this.answerSourceChunkStore = Objects.requireNonNull(answerSourceChunkStore, "answerSourceChunkStore must not be null");
    }

    @POST
    @Path("/query")
    public Response query(ChatQueryRequest request, 
                         @HeaderParam("X-User-Id") String userIdHeader,
                         @Context SecurityContext securityContext) {
        try {
            UUID userId = extractUserId(userIdHeader, securityContext);
            ChatQueryRequest validatedRequest = validateRequest(request);
            int timeoutMs = validatedRequest.resolvedMaxResponseTimeMs();

            QueryDocumentsOutput output = CompletableFuture
                .supplyAsync(() -> queryDocuments.execute(new QueryDocumentsInput(userId, validatedRequest.question(), timeoutMs)), executor)
                .get(timeoutMs, TimeUnit.MILLISECONDS);

            return mapOutput(userId, validatedRequest.question(), output);
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        } catch (TimeoutException exception) {
            return Response.status(Response.Status.REQUEST_TIMEOUT)
                .entity(errorResponse(QUERY_TIMEOUT_MESSAGE, ChatQueryRequest.DEFAULT_MAX_RESPONSE_TIME_MS))
                .build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return internalError();
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IllegalArgumentException illegalArgumentException) {
                return badRequest(illegalArgumentException.getMessage());
            }
            return internalError();
        } catch (RuntimeException exception) {
            return internalError();
        }
    }

    private ChatQueryRequest validateRequest(ChatQueryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body must not be null");
        }
        if (request.question() == null || request.question().isBlank()) {
            throw new IllegalArgumentException("question must not be null or empty");
        }
        if (request.maxResponseTimeMs() != null && request.maxResponseTimeMs() <= 0) {
            throw new IllegalArgumentException("maxResponseTimeMs must be positive");
        }
        return request;
    }

    private UUID extractUserId(String userIdHeader, SecurityContext securityContext) {
        // In development mode, use X-User-Id header if available
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                return UUID.fromString(userIdHeader);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("X-User-Id header must be a valid UUID", exception);
            }
        }

        // Fall back to SecurityContext for production mode
        if (securityContext == null) {
            throw new IllegalArgumentException("authenticated user is required");
        }

        Principal principal = securityContext.getUserPrincipal();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalArgumentException("authenticated user is required");
        }

        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("authenticated user id must be a valid UUID", exception);
        }
    }

    private Response mapOutput(UUID userId, String question, QueryDocumentsOutput output) {
        if (output.success()) {
            UUID answerId = UUID.randomUUID();
            ChatMessage message = new ChatMessage(
                answerId,
                userId,
                question,
                output.answer(),
                output.documentReferences(),
                java.time.Instant.now(),
                Math.max(1, output.responseTimeMs())
            );

            try {
                answerPersistence.persist(message, sourceReferences(answerId, output));
                answerSourceChunkStore.store(answerId, output.sourceChunks());
            } catch (RuntimeException exception) {
                LOG.errorf(exception,
                    "Failed to persist chat answer %s for user %s. Returning internal error instead of incomplete answer.",
                    message.messageId(),
                    message.userId());
                return internalError();
            }

            return Response.ok(new ChatQueryResponse(
                answerId.toString(),
                output.answer(),
                output.documentReferences().stream().map(this::toDto).toList(),
                output.responseTimeMs(),
                true,
                null
            )).build();
        }

        if ("Response time exceeded limit".equals(output.errorMessage())) {
            return Response.status(Response.Status.REQUEST_TIMEOUT)
                .entity(errorResponse(QUERY_TIMEOUT_MESSAGE, output.responseTimeMs()))
                .build();
        }

        if ("No relevant documents found for the question".equals(output.errorMessage())
            || "No ready documents available for this query".equals(output.errorMessage())) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(errorResponse(NO_ANSWER_FOUND_MESSAGE, output.responseTimeMs()))
                .build();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(errorResponse(GENERIC_ERROR_MESSAGE, output.responseTimeMs()))
            .build();
    }

    private List<AnswerSourceReference> sourceReferences(UUID answerId, QueryDocumentsOutput output) {
        List<com.rag.app.usecases.models.DocumentChunk> chunks = output.sourceChunks();
        java.util.ArrayList<AnswerSourceReference> references = new java.util.ArrayList<>(chunks.size());
        for (int index = 0; index < chunks.size(); index++) {
            references.add(AnswerSourceReference.fromChunk(answerId, chunks.get(index), index));
        }
        return List.copyOf(references);
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(errorResponse(message, 0))
            .build();
    }

    private Response internalError() {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(errorResponse(GENERIC_ERROR_MESSAGE, 0))
            .build();
    }

    private ChatQueryResponse errorResponse(String message, int responseTimeMs) {
        return new ChatQueryResponse(null, null, List.of(), responseTimeMs, false, message);
    }

    private DocumentReferenceDto toDto(DocumentReference reference) {
        return new DocumentReferenceDto(
            reference.documentId().toString(),
            reference.documentName(),
            reference.paragraphReference(),
            reference.relevanceScore()
        );
    }

    private static Executor createDaemonExecutor() {
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "chat-query-controller");
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newCachedThreadPool(threadFactory);
    }

    private static InMemoryAnswerPersistence defaultInMemoryPersistence() {
        return new InMemoryAnswerPersistence(new InMemoryChatMessageRepository());
    }

    private static final class InMemoryChatMessageRepository implements ChatMessageRepository {
        private final java.util.Map<UUID, ChatMessage> messages = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public ChatMessage save(ChatMessage message) {
            messages.put(message.messageId(), message);
            return message;
        }

        @Override
        public java.util.Optional<ChatMessage> findById(UUID messageId) {
            return java.util.Optional.ofNullable(messages.get(messageId));
        }

        @Override
        public List<ChatMessage> findByUserId(UUID userId) {
            return messages.values().stream().filter(message -> message.userId().equals(userId)).toList();
        }

        @Override
        public List<ChatMessage> findRecentByUserId(UUID userId, int limit) {
            return findByUserId(userId).stream()
                .sorted(java.util.Comparator.comparing(ChatMessage::createdAt).reversed())
                .limit(limit)
                .toList();
        }
    }

    private static final class InMemoryAnswerPersistence implements AnswerPersistence {
        private final ChatMessageRepository chatMessageRepository;

        private InMemoryAnswerPersistence(ChatMessageRepository chatMessageRepository) {
            this.chatMessageRepository = chatMessageRepository;
        }

        @Override
        public void persist(ChatMessage message, List<AnswerSourceReference> sourceReferences) {
            chatMessageRepository.save(message);
        }
    }
}
