package integration;

import com.rag.app.api.dto.ChatQueryResponse;
import com.rag.app.api.dto.ErrorResponse;
import com.rag.app.api.dto.UploadDocumentResponse;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.usecases.models.ProcessDocumentOutput;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ErrorScenarioIntegrationTest {

    @Test
    void shouldRejectOversizedUnsupportedAndCorruptedInputs() throws Exception {
        IntegrationTestSupport support = new IntegrationTestSupport();
        Path oversized = Files.createTempFile("oversized-", ".pdf");
        try {
            try (RandomAccessFile file = new RandomAccessFile(oversized.toFile(), "rw")) {
                file.setLength(41_943_041L);
            }

            Response oversizedResponse = support.uploadResponse(IntegrationTestSupport.STANDARD_USER_ID, "large.pdf", "application/pdf", Files.readAllBytes(oversized));
            Response unsupportedResponse = support.uploadResponse(IntegrationTestSupport.STANDARD_USER_ID, "large.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx".getBytes());

            UploadDocumentResponse upload = support.upload(IntegrationTestSupport.STANDARD_USER_ID, "corrupted.pdf", "application/pdf", "not-a-real-pdf".getBytes());
            ProcessDocumentOutput failedProcessing = support.process(UUID.fromString(upload.documentId()), "not-a-real-pdf".getBytes());
            Response noMatchResponse = support.chat(IntegrationTestSupport.STANDARD_USER_ID, "What is the telescope orbit?", 20_000);

            ErrorResponse oversizedEntity = assertInstanceOf(ErrorResponse.class, oversizedResponse.getEntity());
            ErrorResponse unsupportedEntity = assertInstanceOf(ErrorResponse.class, unsupportedResponse.getEntity());
            ChatQueryResponse noMatchEntity = (ChatQueryResponse) noMatchResponse.getEntity();

            assertEquals(413, oversizedResponse.getStatus());
            assertEquals("FILE_TOO_LARGE", oversizedEntity.error());
            assertEquals(415, unsupportedResponse.getStatus());
            assertEquals("UNSUPPORTED_FILE_TYPE", unsupportedEntity.error());
            assertEquals(DocumentStatus.FAILED, failedProcessing.finalStatus());
            assertEquals(404, noMatchResponse.getStatus());
            assertEquals("no answer found", noMatchEntity.errorMessage());
        } finally {
            Files.deleteIfExists(oversized);
        }
    }
}
