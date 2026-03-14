package integration;

import com.rag.app.api.dto.AdminProgressResponse;
import com.rag.app.api.dto.UploadDocumentResponse;
import com.rag.app.domain.valueobjects.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integration")
class AdminProgressIntegrationTest {

    @Test
    void shouldReportReadyFailedAndProcessingDocumentsForAdmin() throws Exception {
        IntegrationTestSupport support = new IntegrationTestSupport();
        byte[] markdown = support.loadResource("test-documents/knowledge-base.md");
        byte[] text = support.loadResource("test-documents/operations.txt");

        UploadDocumentResponse readyUpload = support.upload(IntegrationTestSupport.STANDARD_USER_ID, "ready.md", "text/markdown", markdown);
        UploadDocumentResponse failedUpload = support.upload(IntegrationTestSupport.STANDARD_USER_ID, "failed.pdf", "application/pdf", "not-a-real-pdf".getBytes());
        UploadDocumentResponse processingUpload = support.upload(IntegrationTestSupport.OTHER_USER_ID, "processing.txt", "text/plain", text);

        support.process(UUID.fromString(readyUpload.documentId()), markdown);
        support.process(UUID.fromString(failedUpload.documentId()), "not-a-real-pdf".getBytes());
        support.updateStatus(UUID.fromString(processingUpload.documentId()), DocumentStatus.PROCESSING);

        Response response = support.adminProgress(IntegrationTestSupport.ADMIN_USER_ID);
        AdminProgressResponse entity = (AdminProgressResponse) response.getEntity();

        assertEquals(200, response.getStatus());
        assertEquals(3, entity.statistics().totalDocuments());
        assertEquals(1, entity.statistics().processingCount());
        assertEquals(1, entity.statistics().readyCount());
        assertEquals(1, entity.statistics().failedCount());
        assertEquals(1, entity.failedDocuments().size());
        assertEquals("failed.pdf", entity.failedDocuments().get(0).fileName());
        assertEquals(1, entity.processingDocuments().size());
        assertEquals("processing.txt", entity.processingDocuments().get(0).fileName());
    }
}
