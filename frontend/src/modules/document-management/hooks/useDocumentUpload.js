import { useCallback, useMemo } from 'react';

import { useNotification } from '../../shared/hooks/useNotification';
import { createDocumentApi } from '../services/documentApi';

export function useDocumentUpload(options = {}) {
  const { showNotification } = useNotification();
  const documentApi = useMemo(() => createDocumentApi(options), [options]);

  const uploadDocument = useCallback(async (file, onProgress, requestOptions = {}) => {
    try {
      const request = documentApi.uploadDocument(file, onProgress, requestOptions);
      const payload = await request.promise;
      showNotification('Document uploaded successfully', 'success');
      return payload;
    } catch (error) {
      showNotification('Failed to upload document', 'error');
      throw error;
    }
  }, [documentApi, showNotification]);

  return { uploadDocument };
}
