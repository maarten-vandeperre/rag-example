import { useCallback, useMemo } from 'react';

import { createDocumentApi } from '../services/documentApi';

export function useDocumentList(options = {}) {
  const documentApi = useMemo(() => createDocumentApi(options), [options]);

  const loadDocuments = useCallback((includeAll = false, requestOptions = {}) => {
    return documentApi.getUserDocuments(includeAll, requestOptions);
  }, [documentApi]);

  return { loadDocuments };
}
