package integration;

import com.rag.app.api.dto.ChatQueryResponse;
import com.rag.app.api.dto.UploadDocumentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class ChatQueryIntegrationTest {

    @Test
    void shouldReturnGroundedAnswerWithSourceReferencesWithinTimeout() throws Exception {
        IntegrationTestSupport support = new IntegrationTestSupport();
        byte[] markdown = support.loadResource("test-documents/knowledge-base.md");
        UploadDocumentResponse upload = support.upload(IntegrationTestSupport.STANDARD_USER_ID, "knowledge.md", "text/markdown", markdown);
        support.process(UUID.fromString(upload.documentId()), markdown);

        Response response = support.chat(IntegrationTestSupport.STANDARD_USER_ID, "How does the system answer chat questions?", 20_000);
        ChatQueryResponse entity = (ChatQueryResponse) response.getEntity();

        assertEquals(200, response.getStatus());
        assertTrue(entity.success());
        assertTrue(entity.answer().contains("knowledge.md"));
        assertTrue(entity.documentReferences().stream().anyMatch(reference -> "knowledge.md".equals(reference.documentName())));
        assertTrue(entity.responseTimeMs() < 20_000);
    }
}
