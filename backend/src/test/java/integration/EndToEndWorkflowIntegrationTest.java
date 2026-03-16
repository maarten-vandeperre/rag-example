package integration;

import com.rag.app.api.dto.ChatQueryResponse;
import com.rag.app.api.dto.UploadDocumentResponse;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.integration.api.dto.ApiResponse;
import com.rag.app.integration.api.dto.knowledge.KnowledgeGraphDto;
import com.rag.app.integration.api.dto.knowledge.KnowledgeGraphStatisticsDto;
import com.rag.app.integration.api.dto.knowledge.KnowledgeGraphSummaryDto;
import com.rag.app.integration.api.dto.knowledge.KnowledgeSearchResultDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class EndToEndWorkflowIntegrationTest {

    @Test
    void shouldCompleteChatAndKnowledgeGraphWorkflowsForUploadedDocument() throws Exception {
        IntegrationTestSupport support = new IntegrationTestSupport();
        byte[] content = """
            Ada Lovelace works for Analytical Engine Labs in London.
            Analytical Engine Labs operates in London and builds a knowledge graph pipeline.
            Semantic search retrieves relevant document chunks so chat answers stay grounded.
            """.getBytes(StandardCharsets.UTF_8);

        UploadDocumentResponse upload = support.upload(
            IntegrationTestSupport.STANDARD_USER_ID,
            "workflow.txt",
            "text/plain",
            content
        );

        UUID documentId = UUID.fromString(upload.documentId());
        assertEquals(DocumentStatus.READY, support.documentRepository.findById(documentId).orElseThrow().status());

        Response chatResponse = support.chat(IntegrationTestSupport.STANDARD_USER_ID, "How does semantic search retrieve document chunks?", 20_000);
        ChatQueryResponse chatEntity = (ChatQueryResponse) chatResponse.getEntity();

        assertEquals(200, chatResponse.getStatus());
        assertTrue(chatEntity.success());
        assertFalse(chatEntity.answer().isBlank());
        assertTrue(chatEntity.documentReferences().stream().anyMatch(reference -> "workflow.txt".equals(reference.documentName())));

        Response listResponse = support.knowledgeGraphs(IntegrationTestSupport.ADMIN_USER_ID);
        @SuppressWarnings("unchecked")
        ApiResponse<List<KnowledgeGraphSummaryDto>> listEntity = (ApiResponse<List<KnowledgeGraphSummaryDto>>) listResponse.getEntity();

        assertEquals(200, listResponse.getStatus());
        assertTrue(listEntity.success());
        assertEquals(1, listEntity.data().size());
        String graphId = listEntity.data().get(0).graphId();

        Response graphResponse = support.knowledgeGraph(IntegrationTestSupport.ADMIN_USER_ID, graphId);
        @SuppressWarnings("unchecked")
        ApiResponse<KnowledgeGraphDto> graphEntity = (ApiResponse<KnowledgeGraphDto>) graphResponse.getEntity();

        assertEquals(200, graphResponse.getStatus());
        assertTrue(graphEntity.success());
        assertNotNull(graphEntity.data());
        assertTrue(graphEntity.data().nodes().stream().anyMatch(node -> node.label().contains("Ada Lovelace")));
        assertTrue(graphEntity.data().nodes().stream().anyMatch(node -> node.label().contains("Analytical Engine Labs")));

        Response searchResponse = support.knowledgeGraphSearch(IntegrationTestSupport.ADMIN_USER_ID, "Ada", graphId);
        @SuppressWarnings("unchecked")
        ApiResponse<KnowledgeSearchResultDto> searchEntity = (ApiResponse<KnowledgeSearchResultDto>) searchResponse.getEntity();
        assertEquals(200, searchResponse.getStatus());
        assertTrue(searchEntity.success());
        assertTrue(searchEntity.data().totalResults() > 0);

        Response statsResponse = support.knowledgeGraphStatistics(IntegrationTestSupport.ADMIN_USER_ID, graphId);
        @SuppressWarnings("unchecked")
        ApiResponse<KnowledgeGraphStatisticsDto> statsEntity = (ApiResponse<KnowledgeGraphStatisticsDto>) statsResponse.getEntity();
        assertEquals(200, statsResponse.getStatus());
        assertTrue(statsEntity.success());
        assertEquals(1, statsEntity.data().totalGraphs());
        assertTrue(statsEntity.data().totalNodes() > 0);
    }

    @Test
    void shouldReturnHelpfulErrorsForMissingChatContextAndForbiddenKnowledgeGraphAccess() {
        IntegrationTestSupport support = new IntegrationTestSupport();

        Response chatResponse = support.chat(IntegrationTestSupport.STANDARD_USER_ID, "What documents are available?", 20_000);
        ChatQueryResponse chatEntity = (ChatQueryResponse) chatResponse.getEntity();
        assertEquals(404, chatResponse.getStatus());
        assertEquals("no answer found", chatEntity.errorMessage());

        Response forbiddenGraphResponse = support.knowledgeGraphs(IntegrationTestSupport.STANDARD_USER_ID);
        @SuppressWarnings("unchecked")
        ApiResponse<List<KnowledgeGraphSummaryDto>> forbiddenGraphEntity = (ApiResponse<List<KnowledgeGraphSummaryDto>>) forbiddenGraphResponse.getEntity();
        assertEquals(403, forbiddenGraphResponse.getStatus());
        assertFalse(forbiddenGraphEntity.success());
        assertEquals("ADMIN_ACCESS_REQUIRED", forbiddenGraphEntity.error().code());
    }
}
