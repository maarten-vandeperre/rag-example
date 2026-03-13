package com.rag.app.infrastructure.persistence;

import com.rag.app.domain.valueobjects.DocumentReference;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class DocumentReferenceRowMapper {

    public DocumentReference map(ResultSet resultSet) throws SQLException {
        return new DocumentReference(
            UUID.fromString(resultSet.getString("document_id")),
            resultSet.getString("document_name"),
            resultSet.getString("paragraph_reference"),
            resultSet.getDouble("relevance_score")
        );
    }
}
