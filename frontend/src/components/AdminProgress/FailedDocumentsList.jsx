function formatBytes(size) {
  if (!size) {
    return '0 B';
  }

  const units = ['B', 'KB', 'MB', 'GB'];
  const unitIndex = Math.min(Math.floor(Math.log(size) / Math.log(1024)), units.length - 1);
  const value = size / 1024 ** unitIndex;
  return `${value.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}

function formatDate(value) {
  return new Intl.DateTimeFormat('en', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function FailedDocumentsList({ failedDocuments }) {
  return (
    <section className="data-panel">
      <div className="data-panel__header">
        <h3>Failed imports</h3>
        <span>{failedDocuments.length} items</span>
      </div>
      {failedDocuments.length === 0 ? (
        <div className="data-panel__empty">No failed documents right now.</div>
      ) : (
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Filename</th>
                <th>Uploaded by</th>
                <th>Upload date</th>
                <th>Size</th>
                <th>Failure details</th>
              </tr>
            </thead>
            <tbody>
              {failedDocuments.map((document) => (
                <tr key={document.documentId}>
                  <td>{document.fileName}</td>
                  <td>{document.uploadedBy}</td>
                  <td>{formatDate(document.uploadedAt)}</td>
                  <td>{formatBytes(document.fileSize)}</td>
                  <td>
                    <details>
                      <summary>View reason</summary>
                      <p>{document.failureReason}</p>
                    </details>
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

export default FailedDocumentsList;
