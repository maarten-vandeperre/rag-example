package com.rag.app.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/health")
public class BackendStatusResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public StatusResponse status() {
        return new StatusResponse("ok");
    }

    public record StatusResponse(String status) {
    }
}
