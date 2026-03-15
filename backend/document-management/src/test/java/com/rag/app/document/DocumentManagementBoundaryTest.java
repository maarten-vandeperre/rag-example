package com.rag.app.document;

import com.rag.app.document.infrastructure.persistence.JdbcDocumentRepository;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentManagementBoundaryTest {
    @Test
    void shouldKeepJdbcRepositorySelfContainedInsideModule() throws Exception {
        Field field = JdbcDocumentRepository.class.getDeclaredField("dataSource");

        assertEquals(DataSource.class, field.getType());
    }
}
