import { useCallback, useState } from 'react';

import { createApiClient } from '../services/apiClient';

export function useApi(options = {}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const execute = useCallback(async (executor) => {
    setLoading(true);
    setError(null);
    const apiClient = createApiClient(options);

    try {
      return await executor(apiClient);
    } catch (requestError) {
      setError(requestError);
      throw requestError;
    } finally {
      setLoading(false);
    }
  }, [options]);

  return { execute, loading, error };
}
