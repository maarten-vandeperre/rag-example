import { useEffect, useState } from 'react';

function formatDate(value) {
  return new Intl.DateTimeFormat('en', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function formatDuration(startedAt, now) {
  const elapsedSeconds = Math.max(0, Math.floor((now - new Date(startedAt).getTime()) / 1000));
  const hours = Math.floor(elapsedSeconds / 3600);
  const minutes = Math.floor((elapsedSeconds % 3600) / 60);
  const seconds = elapsedSeconds % 60;

  if (hours > 0) {
    return `${hours}h ${minutes}m ${seconds}s`;
  }

  if (minutes > 0) {
    return `${minutes}m ${seconds}s`;
  }

  return `${seconds}s`;
}

function ProcessingDocumentsList({ processingDocuments }) {
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    const intervalId = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(intervalId);
  }, []);

  return (
    <section className="data-panel">
      <div className="data-panel__header">
        <h3>Currently processing</h3>
        <span>{processingDocuments.length} active</span>
      </div>
      {processingDocuments.length === 0 ? (
        <div className="data-panel__empty">No documents are processing right now.</div>
      ) : (
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Filename</th>
                <th>Uploaded by</th>
                <th>Started</th>
                <th>Uploaded</th>
                <th>Duration</th>
              </tr>
            </thead>
            <tbody>
              {processingDocuments.map((document) => (
                <tr key={document.documentId}>
                  <td>{document.fileName}</td>
                  <td>{document.uploadedBy}</td>
                  <td>{formatDate(document.processingStartedAt)}</td>
                  <td>{formatDate(document.uploadedAt)}</td>
                  <td>
                    <span className="processing-pill">{formatDuration(document.processingStartedAt, now)}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default ProcessingDocumentsList;
