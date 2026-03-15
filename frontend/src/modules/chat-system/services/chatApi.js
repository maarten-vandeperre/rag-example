import { createApiClient } from '../../shared/services/apiClient';

export function createChatApi(options = {}) {
  const apiClient = createApiClient(options);

  return {
    submitQuery(question, maxResponseTimeMs, requestOptions = {}) {
      return apiClient.submitChatQuery(question, maxResponseTimeMs, requestOptions);
    }
  };
}
