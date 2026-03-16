import { useCallback, useEffect, useState } from 'react';

import documentCache from '../utils/documentCache';

function useDocumentContent({ apiClient, documentId, isOpen }) {
  const [document, setDocument] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [reloadToken, setReloadToken] = useState(0);

  const reload = useCallback(() => {
    setReloadToken((current) => current + 1);
  }, []);

  useEffect(() => {
    if (!isOpen || !documentId) {
      setDocument(null);
      setLoading(false);
      setError('');
      return;
    }

    const cachedDocument = documentCache.get(documentId);
    if (cachedDocument) {
      setDocument(cachedDocument);
      setLoading(false);
      setError('');
      return;
    }

    let isCancelled = false;
    setLoading(true);
    setError('');

    apiClient.getDocumentContent(documentId)
      .then((response) => {
        if (isCancelled) {
          return;
        }

        documentCache.set(documentId, response);
        setDocument(response);
      })
      .catch((requestError) => {
        if (isCancelled) {
          return;
        }

        setError(requestError.message || 'Failed to load document content.');
      })
      .finally(() => {
        if (!isCancelled) {
          setLoading(false);
        }
      });

    return () => {
      isCancelled = true;
    };
  }, [apiClient, documentId, isOpen, reloadToken]);

  return {
    document,
    loading,
    error,
    reload
  };
}

export default useDocumentContent;
