package com.rag.app.api;

import com.rag.app.api.dto.ErrorResponse;
import com.rag.app.api.dto.UploadDocumentRequest;
import com.rag.app.api.dto.UploadDocumentResponse;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.usecases.UploadDocument;
import com.rag.app.usecases.models.UploadDocumentInput;
import com.rag.app.usecases.models.UploadDocumentOutput;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Path("/api/documents")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class DocumentUploadController {
    private final UploadDocument uploadDocument;
    private final Clock clock;

    @Inject
    public DocumentUploadController(UploadDocument uploadDocument) {
        this(uploadDocument, Clock.systemUTC());
    }

    DocumentUploadController(UploadDocument uploadDocument, Clock clock) {
        this.uploadDocument = Objects.requireNonNull(uploadDocument, "uploadDocument must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@MultipartForm UploadDocumentRequest request) {
        Instant now = Instant.now(clock);

        if (request == null || request.file == null || request.userId == null || request.userId.isBlank()) {
            return error(Response.Status.BAD_REQUEST, "INVALID_REQUEST", "File and userId are required", now);
        }

        UUID userId;
        try {
            userId = UUID.fromString(request.userId);
        } catch (IllegalArgumentException exception) {
            return error(Response.Status.BAD_REQUEST, "INVALID_USER_ID", "userId must be a valid UUID", now);
        }

        try {
            FileUpload file = request.file;
            String fileName = file.fileName();
            if (fileName == null || fileName.isBlank()) {
                return error(Response.Status.BAD_REQUEST, "INVALID_FILE", "File name is required", now);
            }

            long fileSize = Files.size(file.uploadedFile());
            if (fileSize > DocumentMetadata.MAX_FILE_SIZE_BYTES) {
                return error(Response.Status.REQUEST_ENTITY_TOO_LARGE, "FILE_TOO_LARGE", "File size exceeds maximum allowed size of 40MB", now);
            }

            FileType fileType = resolveFileType(fileName, file.contentType());
            if (fileType == null) {
                return error(Response.Status.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FILE_TYPE", "Only PDF, Markdown, and plain text files are supported", now);
            }

            byte[] fileContent = Files.readAllBytes(file.uploadedFile());
            UploadDocumentOutput output = uploadDocument.execute(new UploadDocumentInput(fileName, fileSize, fileType, fileContent, userId));

            return Response.status(Response.Status.CREATED)
                .entity(new UploadDocumentResponse(
                    output.documentId().toString(),
                    fileName,
                    output.status().name(),
                    output.message(),
                    now
                ))
                .build();
        } catch (IllegalArgumentException exception) {
            return error(Response.Status.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage(), now);
        } catch (IOException exception) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "UPLOAD_ERROR", "Failed to read uploaded file", now);
        } catch (RuntimeException exception) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "UPLOAD_ERROR", exception.getMessage(), now);
        }
    }

    private Response error(Response.Status status, String error, String message, Instant timestamp) {
        return Response.status(status).entity(new ErrorResponse(error, message, timestamp)).build();
    }

    private FileType resolveFileType(String fileName, String contentType) {
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        if (lowerFileName.endsWith(".pdf") || MediaType.APPLICATION_OCTET_STREAM.equals(contentType) && lowerFileName.endsWith(".pdf")
            || "application/pdf".equalsIgnoreCase(contentType)) {
            return FileType.PDF;
        }
        if (lowerFileName.endsWith(".md") || lowerFileName.endsWith(".markdown") || "text/markdown".equalsIgnoreCase(contentType)) {
            return FileType.MARKDOWN;
        }
        if (lowerFileName.endsWith(".txt") || "text/plain".equalsIgnoreCase(contentType)) {
            return FileType.PLAIN_TEXT;
        }
        return null;
    }
}
