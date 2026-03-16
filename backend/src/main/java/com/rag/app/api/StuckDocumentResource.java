package com.rag.app.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Path("/api/admin/documents/stuck")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class StuckDocumentResource {
    private static final String STUCK_STATUS = "UPLOADED";
    private static final String FAILED_STATUS = "FAILED";
    private static final String FAILURE_REASON = "Document upload became stuck before processing completed. Please re-upload the document to try again.";

    private final DataSource dataSource;

    @Inject
    public StuckDocumentResource(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @GET
    public Response getStuckDocuments() {
        try {
            return Response.ok(new StuckDocumentsResponse(loadStuckDocuments())).build();
        } catch (SQLException exception) {
            return serverError("Failed to load stuck documents", exception);
        }
    }

    @POST
    @Path("/{documentId}/mark-failed")
    public Response markDocumentAsFailed(@PathParam("documentId") String documentId) {
        UUID parsedDocumentId;
        try {
            parsedDocumentId = UUID.fromString(documentId);
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Invalid document id", "documentId must be a valid UUID"))
                .build();
        }

        try {
            CleanupResult result = markUploadedDocumentAsFailed(parsedDocumentId.toString());
            if (!result.found()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Document not found", "No document exists with the provided id"))
                    .build();
            }
            if (!result.updated()) {
                return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Document is not stuck", "Only documents with UPLOADED status can be marked as failed"))
                    .build();
            }

            return Response.ok(new CleanupResponse(
                1,
                List.of(parsedDocumentId.toString()),
                "Marked stuck document as failed. Please re-upload the document to process it again."
            )).build();
        } catch (SQLException exception) {
            return serverError("Failed to update stuck document", exception);
        }
    }

    @POST
    @Path("/cleanup")
    public Response cleanupStuckDocuments() {
        try {
            CleanupResponse response = cleanupAllStuckDocuments();
            return Response.ok(response).build();
        } catch (SQLException exception) {
            return serverError("Failed to clean up stuck documents", exception);
        }
    }

    private List<StuckDocumentDto> loadStuckDocuments() throws SQLException {
        String sql = """
            SELECT document_id, file_name, file_size, file_type, uploaded_by, uploaded_at, last_updated
            FROM documents
            WHERE status = ?
            ORDER BY uploaded_at ASC
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, STUCK_STATUS);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<StuckDocumentDto> stuckDocuments = new ArrayList<>();
                while (resultSet.next()) {
                    stuckDocuments.add(new StuckDocumentDto(
                        resultSet.getString("document_id"),
                        resultSet.getString("file_name"),
                        resultSet.getLong("file_size"),
                        resultSet.getString("file_type"),
                        resultSet.getString("uploaded_by"),
                        resultSet.getTimestamp("uploaded_at").toInstant(),
                        timestampToInstant(resultSet.getTimestamp("last_updated")),
                        FAILURE_REASON
                    ));
                }
                return stuckDocuments;
            }
        }
    }

    private CleanupResult markUploadedDocumentAsFailed(String documentId) throws SQLException {
        String selectSql = "SELECT status FROM documents WHERE document_id = ?";
        String updateSql = """
            UPDATE documents
            SET status = ?, failure_reason = ?, last_updated = ?, processing_started_at = NULL
            WHERE document_id = ? AND status = ?
            """;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String currentStatus;
                try (PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
                    selectStatement.setString(1, documentId);
                    try (ResultSet resultSet = selectStatement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return new CleanupResult(false, false);
                        }
                        currentStatus = resultSet.getString("status");
                    }
                }

                if (!STUCK_STATUS.equals(currentStatus)) {
                    connection.rollback();
                    return new CleanupResult(true, false);
                }

                try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                    updateStatement.setString(1, FAILED_STATUS);
                    updateStatement.setString(2, FAILURE_REASON);
                    updateStatement.setTimestamp(3, Timestamp.from(Instant.now()));
                    updateStatement.setString(4, documentId);
                    updateStatement.setString(5, STUCK_STATUS);
                    updateStatement.executeUpdate();
                }

                connection.commit();
                return new CleanupResult(true, true);
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private CleanupResponse cleanupAllStuckDocuments() throws SQLException {
        List<String> updatedDocumentIds = new ArrayList<>();
        String selectSql = "SELECT document_id FROM documents WHERE status = ? ORDER BY uploaded_at ASC";
        String updateSql = """
            UPDATE documents
            SET status = ?, failure_reason = ?, last_updated = ?, processing_started_at = NULL
            WHERE status = ?
            """;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
                    selectStatement.setString(1, STUCK_STATUS);
                    try (ResultSet resultSet = selectStatement.executeQuery()) {
                        while (resultSet.next()) {
                            updatedDocumentIds.add(resultSet.getString("document_id"));
                        }
                    }
                }

                try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                    updateStatement.setString(1, FAILED_STATUS);
                    updateStatement.setString(2, FAILURE_REASON);
                    updateStatement.setTimestamp(3, Timestamp.from(Instant.now()));
                    updateStatement.setString(4, STUCK_STATUS);
                    updateStatement.executeUpdate();
                }

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }

        return new CleanupResponse(
            updatedDocumentIds.size(),
            List.copyOf(updatedDocumentIds),
            updatedDocumentIds.isEmpty()
                ? "No stuck documents found."
                : "Marked stuck documents as failed. Please re-upload the affected documents to process them again."
        );
    }

    private Response serverError(String error, SQLException exception) {
        return Response.serverError()
            .entity(new ErrorResponse(error, exception.getMessage()))
            .build();
    }

    private Instant timestampToInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record CleanupResult(boolean found, boolean updated) {
    }

    public record StuckDocumentsResponse(List<StuckDocumentDto> documents) {
    }

    public record StuckDocumentDto(
        String documentId,
        String fileName,
        long fileSize,
        String fileType,
        String uploadedBy,
        Instant uploadedAt,
        Instant lastUpdated,
        String guidance
    ) {
    }

    public record CleanupResponse(int updatedCount, List<String> documentIds, String message) {
    }

    public record ErrorResponse(String error, String message) {
    }
}
