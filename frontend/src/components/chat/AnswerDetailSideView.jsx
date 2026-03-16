import { useEffect, useId, useMemo, useRef, useState } from 'react';

import useAnswerSources from '../../hooks/useAnswerSources';
import { logWarning } from '../../utils/errorLogger';
import styles from './AnswerDetailSideView.module.css';
import DocumentViewer from './DocumentViewer';
import DocumentViewerModal from './DocumentViewerModal';
import ErrorWarningDisplay from './ErrorWarningDisplay';
import SourceSelector from './SourceSelector';
import SourceSnippetDisplay from './SourceSnippetDisplay';
import useErrorHandling from '../../hooks/useErrorHandling';

const FOCUSABLE_SELECTOR = [
  'button:not([disabled])',
  '[href]',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])'
].join(',');

function renderAnswerContent(content) {
  if (!content) {
    return <p className={styles.emptyState}>No answer details are available yet.</p>;
  }

  return content
    .split(/\n\s*\n/)
    .filter(Boolean)
    .map((paragraph, index) => (
      <p key={`answer-paragraph-${index}`} className={styles.answerParagraph}>
        {paragraph}
      </p>
    ));
}

function getSourceScore(source) {
  if (typeof source?.relevanceScore !== 'number') {
    return null;
  }

  return `${Math.round(source.relevanceScore * 100)}% relevant`;
}

