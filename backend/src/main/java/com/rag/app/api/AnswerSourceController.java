package com.rag.app.api;

import com.rag.app.api.dto.AnswerSourceDetailDto;
import com.rag.app.api.dto.AnswerSourceDetailsResponse;
import com.rag.app.api.dto.AnswerSourceMetadataDto;
import com.rag.app.api.dto.SourceSnippetDto;
import com.rag.app.usecases.GetAnswerSourceDetails;
import com.rag.app.usecases.models.AnswerSourceDetail;
import com.rag.app.usecases.models.AnswerSourceDetailsOutput;
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

@Path("/api/chat/answers")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AnswerSourceController {
    private final GetAnswerSourceDetails getAnswerSourceDetails;

    @Inject
    public AnswerSourceController(GetAnswerSourceDetails getAnswerSourceDetails) {
        this.getAnswerSourceDetails = getAnswerSourceDetails;
    }

    @GET
    @Path("/{answerId}/sources")
    public Response getAnswerSources(@PathParam("answerId") String answerId,
                                     @HeaderParam("X-User-Id") String userIdHeader,
                                     @Context SecurityContext securityContext) {
        try {
            UUID userId = extractUserId(userIdHeader, securityContext);
            UUID parsedAnswerId = UUID.fromString(answerId);
            AnswerSourceDetailsOutput output = getAnswerSourceDetails.execute(parsedAnswerId, userId);
            return Response.ok(toResponse(output)).build();
        } catch (SecurityException exception) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
        } catch (NoSuchElementException exception) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(exception.getMessage()))
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

    private AnswerSourceDetailsResponse toResponse(AnswerSourceDetailsOutput output) {
        return new AnswerSourceDetailsResponse(
            output.answerId().toString(),
            output.sources().stream().map(this::toDto).toList(),
            output.totalSources(),
            output.availableSources()
        );
    }

    private AnswerSourceDetailDto toDto(AnswerSourceDetail source) {
        return new AnswerSourceDetailDto(
            source.sourceId(),
            source.documentId().toString(),
            source.fileName(),
            source.fileType(),
            source.snippet() == null ? null : new SourceSnippetDto(
                source.snippet().content(),
                source.snippet().startPosition(),
                source.snippet().endPosition(),
                source.snippet().context()
            ),
            new AnswerSourceMetadataDto(
                source.metadata().title(),
                source.metadata().author(),
                source.metadata().createdAt(),
                source.metadata().pageNumber(),
                source.metadata().chunkIndex()
            ),
            source.relevanceScore(),
            source.available()
        );
    }

    public record ErrorResponse(String message) {
    }
}
