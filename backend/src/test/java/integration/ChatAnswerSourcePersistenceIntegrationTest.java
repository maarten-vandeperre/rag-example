package integration;

import com.rag.app.api.AnswerSourceController;
import com.rag.app.api.ChatController;
import com.rag.app.api.dto.AnswerSourceDetailsResponse;
import com.rag.app.api.dto.ChatQueryRequest;
import com.rag.app.api.dto.ChatQueryResponse;
import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.infrastructure.document.DocumentContentExtractorImpl;
import com.rag.app.infrastructure.llm.AnswerGeneratorImpl;
import com.rag.app.infrastructure.llm.HeuristicLlmClient;
import com.rag.app.infrastructure.llm.PromptTemplate;
import com.rag.app.infrastructure.llm.ResponseValidator;
import com.rag.app.infrastructure.persistence.DocumentRowMapper;
import com.rag.app.infrastructure.persistence.JdbcAnswerPersistence;
import com.rag.app.infrastructure.persistence.JdbcAnswerSourceReferenceRepository;
import com.rag.app.infrastructure.persistence.JdbcChatMessageRepository;
import com.rag.app.infrastructure.persistence.JdbcDocumentRepository;
import com.rag.app.infrastructure.persistence.JdbcUserRepository;
import com.rag.app.infrastructure.persistence.UserRowMapper;
import com.rag.app.infrastructure.vector.EmbeddingGenerator;
import com.rag.app.infrastructure.vector.InMemoryAnswerSourceChunkStore;
import com.rag.app.infrastructure.vector.TextChunker;
import com.rag.app.infrastructure.vector.VectorStoreImpl;
import com.rag.app.usecases.GetAnswerSourceDetails;
import com.rag.app.usecases.ProcessDocument;
import com.rag.app.usecases.QueryDocuments;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class ChatAnswerSourcePersistenceIntegrationTest {

    @Test
    void shouldPersistSourceSnippetsForAnswerDetailsAcrossFreshControllerInstances() throws Exception {
        JdbcDataSource dataSource = dataSource();
        executeSchema(dataSource);

        UserRepository userRepository = new JdbcUserRepository(dataSource, new UserRowMapper());
        DocumentRepository documentRepository = new JdbcDocumentRepository(dataSource, new DocumentRowMapper());
        JdbcChatMessageRepository chatMessageRepository = new JdbcChatMessageRepository(dataSource);
        JdbcAnswerSourceReferenceRepository answerSourceRepository = new JdbcAnswerSourceReferenceRepository(dataSource);

        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        userRepository.save(new User(userId, "standard", "standard@example.com", UserRole.STANDARD,
            Instant.parse("2026-03-16T08:00:00Z"), true));

        UUID documentId = UUID.randomUUID();
        documentRepository.save(new Document(
            documentId,
            new DocumentMetadata("knowledge.md", 512L, FileType.MARKDOWN, "hash-knowledge"),
            userId.toString(),
            Instant.parse("2026-03-16T08:15:00Z"),
            DocumentStatus.UPLOADED
        ));

        VectorStoreImpl vectorStore = new VectorStoreImpl(documentRepository, new TextChunker(), new EmbeddingGenerator());
        ProcessDocument processDocument = new ProcessDocument(documentRepository, new DocumentContentExtractorImpl(), vectorStore);
        byte[] markdown = ("The system answers chat questions by retrieving the most relevant knowledge chunks. " +
            "Those chunks are then used to ground the generated answer with source references.")
            .getBytes(StandardCharsets.UTF_8);
        processDocument.execute(new com.rag.app.usecases.models.ProcessDocumentInput(documentId, markdown));

        Clock clock = Clock.fixed(Instant.parse("2026-03-16T10:00:00Z"), ZoneOffset.UTC);
        QueryDocuments queryDocuments = new QueryDocuments(
            userRepository,
            documentRepository,
            vectorStore,
            new AnswerGeneratorImpl(new PromptTemplate(), new HeuristicLlmClient(), new ResponseValidator()),
            clock
        );

        ChatController chatController = new ChatController(
            queryDocuments,
            new JdbcAnswerPersistence(dataSource, answerSourceRepository),
            new InMemoryAnswerSourceChunkStore()
        );

        Response chatResponse = chatController.query(
            new ChatQueryRequest("How does the system answer chat questions?", 20_000),
            userId.toString(),
            securityContext(userId)
        );

        ChatQueryResponse chatEntity = (ChatQueryResponse) chatResponse.getEntity();
        assertEquals(200, chatResponse.getStatus());
        assertTrue(chatEntity.success());
        assertNotNull(chatEntity.answerId());

        AnswerSourceController answerSourceController = new AnswerSourceController(new GetAnswerSourceDetails(
            chatMessageRepository,
            documentRepository,
            userRepository,
            answerSourceRepository,
            new InMemoryAnswerSourceChunkStore(),
            vectorStore
        ));

        Response sourceResponse = answerSourceController.getAnswerSources(chatEntity.answerId(), userId.toString(), securityContext(userId));

        assertEquals(200, sourceResponse.getStatus());
        AnswerSourceDetailsResponse sourceEntity = (AnswerSourceDetailsResponse) sourceResponse.getEntity();
        assertFalse(sourceEntity.sources().isEmpty());
        assertNotNull(sourceEntity.sources().get(0).snippet());
        assertTrue(sourceEntity.sources().get(0).snippet().content().contains("retrieving the most relevant knowledge chunks"));
    }

    private JdbcDataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:chat-answer-source-persistence;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        return dataSource;
    }

    private void executeSchema(JdbcDataSource dataSource) throws SQLException, IOException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            String schemaSql = new String(
                ChatAnswerSourcePersistenceIntegrationTest.class.getResourceAsStream("/schema.sql").readAllBytes(),
                StandardCharsets.UTF_8
            );
            for (String sqlStatement : schemaSql.split(";")) {
                String trimmedStatement = sqlStatement.trim();
                if (!trimmedStatement.isEmpty()) {
                    statement.execute(trimmedStatement);
                }
            }
        }
    }

    private SecurityContext securityContext(UUID userId) {
        return new SecurityContext() {
            @Override
            public java.security.Principal getUserPrincipal() {
                return userId::toString;
            }

            @Override
            public boolean isUserInRole(String role) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public String getAuthenticationScheme() {
                return "test";
            }
        };
    }
}
