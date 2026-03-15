package com.rag.app.chat;

import com.rag.app.chat.infrastructure.persistence.JdbcChatMessageRepository;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatSystemBoundaryTest {
    @Test
    void shouldKeepJdbcRepositorySelfContainedInsideModule() throws Exception {
        Field field = JdbcChatMessageRepository.class.getDeclaredField("dataSource");

        assertEquals(DataSource.class, field.getType());
    }
}
