import { useEffect, useMemo, useRef, useState } from 'react';

import ApiClient from '../../services/ApiClient';
import DocumentList from './DocumentList';
import FileUpload, { validateFile } from './FileUpload';

const DEFAULT_USER_ID = process.env.REACT_APP_DEFAULT_USER_ID
  || (process.env.REACT_APP_USER_ROLE === 'ADMIN'
    ? '22222222-2222-2222-2222-222222222222'
    : '11111111-1111-1111-1111-111111111111');

function normalizeDocuments(payload) {
  const documents = Array.isArray(payload) ? payload : payload.documents || [];

  return [...documents].sort((left, right) => new Date(right.uploadedAt) - new Date(left.uploadedAt));
}

function mapFileType(fileName) {
  const lowerName = fileName.toLowerCase();
  if (lowerName.endsWith('.pdf')) {
    return 'PDF';
  }
  if (lowerName.endsWith('.md')) {
    return 'MARKDOWN';
  }
  return 'PLAIN_TEXT';
}

function DocumentLibrary({ apiBaseUrl = process.env.REACT_APP_API_URL || '/api', userId = DEFAULT_USER_ID }) {
  const [documents, setDocuments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState('');
  const [validationError, setValidationError] = useState('');
  const [progress, setProgress] = useState(0);
  const [selectedFile, setSelectedFile] = useState(null);
  const activeUploadRef = useRef(null);

  const apiClient = useMemo(() => new ApiClient({
    baseUrl: apiBaseUrl,
    getUserId: () => userId
  }), [apiBaseUrl, userId]);

  useEffect(() => {
    let ignore = false;

    async function loadDocuments() {
      setIsLoading(true);
      setError('');
      try {
        const payload = await apiClient.getUserDocuments(false, { userId });
        if (!ignore) {
          setDocuments(normalizeDocuments(payload));
        }
      } catch (fetchError) {
        if (!ignore) {
          setError(fetchError.message);
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    loadDocuments();

    return () => {
      ignore = true;
      if (activeUploadRef.current) {
        activeUploadRef.current.cancel();
      }
    };
  }, [apiClient, userId]);

  const stats = useMemo(() => ({
    total: documents.length,
    ready: documents.filter((document) => document.status === 'READY').length,
    processing: documents.filter((document) => document.status === 'PROCESSING').length,
  }), [documents]);

  const handleFilesSelected = (files) => {
    const file = files && files[0];
    const nextError = validateFile(file);
    setSelectedFile(file || null);
    setValidationError(nextError || '');
    setError('');
    setProgress(0);
  };

  const handleUpload = async () => {
    console.log('=== Upload Debug ===');
    console.log('Selected file:', selectedFile);
    console.log('File name:', selectedFile?.name);
    console.log('File size:', selectedFile?.size);
    console.log('File type:', selectedFile?.type);
    
    const nextError = validateFile(selectedFile);
    console.log('Validation error:', nextError);
    
    if (nextError) {
      setValidationError(nextError);
      return;
    }

    setIsUploading(true);
    setValidationError('');
    setError('');
    setProgress(15);

    try {
      console.log('Calling apiClient.uploadDocument...');
      const uploadRequest = apiClient.uploadDocument(selectedFile, ({ percent }) => {
        setProgress(percent);
      }, { userId });
      activeUploadRef.current = uploadRequest;

      const payload = await uploadRequest.promise;

      const newDocument = {
        documentId: payload.documentId,
        fileName: payload.fileName,
        fileSize: selectedFile.size,
        fileType: mapFileType(payload.fileName),
        status: payload.status,
        uploadedAt: payload.uploadedAt,
      };

      setDocuments((currentDocuments) => normalizeDocuments([newDocument, ...currentDocuments]));
      setSelectedFile(null);
      setProgress(100);
    } catch (uploadError) {
      setError(uploadError.message);
      setProgress(0);
    } finally {
      activeUploadRef.current = null;
      setIsUploading(false);
    }
  };

  return (
    <main className="library-shell">
      <section className="library-hero">
        <div>
          <span className="library-hero__eyebrow">Private knowledge base</span>
          <h1>Document library</h1>
          <p>Organize uploads, watch processing states, and prepare your reference set for grounded chat answers.</p>
        </div>
        <div className="library-stats">
          <article>
            <strong>{stats.total}</strong>
            <span>Total files</span>
          </article>
          <article>
            <strong>{stats.ready}</strong>
            <span>Ready to query</span>
          </article>
          <article>
            <strong>{stats.processing}</strong>
            <span>In processing</span>
          </article>
        </div>
      </section>
      <div className="library-layout">
        <FileUpload
          error={error}
          isUploading={isUploading}
          progress={progress}
          onFilesSelected={handleFilesSelected}
          onUpload={handleUpload}
          selectedFile={selectedFile}
          validationError={validationError}
        />
        <DocumentList documents={documents} isLoading={isLoading} />
      </div>
    </main>
  );
}

export default DocumentLibrary;
