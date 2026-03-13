import ChatApiClient from './ChatApiClient';
import DocumentApiClient from './DocumentApiClient';
import HttpClient from '../utils/HttpClient';
import { toApiError } from './ErrorHandler';

class ApiClient {
  constructor(options = {}) {
    const httpClient = new HttpClient({
      ...options,
      requestInterceptors: [
        ...(options.requestInterceptors || []),
        (config) => ({
          ...config,
          headers: {
            ...config.headers,
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
