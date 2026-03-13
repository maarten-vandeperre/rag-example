package com.rag.app.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
