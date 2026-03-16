import HttpClient from '../utils/HttpClient';
import { ApiError } from './ErrorHandler';

function ensureUserId(options = {}) {
  if (!options.userId) {
    throw new ApiError('A user id is required to access knowledge graph endpoints.', {
      code: 'MISSING_USER_ID'
    });
  }
}

function unwrapApiResponse(payload) {
  if (payload?.success === false) {
    throw new ApiError(payload?.error?.message || 'Knowledge graph request failed.', {
      code: payload?.error?.code || 'KNOWLEDGE_GRAPH_REQUEST_FAILED',
      details: payload?.error || payload
    });
  }

  return payload?.data ?? payload;
}

function normalizeKnowledgeGraphError(error) {
  if (error instanceof ApiError && error.code && typeof error.code === 'object') {
    return new ApiError(error.code.message || error.message, {
      status: error.status,
      code: error.code.code || 'KNOWLEDGE_GRAPH_REQUEST_FAILED',
      details: error.details,
      cause: error
    });
  }

  return error;
}

function buildQueryString(parameters = {}) {
  const searchParams = new URLSearchParams();

  Object.entries(parameters).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }

    if (Array.isArray(value)) {
      value.filter(Boolean).forEach((item) => searchParams.append(key, item));
      return;
    }

    searchParams.set(key, String(value));
  });

  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : '';
}

class KnowledgeGraphApiClient {
  constructor(httpClient = new HttpClient()) {
    this.httpClient = httpClient;
  }

  async listGraphs({ page = 0, size = 10, userId, authToken } = {}) {
    ensureUserId({ userId });

    try {
      const payload = await this.httpClient.request(`/knowledge-graph/graphs${buildQueryString({ page, size })}`, {
        userId,
        authToken
      });

      return unwrapApiResponse(payload);
    } catch (error) {
      throw normalizeKnowledgeGraphError(error);
    }
  }

  async getGraph(graphId, { page = 0, size = 100, userId, authToken } = {}) {
    ensureUserId({ userId });
    if (!graphId) {
      throw new ApiError('A graph id is required.', { code: 'MISSING_GRAPH_ID' });
    }

    try {
      const payload = await this.httpClient.request(`/knowledge-graph/graphs/${encodeURIComponent(graphId)}${buildQueryString({ page, size })}`, {
        userId,
        authToken
      });

      return unwrapApiResponse(payload);
    } catch (error) {
      throw normalizeKnowledgeGraphError(error);
    }
  }

  async getNodeDetails(graphId, nodeId, { userId, authToken } = {}) {
    ensureUserId({ userId });
    if (!graphId || !nodeId) {
      throw new ApiError('Graph and node ids are required.', { code: 'MISSING_NODE_DETAILS_PARAMETERS' });
    }

    try {
      const payload = await this.httpClient.request(
        `/knowledge-graph/graphs/${encodeURIComponent(graphId)}/nodes/${encodeURIComponent(nodeId)}`,
        { userId, authToken }
      );

      return unwrapApiResponse(payload);
    } catch (error) {
      throw normalizeKnowledgeGraphError(error);
    }
  }

  async getSubgraph(graphId, centerNodeId, { depth = 2, userId, authToken } = {}) {
    ensureUserId({ userId });
    if (!graphId || !centerNodeId) {
      throw new ApiError('Graph id and center node id are required.', { code: 'MISSING_SUBGRAPH_PARAMETERS' });
    }

    try {
      const payload = await this.httpClient.request(
        `/knowledge-graph/graphs/${encodeURIComponent(graphId)}/subgraph/${encodeURIComponent(centerNodeId)}${buildQueryString({ depth })}`,
        { userId, authToken }
      );

      return unwrapApiResponse(payload);
    } catch (error) {
      throw normalizeKnowledgeGraphError(error);
    }
  }

  async search({ query, graphId, nodeTypes = [], relationshipTypes = [], page = 0, size = 20, userId, authToken } = {}) {
    ensureUserId({ userId });
    if (!query?.trim()) {
      throw new ApiError('Enter a search query before searching.', { code: 'MISSING_SEARCH_QUERY' });
    }

    try {
      const payload = await this.httpClient.request(`/knowledge-graph/search${buildQueryString({
        query: query.trim(),
        graphId,
        nodeTypes,
        relationshipTypes,
        page,
        size
      })}`, {
        userId,
        authToken
      });

      return unwrapApiResponse(payload);
    } catch (error) {
      throw normalizeKnowledgeGraphError(error);
    }
  }

  async getStatistics({ graphId, userId, authToken } = {}) {
    ensureUserId({ userId });

    try {
      const payload = await this.httpClient.request(`/knowledge-graph/statistics${buildQueryString({ graphId })}`, {
        userId,
        authToken
      });

      return unwrapApiResponse(payload);
    } catch (error) {
      throw normalizeKnowledgeGraphError(error);
    }
  }
}

export default KnowledgeGraphApiClient;