function AnswerDetailSideView({
  isOpen,
  answer,
  selectedSource,
  sources,
  onClose,
  onSourceSelect,
  onViewFullDocument,
  apiClient,
  loading = false,
  error = ''
}) {
  const panelRef = useRef(null);
  const closeButtonRef = useRef(null);
  const titleId = useId();
  const descriptionId = useId();
  const [isDocumentViewerOpen, setIsDocumentViewerOpen] = useState(false);
  const [selectedDocument, setSelectedDocument] = useState(null);
  const [dismissedWarningIds, setDismissedWarningIds] = useState([]);
  const [dismissedErrorIds, setDismissedErrorIds] = useState([]);
  const { errors, warnings, replaceErrors, replaceWarnings, removeError, removeWarning, clearAll } = useErrorHandling();

  const {
    sources: resolvedSources,
    loading: sourcesLoading,
    error: sourcesError,
    errorDetails: sourcesErrorDetails,
    hasResolvedSources,
    retry: retrySources
  } = useAnswerSources({
    answer,
    apiClient,
    initialSources: sources,
    isOpen,
    onSourceSelect,
    selectedSource
  });

  const activeSource = useMemo(
    () => resolvedSources.find((source) => source.sourceId === selectedSource?.sourceId) || resolvedSources[0] || null,
    [resolvedSources, selectedSource?.sourceId]
  );

  useEffect(() => {
    if (!isOpen) {
      return undefined;
    }

    const previousActiveElement = document.activeElement;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    closeButtonRef.current?.focus();

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onClose();
        return;
      }

      if (event.key !== 'Tab' || !panelRef.current) {
        return;
      }

      const focusableElements = Array.from(panelRef.current.querySelectorAll(FOCUSABLE_SELECTOR));
      if (focusableElements.length === 0) {
        event.preventDefault();
        return;
      }

      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];

      if (event.shiftKey && document.activeElement === firstElement) {
        event.preventDefault();
        lastElement.focus();
      } else if (!event.shiftKey && document.activeElement === lastElement) {
        event.preventDefault();
        firstElement.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
      previousActiveElement?.focus?.();
    };
  }, [isOpen, onClose]);

  useEffect(() => {
    if (!isOpen) {
      setIsDocumentViewerOpen(false);
      setSelectedDocument(null);
      setDismissedWarningIds([]);
      setDismissedErrorIds([]);
      clearAll();
    }
  }, [clearAll, isOpen]);

  useEffect(() => {
    setDismissedWarningIds([]);
    setDismissedErrorIds([]);
  }, [answer?.answerId]);

  useEffect(() => {
    if (!isOpen || loading || !hasResolvedSources) {
      return;
    }

    const availableSources = resolvedSources.filter((source) => source.available);
    const failedSources = resolvedSources.filter((source) => !source.available);

    const nextWarnings = [];

    if (!sourcesLoading && !sourcesError && resolvedSources.length === 0) {
      nextWarnings.push({
        id: 'warning-no-sources',
        key: 'no-sources',
        type: 'NO_SOURCES'
      });
    }

    if (failedSources.length > 0 && availableSources.length > 0) {
      nextWarnings.push({
        id: 'warning-partial-failure',
        key: 'partial-failure',
        type: 'PARTIAL_FAILURE',
        totalSources: resolvedSources.length,
        availableSources: availableSources.length,
        failedSources
      });
    }

    const visibleWarnings = nextWarnings.filter((entry) => !dismissedWarningIds.includes(entry.id));
    replaceWarnings(visibleWarnings);

    visibleWarnings.forEach((warning) => {
      logWarning({
        name: warning.type,
        message: warning.type,
        code: warning.type
      }, {
        answerId: answer?.answerId || null,
        feature: 'answer-source-details'
      });
    });
  }, [answer?.answerId, dismissedWarningIds, hasResolvedSources, isOpen, loading, replaceWarnings, resolvedSources, sourcesError, sourcesLoading]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const failedSources = resolvedSources.filter((source) => !source.available);
    const nextErrors = sourcesErrorDetails ? [{
      id: 'error-source-load',
      ...sourcesErrorDetails,
      message: sourcesError,
      failedSources
    }] : [];

    replaceErrors(nextErrors.filter((entry) => !dismissedErrorIds.includes(entry.id)));
  }, [dismissedErrorIds, isOpen, replaceErrors, resolvedSources, sourcesError, sourcesErrorDetails]);

  const handleDismissWarning = (warningId) => {
    setDismissedWarningIds((current) => [...current, warningId]);
    removeWarning(warningId);
  };

  const handleDismissError = (errorId) => {
    setDismissedErrorIds((current) => [...current, errorId]);
    removeError(errorId);
  };

  const handleViewFullDocument = (source) => {
    setSelectedDocument({
      documentId: source.documentId,
      fileName: source.fileName,
      fileType: source.fileType,
      metadata: source.metadata
    });
    onViewFullDocument?.(source);
    setIsDocumentViewerOpen(true);
  };

  if (!isOpen) {
    return null;
  }

  return (
    <div className={styles.backdrop} data-testid="answer-detail-backdrop" onClick={onClose}>
      <aside
        aria-describedby={descriptionId}
        aria-labelledby={titleId}
        aria-modal="true"
        className={styles.panel}
        onClick={(event) => event.stopPropagation()}
        ref={panelRef}
        role="dialog"
      >
        <header className={styles.header}>
          <div>
            <p className={styles.eyebrow}>Answer details</p>
            <h2 className={styles.title} id={titleId}>Review answer and sources</h2>
            <p className={styles.subtitle} id={descriptionId}>
              Inspect the generated answer alongside the supporting source snippet.
            </p>
          </div>
          <button
            aria-label="Close answer details"
            className={styles.closeButton}
            onClick={onClose}
            ref={closeButtonRef}
            type="button"
          >
            Close
          </button>
        </header>

        <div className={styles.content}>
          <ErrorWarningDisplay
            errors={errors}
            onDismissError={handleDismissError}
            onDismissWarning={handleDismissWarning}
            onRetry={retrySources}
            warnings={warnings}
          />

          {loading ? (
            <div aria-live="polite" className={styles.stateCard} role="status">
              <h3 className={styles.sectionTitle}>Loading details</h3>
              <p className={styles.stateMessage}>We are gathering the answer content and source details.</p>
            </div>
          ) : null}

          {error ? (
            <div aria-live="assertive" className={styles.errorCard} role="alert">
              <h3 className={styles.sectionTitle}>Unable to load answer details</h3>
              <p className={styles.stateMessage}>{error}</p>
              <p className={styles.stateMessage}>Try another source or return to the chat and try again.</p>
            </div>
          ) : null}

          {!loading ? (
            <>
              <section className={styles.section}>
                <div className={styles.sectionHeaderRow}>
                  <h3 className={styles.sectionTitle}>Answer</h3>
                  {answer?.timestamp ? <span className={styles.timestamp}>{answer.timestamp}</span> : null}
                </div>
                <div className={styles.answerCard}>{renderAnswerContent(answer?.content || answer?.answer)}</div>
              </section>

              <section className={styles.section}>
                <div className={styles.sectionHeaderRow}>
                  <h3 className={styles.sectionTitle}>Source</h3>
                  {activeSource && getSourceScore(activeSource) ? (
                    <span className={styles.matchBadge}>{getSourceScore(activeSource)}</span>
                  ) : null}
                </div>

                <SourceSnippetDisplay
                  error={sourcesErrorDetails ? '' : sourcesError}
                  loading={sourcesLoading}
                  onRetry={retrySources}
                  onViewFullDocument={handleViewFullDocument}
                  source={activeSource}
                />
              </section>

              {resolvedSources.length > 0 ? (
                <section className={styles.section}>
                  <h3 className={styles.sectionTitle}>Sources</h3>
                  <SourceSelector
                    error={sourcesError}
                    loading={sourcesLoading}
                    onRetry={retrySources}
                    onSourceSelect={onSourceSelect}
                    selectedSource={activeSource}
                    sources={resolvedSources}
                  />
                </section>
              ) : null}
            </>
          ) : null}
        </div>
      </aside>

      <DocumentViewerModal
        isOpen={isDocumentViewerOpen}
        onClose={() => setIsDocumentViewerOpen(false)}
        title={selectedDocument?.fileName || 'Document viewer'}
      >
        <DocumentViewer
          apiClient={apiClient}
          document={selectedDocument}
          highlightSnippet={activeSource?.snippet || null}
          onClose={() => setIsDocumentViewerOpen(false)}
        />
      </DocumentViewerModal>
    </div>
  );
}

export default AnswerDetailSideView;
