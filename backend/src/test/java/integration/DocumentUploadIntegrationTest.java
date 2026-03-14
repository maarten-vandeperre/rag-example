package integration;

import com.rag.app.api.dto.DocumentListResponse;
import com.rag.app.api.dto.UploadDocumentResponse;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.usecases.models.ProcessDocumentOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class DocumentUploadIntegrationTest {

    @Test
    void shouldUploadProcessAndSearchPdfMarkdownAndTextDocuments() throws Exception {
        IntegrationTestSupport support = new IntegrationTestSupport();

        UploadDocumentResponse pdfUpload = support.upload(
            IntegrationTestSupport.STANDARD_USER_ID,
            "guide.pdf",
            "application/pdf",
            support.createPdf("Uploads are processed asynchronously and searchable for chat answers.")
        );
        UploadDocumentResponse markdownUpload = support.upload(
            IntegrationTestSupport.STANDARD_USER_ID,
            "guide.md",
            "text/markdown",
            support.loadResource("test-documents/knowledge-base.md")
        );
        UploadDocumentResponse textUpload = support.upload(
            IntegrationTestSupport.STANDARD_USER_ID,
            "guide.txt",
            "text/plain",
            support.loadResource("test-documents/operations.txt")
        );

        Response uploadedResponse = support.documents(IntegrationTestSupport.STANDARD_USER_ID, false);
        DocumentListResponse uploadedDocuments = (DocumentListResponse) uploadedResponse.getEntity();

        assertEquals(3, uploadedDocuments.totalCount());
        assertTrue(uploadedDocuments.documents().stream().allMatch(document -> "UPLOADED".equals(document.status())));

        ProcessDocumentOutput pdfProcessed = support.process(UUID.fromString(pdfUpload.documentId()), support.createPdf("Uploads are processed asynchronously and searchable for chat answers."));
        ProcessDocumentOutput markdownProcessed = support.process(UUID.fromString(markdownUpload.documentId()), support.loadResource("test-documents/knowledge-base.md"));
        ProcessDocumentOutput textProcessed = support.process(UUID.fromString(textUpload.documentId()), support.loadResource("test-documents/operations.txt"));

        assertEquals(DocumentStatus.READY, pdfProcessed.finalStatus());
        assertEquals(DocumentStatus.READY, markdownProcessed.finalStatus());
        assertEquals(DocumentStatus.READY, textProcessed.finalStatus());

        Response readyResponse = support.documents(IntegrationTestSupport.STANDARD_USER_ID, false);
        DocumentListResponse readyDocuments = (DocumentListResponse) readyResponse.getEntity();

        assertTrue(readyDocuments.documents().stream().allMatch(document -> "READY".equals(document.status())));
        Response chatResponse = support.chat(IntegrationTestSupport.STANDARD_USER_ID, "How are uploads processed?", 20_000);
        assertEquals(200, chatResponse.getStatus());
    }
}
