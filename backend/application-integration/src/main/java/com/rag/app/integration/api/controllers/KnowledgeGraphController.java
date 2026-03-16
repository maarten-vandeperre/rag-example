package com.rag.app.integration.api.controllers;

import com.rag.app.integration.api.dto.ApiResponse;
import com.rag.app.integration.api.dto.knowledge.KnowledgeGraphDtoMapper;
import com.rag.app.integration.api.dto.knowledge.KnowledgeGraphStatisticsDto;
import com.rag.app.integration.api.dto.knowledge.KnowledgeGraphSummaryDto;
import com.rag.app.integration.api.dto.knowledge.KnowledgeNodeDetailDto;
import com.rag.app.integration.api.dto.knowledge.KnowledgeSearchResultDto;
import com.rag.app.integration.api.dto.knowledge.KnowledgeSubgraphDto;
import com.rag.app.shared.domain.exceptions.DomainException;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeType;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipType;
import com.rag.app.shared.usecases.knowledge.BrowseKnowledgeGraph;
import com.rag.app.shared.usecases.knowledge.GetKnowledgeGraphStatistics;
import com.rag.app.shared.usecases.knowledge.SearchKnowledgeGraph;
import com.rag.app.shared.usecases.knowledge.models.BrowseKnowledgeGraphInput;
import com.rag.app.shared.usecases.knowledge.models.BrowseType;
import com.rag.app.shared.usecases.knowledge.models.GetKnowledgeGraphStatisticsInput;
import com.rag.app.shared.usecases.knowledge.models.SearchKnowledgeGraphInput;
import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.domain.valueobjects.UserRole;
import com.rag.app.user.interfaces.UserManagementFacade;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class KnowledgeGraphController {
    private final BrowseKnowledgeGraph browseKnowledgeGraph;
    private final SearchKnowledgeGraph searchKnowledgeGraph;
    private final GetKnowledgeGraphStatistics getKnowledgeGraphStatistics;
    private final KnowledgeGraphDtoMapper dtoMapper;
    private final UserManagementFacade userManagementFacade;

    public KnowledgeGraphController(BrowseKnowledgeGraph browseKnowledgeGraph,
                                    SearchKnowledgeGraph searchKnowledgeGraph,
                                    GetKnowledgeGraphStatistics getKnowledgeGraphStatistics,
                                    KnowledgeGraphDtoMapper dtoMapper,
                                    UserManagementFacade userManagementFacade) {
        this.browseKnowledgeGraph = Objects.requireNonNull(browseKnowledgeGraph, "browseKnowledgeGraph cannot be null");
        this.searchKnowledgeGraph = Objects.requireNonNull(searchKnowledgeGraph, "searchKnowledgeGraph cannot be null");
        this.getKnowledgeGraphStatistics = Objects.requireNonNull(getKnowledgeGraphStatistics, "getKnowledgeGraphStatistics cannot be null");
        this.dtoMapper = Objects.requireNonNull(dtoMapper, "dtoMapper cannot be null");
        this.userManagementFacade = Objects.requireNonNull(userManagementFacade, "userManagementFacade cannot be null");
    }

    public ApiResponse<List<KnowledgeGraphSummaryDto>> listGraphs(String requesterUserId, int page, int size) {
        try {
            ensureAdmin(requesterUserId);
            var output = browseKnowledgeGraph.execute(new BrowseKnowledgeGraphInput(BrowseType.LIST_GRAPHS, null, null, page, size));
            return ApiResponse.success(output.graphs().stream().map(dtoMapper::toSummaryDto).toList());
        } catch (SecurityException exception) {
            return ApiResponse.failure("ADMIN_ACCESS_REQUIRED", exception.getMessage());
        } catch (RuntimeException exception) {
            if (isValidationFailure(exception)) {
                return ApiResponse.failure("INVALID_GRAPH_LIST_REQUEST", exception.getMessage());
            }
            return ApiResponse.failure("GRAPH_LIST_FAILED", exception.getMessage());
        }
    }

    public ApiResponse<com.rag.app.integration.api.dto.knowledge.KnowledgeGraphDto> getGraph(String requesterUserId, String graphId, int page, int size) {
        try {
            ensureAdmin(requesterUserId);
            var output = browseKnowledgeGraph.execute(new BrowseKnowledgeGraphInput(BrowseType.GET_GRAPH, new GraphId(graphId), null, page, size));
            if (!output.found() || output.graphs().isEmpty()) {
                return ApiResponse.failure("GRAPH_NOT_FOUND", output.errorMessage());
            }
            return ApiResponse.success(dtoMapper.toDto(output.graphs().get(0), output.nodes(), output.relationships()));
        } catch (SecurityException exception) {
            return ApiResponse.failure("ADMIN_ACCESS_REQUIRED", exception.getMessage());
        } catch (RuntimeException exception) {
            if (isValidationFailure(exception)) {
                return ApiResponse.failure("INVALID_GRAPH_ID", exception.getMessage());
            }
            return ApiResponse.failure("GRAPH_RETRIEVAL_FAILED", exception.getMessage());
        }
    }

    public ApiResponse<KnowledgeNodeDetailDto> getNodeDetails(String requesterUserId, String graphId, String nodeId) {
        try {
            ensureAdmin(requesterUserId);
            var output = browseKnowledgeGraph.execute(new BrowseKnowledgeGraphInput(BrowseType.GET_NODE_DETAILS, new GraphId(graphId), new NodeId(nodeId), 0, 1));
            if (!output.found() || output.nodes().isEmpty()) {
                return ApiResponse.failure("NODE_NOT_FOUND", output.errorMessage());
            }
            return ApiResponse.success(dtoMapper.toNodeDetailDto(output.nodes().get(0), output.connectedNodes(), output.relationships()));
        } catch (SecurityException exception) {
            return ApiResponse.failure("ADMIN_ACCESS_REQUIRED", exception.getMessage());
        } catch (RuntimeException exception) {
            if (isValidationFailure(exception)) {
                return ApiResponse.failure("INVALID_NODE_ID", exception.getMessage());
            }
            return ApiResponse.failure("NODE_RETRIEVAL_FAILED", exception.getMessage());
        }
    }

    public ApiResponse<KnowledgeSubgraphDto> getSubgraph(String requesterUserId, String graphId, String centerNodeId, int depth) {
        try {
            ensureAdmin(requesterUserId);
            var output = browseKnowledgeGraph.execute(new BrowseKnowledgeGraphInput(BrowseType.GET_SUBGRAPH, new GraphId(graphId), new NodeId(centerNodeId), 0, 1000).withDepth(depth));
            return ApiResponse.success(dtoMapper.toSubgraphDto(output.nodes(), output.relationships(), centerNodeId, depth));
        } catch (SecurityException exception) {
            return ApiResponse.failure("ADMIN_ACCESS_REQUIRED", exception.getMessage());
        } catch (RuntimeException exception) {
            if (isValidationFailure(exception)) {
                return ApiResponse.failure("INVALID_SUBGRAPH_REQUEST", exception.getMessage());
            }
            return ApiResponse.failure("SUBGRAPH_RETRIEVAL_FAILED", exception.getMessage());
        }
    }

    public ApiResponse<KnowledgeSearchResultDto> searchGraph(String requesterUserId,
                                                             String query,
                                                             String graphId,
                                                             List<String> nodeTypes,
                                                             List<String> relationshipTypes,
                                                             int page,
                                                             int size) {
        try {
            ensureAdmin(requesterUserId);
            var output = searchKnowledgeGraph.execute(new SearchKnowledgeGraphInput(
                query,
                graphId == null ? null : new GraphId(graphId),
                nodeTypes == null ? List.of() : nodeTypes.stream().map(NodeType::valueOf).toList(),
                relationshipTypes == null ? List.of() : relationshipTypes.stream().map(RelationshipType::valueOf).toList(),
                page,
                size
            ));
            return ApiResponse.success(dtoMapper.toSearchResultDto(output));
        } catch (SecurityException exception) {
            return ApiResponse.failure("ADMIN_ACCESS_REQUIRED", exception.getMessage());
        } catch (RuntimeException exception) {
            if (isValidationFailure(exception)) {
                return ApiResponse.failure("INVALID_SEARCH_PARAMETERS", exception.getMessage());
            }
            return ApiResponse.failure("SEARCH_FAILED", exception.getMessage());
        }
    }

    public ApiResponse<KnowledgeGraphStatisticsDto> getStatistics(String requesterUserId, String graphId) {
        try {
            ensureAdmin(requesterUserId);
            return ApiResponse.success(dtoMapper.toStatisticsDto(
                getKnowledgeGraphStatistics.execute(new GetKnowledgeGraphStatisticsInput(graphId == null ? null : new GraphId(graphId)))
            ));
        } catch (SecurityException exception) {
            return ApiResponse.failure("ADMIN_ACCESS_REQUIRED", exception.getMessage());
        } catch (RuntimeException exception) {
            if (isValidationFailure(exception)) {
                return ApiResponse.failure("INVALID_STATISTICS_REQUEST", exception.getMessage());
            }
            return ApiResponse.failure("STATISTICS_RETRIEVAL_FAILED", exception.getMessage());
        }
    }

    private void ensureAdmin(String requesterUserId) {
        UserId userId = new UserId(UUID.fromString(requesterUserId));
        if (!userManagementFacade.isActiveUser(userId) || userManagementFacade.getUserRole(userId) != UserRole.ADMIN) {
            throw new SecurityException("Admin access required");
        }
    }

    private boolean isValidationFailure(RuntimeException exception) {
        return exception instanceof IllegalArgumentException || exception instanceof DomainException;
    }
}
