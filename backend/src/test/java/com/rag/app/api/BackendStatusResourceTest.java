package com.rag.app.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackendStatusResourceTest {

    @Test
    void shouldReturnBackendStatus() {
        BackendStatusResource resource = new BackendStatusResource();

        BackendStatusResource.StatusResponse response = resource.status();

        assertEquals("ok", response.status());
    }
}
