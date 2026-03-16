package com.rag.app.api;

import com.rag.app.api.dto.AdminProgressResponse;
import com.rag.app.api.dto.FailedDocumentDto;
import com.rag.app.api.dto.ProcessingDocumentDto;
import com.rag.app.api.dto.ProcessingStatisticsDto;
import com.rag.app.usecases.GetAdminProgress;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.GetAdminProgressInput;
import com.rag.app.usecases.models.GetAdminProgressOutput;
import com.rag.app.usecases.models.ProcessingDocumentInfo;
import com.rag.app.usecases.models.ProcessingStatistics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/admin/documents")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AdminDocumentProgressResource {
    private final GetAdminProgress getAdminProgress;

    @Inject
    public AdminDocumentProgressResource(GetAdminProgress getAdminProgress) {
        this.getAdminProgress = getAdminProgress;
    }

    @GET
    @Path("/progress")
    public Response getAdminProgress(@HeaderParam("X-User-Id") String userId) {
        try {
            GetAdminProgressOutput output = getAdminProgress.execute(new GetAdminProgressInput(userId));
            return Response.ok(toAdminProgressResponse(output)).build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.FORBIDDEN).entity(new DocumentLibraryResource.ErrorResponse(exception.getMessage())).build();
        } catch (Exception exception) {
            return Response.serverError().entity(new DocumentLibraryResource.ErrorResponse(exception.getMessage())).build();
        }
    }

    private AdminProgressResponse toAdminProgressResponse(GetAdminProgressOutput output) {
        return new AdminProgressResponse(
            toProcessingStatisticsDto(output.processingStatistics()),
            output.failedDocuments().stream().map(this::toFailedDocumentDto).toList(),
            output.processingDocuments().stream().map(this::toProcessingDocumentDto).toList()
        );
    }

    private ProcessingStatisticsDto toProcessingStatisticsDto(ProcessingStatistics statistics) {
        return new ProcessingStatisticsDto(
            statistics.totalDocuments(),
            statistics.uploadedCount(),
            statistics.processingCount(),
            statistics.readyCount(),
            statistics.failedCount()
        );
    }

    private FailedDocumentDto toFailedDocumentDto(FailedDocumentInfo failedDocument) {
        return new FailedDocumentDto(
            failedDocument.documentId(),
            failedDocument.fileName(),
            failedDocument.uploadedBy(),
            failedDocument.uploadedAt(),
            failedDocument.failureReason(),
            failedDocument.fileSize()
        );
    }

    private ProcessingDocumentDto toProcessingDocumentDto(ProcessingDocumentInfo processingDocument) {
        return new ProcessingDocumentDto(
            processingDocument.documentId(),
            processingDocument.fileName(),
            processingDocument.uploadedBy(),
            processingDocument.uploadedAt(),
            processingDocument.processingStartedAt()
        );
    }
}
