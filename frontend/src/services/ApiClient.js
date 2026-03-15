import ChatApiClient from './ChatApiClient';
import DocumentApiClient from './DocumentApiClient';
import HttpClient from '../utils/HttpClient';
import { toApiError } from './ErrorHandler';
import keycloak, { getAuthHeader, getUserInfo } from '../config/keycloak';

export async function checkBackendHealth() {
  const response = await fetch(`${process.env.REACT_APP_BACKEND_URL || 'http://localhost:8081'}/q/health`);
  if (!response.ok) {
    throw new Error(`Health check failed with status ${response.status}`);
  }
  return response.json();
}

class ApiClient {
  constructor(options = {}) {
    const httpClient = new HttpClient({
      ...options,
      getAuthToken: options.getAuthToken || (() => keycloak.token),
      getUserId: options.getUserId || (() => getUserInfo()?.userId || process.env.REACT_APP_USER_ID || null),
      requestInterceptors: [
        ...(options.requestInterceptors || []),
        (config) => ({
          ...config,
          headers: {
            ...config.headers,
            ...getAuthHeader(),
            'X-Debug-Client': 'rag-example-frontend'
          }
        })
      ],
      responseInterceptors: [
        ...(options.responseInterceptors || []),
        (payload) => payload
      ],
      errorInterceptors: [
        ...(options.errorInterceptors || []),
        (error) => toApiError(error)
      ]
    });

    this.documents = new DocumentApiClient(httpClient);
    this.chat = new ChatApiClient(httpClient);
  }

  uploadDocument(file, onProgress, options) {
    return this.documents.uploadDocument(file, onProgress, options);
  }

  getUserDocuments(includeAll, options) {
    return this.documents.getUserDocuments(includeAll, options);
  }

  getAdminProgress(options) {
    return this.documents.getAdminProgress(options);
  }

  submitChatQuery(question, maxResponseTimeMs, options) {
    return this.chat.submitChatQuery(question, maxResponseTimeMs, options);
  }
}

export default ApiClient;
