package integration;

import com.rag.app.api.DocumentLibraryResource;
import com.rag.app.api.dto.DocumentListResponse;
import com.rag.app.api.dto.UploadDocumentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import jakarta.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Tag("integration")
class RoleBasedAccessIntegrationTest {

    @Test
    void shouldRestrictStandardUserViewsAndAllowAdminViews() throws Exception {
        IntegrationTestSupport support = new IntegrationTestSupport();
        UploadDocumentResponse standardUpload = support.upload(
            IntegrationTestSupport.STANDARD_USER_ID,
            "standard.txt",
            "text/plain",
            support.loadResource("test-documents/operations.txt")
        );
        UploadDocumentResponse otherUpload = support.upload(
            IntegrationTestSupport.OTHER_USER_ID,
            "other.md",
            "text/markdown",
            support.loadResource("test-documents/knowledge-base.md")
        );

        support.process(UUID.fromString(standardUpload.documentId()), support.loadResource("test-documents/operations.txt"));
        support.process(UUID.fromString(otherUpload.documentId()), support.loadResource("test-documents/knowledge-base.md"));

        DocumentListResponse standardDocuments = (DocumentListResponse) support.documents(IntegrationTestSupport.STANDARD_USER_ID, false).getEntity();
        DocumentListResponse adminDocuments = (DocumentListResponse) support.documents(IntegrationTestSupport.ADMIN_USER_ID, true).getEntity();
        Response forbiddenResponse = support.adminProgress(IntegrationTestSupport.STANDARD_USER_ID);

        assertEquals(1, standardDocuments.totalCount());
        assertEquals("standard.txt", standardDocuments.documents().get(0).fileName());
        assertEquals(2, adminDocuments.totalCount());
        DocumentLibraryResource.ErrorResponse error = assertInstanceOf(DocumentLibraryResource.ErrorResponse.class, forbiddenResponse.getEntity());
        assertEquals(403, forbiddenResponse.getStatus());
        assertEquals("adminUserId user must be an admin", error.message());
    }
}
