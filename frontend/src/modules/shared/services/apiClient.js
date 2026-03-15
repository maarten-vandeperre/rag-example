import ApiClient from '../../../services/ApiClient';

export function createApiClient(options = {}) {
  return new ApiClient(options);
}

export { ApiClient };
