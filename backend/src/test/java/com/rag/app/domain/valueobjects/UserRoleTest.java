package com.rag.app.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class UserRoleTest {

    @Test
    void shouldExposeSupportedUserRoles() {
        assertArrayEquals(new UserRole[]{UserRole.STANDARD, UserRole.ADMIN}, UserRole.values());
    }
}
