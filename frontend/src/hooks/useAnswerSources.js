import { useCallback, useEffect, useMemo, useState } from 'react';

import { getSourceErrorType } from '../utils/errorTypes';
import { logError } from '../utils/errorLogger';

function normalizeSource(source, index) {
  const fileName = source?.fileName || source?.documentName || source?.title || `Source ${index + 1}`;
  const location = source?.paragraphReference
    || (source?.metadata?.pageNumber ? `Page ${source.metadata.pageNumber}` : null)
    || (source?.metadata?.chunkIndex !== undefined && source?.metadata?.chunkIndex !== null
      ? `Chunk ${source.metadata.chunkIndex}`
      : null)
    || source?.section
    || source?.pageLabel
    || source?.location
    || 'Reference unavailable';

  return {
    ...source,
    sourceId: source?.sourceId || `${source?.documentId || fileName}-${location}-${index}`,
    fileName,
    fileType: source?.fileType || 'Document',
    available: source?.available !== false,
    relevanceScore: typeof source?.relevanceScore === 'number' ? source.relevanceScore : 0,
    snippet: typeof source?.snippet === 'string'
      ? { content: source.snippet, context: '', startPosition: 0, endPosition: source.snippet.length }
      : (source?.snippet || {
        content: source?.excerpt || source?.text || source?.content || '',
        context: '',
        startPosition: 0,
        endPosition: (source?.excerpt || source?.text || source?.content || '').length
      }),
    metadata: {
      title: source?.metadata?.title || source?.title || fileName,
      author: source?.metadata?.author || null,
      createdAt: source?.metadata?.createdAt || null,
      pageNumber: source?.metadata?.pageNumber ?? null,
      chunkIndex: source?.metadata?.chunkIndex ?? null
    },
    paragraphReference: source?.paragraphReference || location
  };
}

function sameSource(left, right) {
  if (!left && !right) {
    return true;
  }

  if (!left || !right) {
    return false;
  }

  return left.sourceId === right.sourceId;
}

function resolvePreferredSource(sources, selectedSource) {
  if (!sources.length) {
    return null;
  }

  if (selectedSource) {
    const matched = sources.find((source) => source.sourceId === selectedSource.sourceId);
    if (matched) {
      return matched;
    }
  }

  return sources.find((source) => source.available) || sources[0];
}

function useAnswerSources({ answer, isOpen, apiClient, initialSources = [], selectedSource, onSourceSelect }) {
  const [sources, setSources] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [errorDetails, setErrorDetails] = useState(null);
  const [hasResolvedSources, setHasResolvedSources] = useState(false);
  const [reloadToken, setReloadToken] = useState(0);

  const normalizedInitialSources = useMemo(
    () => (initialSources || []).map(normalizeSource),
    [initialSources]
  );

  const retry = useCallback(() => {
    setReloadToken((current) => current + 1);
  }, []);

  useEffect(() => {
    if (!isOpen || !answer) {
      setSources([]);
      setLoading(false);
      setError('');
      setErrorDetails(null);
      setHasResolvedSources(false);
      return;
    }

    if (!answer.answerId || typeof apiClient?.getAnswerSources !== 'function') {
      setSources(normalizedInitialSources);
      setLoading(false);
      setError('');
      setErrorDetails(null);
      setHasResolvedSources(true);
      return;
    }

    let isCancelled = false;

    setLoading(true);
    setError('');
    setErrorDetails(null);

    apiClient.getAnswerSources(answer.answerId)
      .then((response) => {
        if (isCancelled) {
          return;
        }

        const nextSources = (response?.sources || []).map(normalizeSource);
        setSources(nextSources.length > 0 ? nextSources : normalizedInitialSources);
        setErrorDetails(null);
        setHasResolvedSources(true);
      })
      .catch((requestError) => {
        if (isCancelled) {
          return;
        }

        setSources(normalizedInitialSources);
        setError(requestError.message || 'Failed to load source details.');
        setErrorDetails({
          ...requestError,
          type: getSourceErrorType(requestError)
        });
        setHasResolvedSources(true);
        logError(requestError, {
          answerId: answer.answerId,
          feature: 'answer-source-details'
        });
      })
      .finally(() => {
        if (!isCancelled) {
          setLoading(false);
        }
      });

    return () => {
      isCancelled = true;
    };
  }, [answer, apiClient, isOpen, normalizedInitialSources, reloadToken]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const preferredSource = resolvePreferredSource(sources, selectedSource);
    if (!sameSource(preferredSource, selectedSource)) {
      onSourceSelect?.(preferredSource);
    }
  }, [isOpen, onSourceSelect, selectedSource, sources]);

  return {
    sources,
    loading,
    error,
    errorDetails,
    hasResolvedSources,
    retry
  };
}

export default useAnswerSources;
