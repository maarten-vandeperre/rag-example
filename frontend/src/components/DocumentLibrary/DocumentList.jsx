import StatusBadge from './StatusBadge';

function formatFileSize(bytes) {
  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
  if (bytes >= 1024) {
    return `${Math.round(bytes / 1024)} KB`;
  }
  return `${bytes} B`;
}

function formatFileType(type) {
  return type.replaceAll('_', ' ');
}

function formatUploadDate(value) {
  return new Date(value).toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function DocumentList({ documents, isLoading }) {
  if (isLoading) {
    return <div className="document-list__empty">Loading document library...</div>;
  }

  if (!documents.length) {
    return <div className="document-list__empty">No documents yet. Upload your first file to start building the library.</div>;
  }

  return (
    <div className="document-list">
      <div className="document-list__header">
        <h2>Library</h2>
        <span>{documents.length} files</span>
      </div>
      <div className="document-table" role="table" aria-label="Uploaded documents">
        <div className="document-table__row document-table__row--head" role="row">
          <span role="columnheader">Name</span>
          <span role="columnheader">Size</span>
          <span role="columnheader">Type</span>
          <span role="columnheader">Status</span>
          <span role="columnheader">Upload date</span>
        </div>
        {documents.map((document) => (
          <div className="document-table__row" role="row" key={document.documentId}>
            <span role="cell" className="document-table__name">{document.fileName}</span>
            <span role="cell">{formatFileSize(document.fileSize)}</span>
            <span role="cell">{formatFileType(document.fileType)}</span>
            <span role="cell"><StatusBadge status={document.status} /></span>
            <span role="cell">{formatUploadDate(document.uploadedAt)}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

export { formatFileSize };
export default DocumentList;
