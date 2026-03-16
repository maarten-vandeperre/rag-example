import styles from './SourceSnippetDisplay.module.css';

function SourceSnippetDisplay({ source, loading = false, error = '', onRetry, onViewFullDocument }) {
  if (loading) {
    return (
      <div aria-live="polite" className={styles.stateCard} role="status">
        <h4 className={styles.title}>Loading source content</h4>
        <p className={styles.message}>We are updating the snippet for the selected source.</p>
      </div>
    );
  }

  if (error) {
    return (
      <div aria-live="assertive" className={styles.errorCard} role="alert">
        <h4 className={styles.title}>Failed to load source</h4>
        <p className={styles.message}>{error}</p>
        {onRetry ? (
          <button className={styles.retryButton} onClick={onRetry} type="button">
            Try again
          </button>
        ) : null}
      </div>
    );
  }

  if (!source || !source.available) {
    return (
      <div className={styles.stateCard}>
        <h4 className={styles.title}>Source unavailable</h4>
        <p className={styles.message}>The selected source is no longer available.</p>
      </div>
    );
  }

  return (
    <div className={styles.card}>
      <div className={styles.header}>
        <div>
          <h4 className={styles.title}>{source.metadata?.title || source.fileName}</h4>
          <p className={styles.meta}>
            {source.fileType}
            {source.metadata?.pageNumber ? ` • Page ${source.metadata.pageNumber}` : ''}
            {source.metadata?.chunkIndex !== null && source.metadata?.chunkIndex !== undefined ? ` • Chunk ${source.metadata.chunkIndex}` : ''}
            {typeof source.relevanceScore === 'number' ? ` • ${Math.round(source.relevanceScore * 100)}% relevant` : ''}
          </p>
        </div>
        <button className={styles.documentButton} onClick={() => onViewFullDocument(source)} type="button">
          View full document
        </button>
      </div>

      <blockquote className={styles.snippet}>{source.snippet?.content || 'No source snippet is available.'}</blockquote>
      {source.snippet?.context ? <p className={styles.context}>{source.snippet.context}</p> : null}
    </div>
  );
}

export default SourceSnippetDisplay;
