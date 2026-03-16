package com.rag.app.api;

import com.rag.app.integration.api.controllers.KnowledgeGraphController;
import com.rag.app.integration.api.dto.ApiResponse;
import com.rag.app.integration.api.dto.knowledge.KnowledgeGraphDto;
import com.rag.app.integration.api.dto.knowledge.KnowledgeGraphStatisticsDto;
import com.rag.app.integration.api.dto.knowledge.KnowledgeGraphSummaryDto;
import com.rag.app.integration.api.dto.knowledge.KnowledgeNodeDetailDto;
import com.rag.app.integration.api.dto.knowledge.KnowledgeSearchResultDto;
import com.rag.app.integration.api.dto.knowledge.KnowledgeSubgraphDto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/knowledge-graph")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class KnowledgeGraphResource {
    private final KnowledgeGraphController knowledgeGraphController;

    @Inject
    public KnowledgeGraphResource(KnowledgeGraphController knowledgeGraphController) {
        this.knowledgeGraphController = knowledgeGraphController;
    }

    @GET
    @Path("/graphs")
    public Response listGraphs(@HeaderParam("X-User-ID") String requesterUserId,
                              @QueryParam("page") @DefaultValue("0") int page,
                              @QueryParam("size") @DefaultValue("10") int size) {
        ApiResponse<List<KnowledgeGraphSummaryDto>> response = knowledgeGraphController.listGraphs(requesterUserId, page, size);
        return buildResponse(response);
    }

    @GET
    @Path("/graphs/{graphId}")
    public Response getGraph(@HeaderParam("X-User-ID") String requesterUserId,
                            @PathParam("graphId") String graphId,
                            @QueryParam("page") @DefaultValue("0") int page,
                            @QueryParam("size") @DefaultValue("100") int size) {
        ApiResponse<KnowledgeGraphDto> response = knowledgeGraphController.getGraph(requesterUserId, graphId, page, size);
        return buildResponse(response);
    }

    @GET
    @Path("/graphs/{graphId}/nodes/{nodeId}")
    public Response getNodeDetails(@HeaderParam("X-User-ID") String requesterUserId,
                                  @PathParam("graphId") String graphId,
                                  @PathParam("nodeId") String nodeId) {
        ApiResponse<KnowledgeNodeDetailDto> response = knowledgeGraphController.getNodeDetails(requesterUserId, graphId, nodeId);
        return buildResponse(response);
    }

    @GET
    @Path("/graphs/{graphId}/subgraph/{centerNodeId}")
    public Response getSubgraph(@HeaderParam("X-User-ID") String requesterUserId,
                               @PathParam("graphId") String graphId,
                               @PathParam("centerNodeId") String centerNodeId,
                               @QueryParam("depth") @DefaultValue("2") int depth) {
        ApiResponse<KnowledgeSubgraphDto> response = knowledgeGraphController.getSubgraph(requesterUserId, graphId, centerNodeId, depth);
        return buildResponse(response);
    }

    @GET
    @Path("/search")
    public Response searchGraph(@HeaderParam("X-User-ID") String requesterUserId,
                               @QueryParam("query") String query,
                               @QueryParam("graphId") String graphId,
                               @QueryParam("nodeTypes") List<String> nodeTypes,
                               @QueryParam("relationshipTypes") List<String> relationshipTypes,
                               @QueryParam("page") @DefaultValue("0") int page,
                               @QueryParam("size") @DefaultValue("20") int size) {
        ApiResponse<KnowledgeSearchResultDto> response = knowledgeGraphController.searchGraph(
            requesterUserId, query, graphId, nodeTypes, relationshipTypes, page, size);
        return buildResponse(response);
    }

    @GET
    @Path("/statistics")
    public Response getStatistics(@HeaderParam("X-User-ID") String requesterUserId,
                                 @QueryParam("graphId") String graphId) {
        ApiResponse<KnowledgeGraphStatisticsDto> response = knowledgeGraphController.getStatistics(requesterUserId, graphId);
        return buildResponse(response);
    }

    private <T> Response buildResponse(ApiResponse<T> apiResponse) {
        if (apiResponse.success()) {
            return Response.ok(apiResponse).build();
        } else {
            String errorCode = apiResponse.error() != null ? apiResponse.error().code() : "UNKNOWN_ERROR";
            return switch (errorCode) {
                case "ADMIN_ACCESS_REQUIRED" -> Response.status(Response.Status.FORBIDDEN).entity(apiResponse).build();
                case "GRAPH_NOT_FOUND", "NODE_NOT_FOUND" -> Response.status(Response.Status.NOT_FOUND).entity(apiResponse).build();
                case "INVALID_GRAPH_ID", "INVALID_NODE_ID", "INVALID_SEARCH_PARAMETERS", "INVALID_SUBGRAPH_REQUEST", 
                     "INVALID_GRAPH_LIST_REQUEST", "INVALID_STATISTICS_REQUEST" -> 
                    Response.status(Response.Status.BAD_REQUEST).entity(apiResponse).build();
                default -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(apiResponse).build();
            };
        }
    }
}
