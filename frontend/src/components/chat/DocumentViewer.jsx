import { useMemo, useState } from 'react';

import useDocumentContent from '../../hooks/useDocumentContent';
import { findTextOccurrences, highlightSearchResult } from '../../utils/textHighlight';
import styles from './DocumentViewer.module.css';
import DocumentContentRenderer from './DocumentContentRenderer';
import DocumentSearch from './DocumentSearch';

function DocumentViewer({ apiClient, document, highlightSnippet, onClose }) {
  const [searchTerm, setSearchTerm] = useState('');
  const [activeMatchIndex, setActiveMatchIndex] = useState(0);
  const { document: loadedDocument, error, loading, reload } = useDocumentContent({
    apiClient,
    documentId: document?.documentId,
    isOpen: Boolean(document)
  });

  const activeDocument = loadedDocument || document;
  const content = activeDocument?.content || '';
  const matches = useMemo(() => findTextOccurrences(content, searchTerm), [content, searchTerm]);
  const highlightedHtml = useMemo(
    () => highlightSearchResult(content, searchTerm, activeMatchIndex, highlightSnippet),
    [activeMatchIndex, content, highlightSnippet, searchTerm]
  );

  const handleSearchChange = (value) => {
    setSearchTerm(value);
    setActiveMatchIndex(0);
  };

  const handlePrevious = () => {
    setActiveMatchIndex((current) => (current <= 0 ? matches.length - 1 : current - 1));
  };

  const handleNext = () => {
    setActiveMatchIndex((current) => (current >= matches.length - 1 ? 0 : current + 1));
  };

  if (!document) {
    return null;
  }

  return (
    <div className={styles.viewerShell}>
      <div className={styles.metaBar}>
        <div>
          <h3 className={styles.documentTitle}>{activeDocument?.fileName || document.fileName}</h3>
          <p className={styles.documentMeta}>
            {activeDocument?.fileType || document.fileType}
            {activeDocument?.metadata?.pageCount ? ` • ${activeDocument.metadata.pageCount} sections` : ''}
            {highlightSnippet?.content ? ' • Highlighted source snippet available' : ''}
          </p>
        </div>
      </div>

      <DocumentSearch
        currentMatchIndex={activeMatchIndex}
        onNext={handleNext}
        onPrevious={handlePrevious}
        onSearchChange={handleSearchChange}
        searchTerm={searchTerm}
        totalMatches={matches.length}
      />

      {loading ? (
        <div aria-live="polite" className={styles.stateCard} role="status">
          <h4 className={styles.stateTitle}>Loading document</h4>
          <p className={styles.stateMessage}>We are preparing the full document content.</p>
        </div>
      ) : null}

      {error ? (
        <div aria-live="assertive" className={styles.errorCard} role="alert">
          <h4 className={styles.stateTitle}>Unable to load document</h4>
          <p className={styles.stateMessage}>{error}</p>
          <div className={styles.errorActions}>
            <button className={styles.actionButton} onClick={reload} type="button">Try again</button>
            <button className={styles.secondaryButton} onClick={onClose} type="button">Close</button>
          </div>
        </div>
      ) : null}

      {!loading && !error ? (
        <DocumentContentRenderer document={activeDocument} html={highlightedHtml} />
      ) : null}
    </div>
  );
}

export default DocumentViewer;
