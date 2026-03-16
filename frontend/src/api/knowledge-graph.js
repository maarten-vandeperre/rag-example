import { createApiClient } from '../modules/shared';

export function createKnowledgeGraphApi(options = {}) {
  const apiClient = createApiClient(options);

  return {
    listGraphs(requestOptions = {}) {
      return apiClient.getKnowledgeGraphs(requestOptions);
    },
    getGraph(graphId, requestOptions = {}) {
      return apiClient.getKnowledgeGraph(graphId, requestOptions);
    },
    getNodeDetails(graphId, nodeId, requestOptions = {}) {
      return apiClient.getKnowledgeGraphNodeDetails(graphId, nodeId, requestOptions);
    },
    getSubgraph(graphId, centerNodeId, requestOptions = {}) {
      return apiClient.getKnowledgeGraphSubgraph(graphId, centerNodeId, requestOptions);
    },
    search(requestOptions = {}) {
      return apiClient.searchKnowledgeGraphs(requestOptions);
    },
    getStatistics(requestOptions = {}) {
      return apiClient.getKnowledgeGraphStatistics(requestOptions);
    }
  };
}
