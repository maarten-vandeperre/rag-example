const MAX_FILE_SIZE = Number(process.env.REACT_APP_MAX_FILE_SIZE || 41943040);
const ACCEPTED_EXTENSIONS = (process.env.REACT_APP_SUPPORTED_FILE_TYPES || 'pdf,md,txt')
  .split(',')
  .map((value) => `.${value.trim().toLowerCase()}`)
  .filter(Boolean);

function validateFile(file) {
  if (!file) {
    return 'Choose a file before uploading.';
  }

  const lowerName = file.name.toLowerCase();
  const isSupported = ACCEPTED_EXTENSIONS.some((extension) => lowerName.endsWith(extension));

  if (!isSupported) {
    return 'Supported files are PDF, Markdown, and plain text.';
  }

  if (file.size > MAX_FILE_SIZE) {
    return 'File size exceeds maximum allowed size of 40MB.';
  }

  return null;
}

function FileUpload({
  error,
  isUploading,
  progress,
  onFilesSelected,
  onUpload,
  selectedFile,
  validationError,
}) {
  const handleDrop = (event) => {
    event.preventDefault();
    onFilesSelected(event.dataTransfer.files);
  };

  const handleDragOver = (event) => {
    event.preventDefault();
  };

  return (
    <section className="upload-card">
      <div className="upload-card__copy">
        <span className="upload-card__eyebrow">Ingestion desk</span>
        <h2>Drop a document into the vault</h2>
        <p>PDF, Markdown, and text files up to 40MB are checked in and tracked through processing.</p>
      </div>
      <div className="upload-dropzone" onDrop={handleDrop} onDragOver={handleDragOver}>
        <input
          id="document-upload-input"
          className="upload-dropzone__input"
          type="file"
          accept=".pdf,.md,.txt,text/plain,text/markdown,application/pdf"
          onChange={(event) => onFilesSelected(event.target.files)}
        />
        <label className="upload-dropzone__label" htmlFor="document-upload-input">
          <span className="upload-dropzone__title">Drag and drop here</span>
          <span className="upload-dropzone__subtitle">or browse files from your machine</span>
        </label>
      </div>
      {selectedFile ? <div className="upload-card__file">Selected: {selectedFile.name}</div> : null}
      {validationError ? <div className="feedback feedback--error">{validationError}</div> : null}
      {error ? <div className="feedback feedback--error">{error}</div> : null}
      {isUploading ? (
        <div className="upload-progress" aria-live="polite">
          <div className="upload-progress__bar"><span style={{ width: `${progress}%` }} /></div>
          <span>{progress}% uploaded</span>
        </div>
      ) : null}
      <button className="upload-card__button" type="button" onClick={onUpload} disabled={!selectedFile || !!validationError || isUploading}>
        {isUploading ? 'Uploading...' : 'Upload document'}
      </button>
    </section>
  );
}

export { MAX_FILE_SIZE, validateFile };
export default FileUpload;
