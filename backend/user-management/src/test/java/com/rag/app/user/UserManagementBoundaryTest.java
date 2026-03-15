package com.rag.app.user;

import com.rag.app.user.infrastructure.persistence.JdbcUserRepository;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserManagementBoundaryTest {
    @Test
    void shouldKeepJdbcRepositorySelfContainedInsideModule() throws Exception {
        Field field = JdbcUserRepository.class.getDeclaredField("dataSource");

        assertEquals(DataSource.class, field.getType());
    }
}
