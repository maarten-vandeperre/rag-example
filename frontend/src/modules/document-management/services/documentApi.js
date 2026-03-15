import { createApiClient } from '../../shared/services/apiClient';

export function createDocumentApi(options = {}) {
  const apiClient = createApiClient(options);

  return {
    uploadDocument(file, onProgress, requestOptions = {}) {
      return apiClient.uploadDocument(file, onProgress, requestOptions);
    },
    getUserDocuments(includeAll = false, requestOptions = {}) {
      return apiClient.getUserDocuments(includeAll, requestOptions);
    },
    getAdminProgress(requestOptions = {}) {
      return apiClient.getAdminProgress(requestOptions);
    }
  };
}
