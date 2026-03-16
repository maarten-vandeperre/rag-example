package com.rag.app.api;

import com.rag.app.api.dto.DocumentContentMetadataDto;
import com.rag.app.api.dto.DocumentContentResponse;
import com.rag.app.usecases.GetDocumentContent;
import com.rag.app.usecases.models.DocumentContentOutput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;
import java.util.NoSuchElementException;
import java.util.UUID;

@Path("/api/documents")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class DocumentContentController {
    private final GetDocumentContent getDocumentContent;

    @Inject
    public DocumentContentController(GetDocumentContent getDocumentContent) {
        this.getDocumentContent = getDocumentContent;
    }

    @GET
    @Path("/{documentId}/content")
    public Response getDocumentContent(@PathParam("documentId") String documentId,
                                       @HeaderParam("X-User-Id") String userIdHeader,
                                       @Context SecurityContext securityContext) {
        try {
            UUID userId = extractUserId(userIdHeader, securityContext);
            UUID parsedDocumentId = UUID.fromString(documentId);
            DocumentContentOutput output = getDocumentContent.execute(parsedDocumentId, userId);
            return Response.ok(new DocumentContentResponse(
                output.documentId().toString(),
                output.fileName(),
                output.fileType(),
                output.content(),
                new DocumentContentMetadataDto(
                    output.metadata().title(),
                    output.metadata().author(),
                    output.metadata().createdAt(),
                    output.metadata().fileSize(),
                    output.metadata().pageCount()
                ),
                output.available()
            )).build();
        } catch (SecurityException exception) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new AnswerSourceController.ErrorResponse(exception.getMessage()))
                .build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new AnswerSourceController.ErrorResponse(exception.getMessage()))
                .build();
        } catch (NoSuchElementException exception) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new AnswerSourceController.ErrorResponse(exception.getMessage()))
                .build();
        }
    }

    private UUID extractUserId(String userIdHeader, SecurityContext securityContext) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return UUID.fromString(userIdHeader);
        }
        if (securityContext == null) {
            throw new SecurityException("authenticated user is required");
        }
        Principal principal = securityContext.getUserPrincipal();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new SecurityException("authenticated user is required");
        }
        return UUID.fromString(principal.getName());
    }
}
