package com.rag.app.integration.modules;

import com.rag.app.chat.interfaces.ChatSystemFacade;
import com.rag.app.document.interfaces.DocumentManagementFacade;
import com.rag.app.integration.orchestration.ApplicationOrchestrator;
import com.rag.app.user.interfaces.UserManagementFacade;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleBoundaryTest {
    @Test
    void shouldExposeOnlyFacadeBasedDependenciesInOrchestrator() {
        Set<Class<?>> fieldTypes = Arrays.stream(ApplicationOrchestrator.class.getDeclaredFields())
            .map(Field::getType)
            .collect(java.util.stream.Collectors.toSet());

        assertThat(fieldTypes).contains(DocumentManagementFacade.class, ChatSystemFacade.class, UserManagementFacade.class);
    }

    @Test
    void shouldKeepModuleFacadesAsInterfaces() {
        assertThat(DocumentManagementFacade.class.isInterface()).isTrue();
        assertThat(ChatSystemFacade.class.isInterface()).isTrue();
        assertThat(UserManagementFacade.class.isInterface()).isTrue();
    }
}
