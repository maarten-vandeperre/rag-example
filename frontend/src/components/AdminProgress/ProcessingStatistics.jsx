const STATUS_CONFIG = [
  { key: 'uploadedCount', label: 'Uploaded', tone: 'uploaded' },
  { key: 'processingCount', label: 'Processing', tone: 'processing' },
  { key: 'readyCount', label: 'Ready', tone: 'ready' },
  { key: 'failedCount', label: 'Failed', tone: 'failed' },
];

function percentage(count, total) {
  if (!total) {
    return 0;
  }

  return Math.round((count / total) * 100);
}

function ProcessingStatistics({ statistics }) {
  const total = statistics?.totalDocuments || 0;

  return (
    <section className="statistics-panel">
      <div className="statistics-panel__hero">
        <span className="statistics-panel__label">Total documents</span>
        <strong>{total}</strong>
      </div>
      <div className="statistics-panel__cards">
        {STATUS_CONFIG.map((status) => {
          const count = statistics?.[status.key] || 0;
          const share = percentage(count, total);

          return (
            <article key={status.key} className={`statistics-card statistics-card--${status.tone}`}>
              <div className="statistics-card__header">
                <span>{status.label}</span>
                <strong>{count}</strong>
              </div>
              <div className="statistics-card__bar" aria-hidden="true">
                <div className="statistics-card__fill" style={{ width: `${share}%` }} />
              </div>
              <span className="statistics-card__meta">{share}% of total</span>
            </article>
          );
        })}
      </div>
    </section>
  );
}

export default ProcessingStatistics;
