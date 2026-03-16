# Implement Full Document Viewing

## Related User Story

User Story: view_chat_answer_with_source_details

## Objective

Implement the ability for users to view full source documents from the answer detail side view, providing access to complete document content while maintaining context of the original answer and source snippet.

## Scope

- Create full document viewer component
- Implement document content loading and display
- Handle different document types (PDF, text, markdown)
- Add navigation and search within documents
- Provide context highlighting for source snippets
- Implement document viewer modal/overlay
- Handle document access permissions and errors

## Out of Scope

- Document editing capabilities
- Document download functionality (unless specifically required)
- Advanced PDF features (annotations, forms)
- Document conversion or processing
- Collaborative viewing features

## Clean Architecture Placement

frontend UI

## Execution Dependencies

- 0059-view_chat_answer_with_source_details-create_answer_detail_side_view_component.md
- 0061-view_chat_answer_with_source_details-create_backend_api_for_answer_source_details.md
- 0062-view_chat_answer_with_source_details-implement_source_switching_functionality.md

## Implementation Details

### Document Viewer Component Architecture

**Main Component Structure:**
```jsx
const DocumentViewer = ({
  isOpen,
  document,
  highlightSnippet,
  onClose,
  onError
}) => {
  const [content, setContent] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  // Component implementation
};
```

**Document Type Handlers:**
```jsx
// DocumentContentRenderer.jsx
const DocumentContentRenderer = ({ document, content, highlightSnippet }) => {
  switch (document.fileType) {
    case 'PDF':
      return <PDFViewer content={content} highlightSnippet={highlightSnippet} />;
    case 'MARKDOWN':
      return <MarkdownViewer content={content} highlightSnippet={highlightSnippet} />;
    case 'PLAIN_TEXT':
      return <TextViewer content={content} highlightSnippet={highlightSnippet} />;
    default:
      return <UnsupportedDocumentType fileType={document.fileType} />;
  }
};
```

### Document Type Implementations

**PDF Viewer Component:**
```jsx
import { Document, Page, pdfjs } from 'react-pdf';

const PDFViewer = ({ content, highlightSnippet, onPageChange }) => {
  const [numPages, setNumPages] = useState(null);
  const [pageNumber, setPageNumber] = useState(1);

  const onDocumentLoadSuccess = ({ numPages }) => {
    setNumPages(numPages);
    
    // Navigate to page containing the snippet
    if (highlightSnippet?.pageNumber) {
      setPageNumber(highlightSnippet.pageNumber);
    }
  };

  return (
    <div className="pdf-viewer">
      <div className="pdf-controls">
        <button 
          disabled={pageNumber <= 1}
          onClick={() => setPageNumber(pageNumber - 1)}
        >
          Previous
        </button>
        <span>Page {pageNumber} of {numPages}</span>
        <button 
          disabled={pageNumber >= numPages}
          onClick={() => setPageNumber(pageNumber + 1)}
        >
          Next
        </button>
      </div>
      
      <Document
        file={content}
        onLoadSuccess={onDocumentLoadSuccess}
        loading={<DocumentLoadingSkeleton />}
        error={<DocumentErrorDisplay />}
      >
        <Page 
          pageNumber={pageNumber}
          renderTextLayer={true}
          renderAnnotationLayer={false}
        />
      </Document>
    </div>
  );
};
```

**Text/Markdown Viewer Component:**
```jsx
const TextViewer = ({ content, highlightSnippet }) => {
  const [highlightedContent, setHighlightedContent] = useState('');

  useEffect(() => {
    if (content && highlightSnippet) {
      const highlighted = highlightTextSnippet(content, highlightSnippet);
      setHighlightedContent(highlighted);
    } else {
      setHighlightedContent(content);
    }
  }, [content, highlightSnippet]);

  return (
    <div className="text-viewer">
      <div className="text-content">
        <pre dangerouslySetInnerHTML={{ __html: highlightedContent }} />
      </div>
    </div>
  );
};

// Utility function for highlighting
const highlightTextSnippet = (content, snippet) => {
  if (!snippet || !snippet.startPosition || !snippet.endPosition) {
    return content;
  }

  const before = content.substring(0, snippet.startPosition);
  const highlighted = content.substring(snippet.startPosition, snippet.endPosition);
  const after = content.substring(snippet.endPosition);

  return `${before}<mark class="snippet-highlight">${highlighted}</mark>${after}`;
};
```

### Document Viewer Modal

**Modal Implementation:**
```jsx
const DocumentViewerModal = ({ isOpen, onClose, children }) => {
  useEffect(() => {
    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleEscape);
      document.body.style.overflow = 'hidden';
    }

    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = 'unset';
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="document-viewer-modal">
      <div className="modal-backdrop" onClick={onClose} />
      <div className="modal-content">
        <div className="modal-header">
          <h2>Document Viewer</h2>
          <button className="close-button" onClick={onClose}>
            ×
          </button>
        </div>
        <div className="modal-body">
          {children}
        </div>
      </div>
    </div>
  );
};
```

### Search and Navigation Features

**Document Search Component:**
```jsx
const DocumentSearch = ({ content, onSearchResult }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [currentResult, setCurrentResult] = useState(0);

  const handleSearch = (term) => {
    if (!term || !content) {
      setSearchResults([]);
      return;
    }

    const results = findTextOccurrences(content, term);
    setSearchResults(results);
    setCurrentResult(0);
    
    if (results.length > 0) {
      onSearchResult(results[0]);
    }
  };

  return (
    <div className="document-search">
      <input
        type="text"
        placeholder="Search in document..."
        value={searchTerm}
        onChange={(e) => {
          setSearchTerm(e.target.value);
          handleSearch(e.target.value);
        }}
      />
      {searchResults.length > 0 && (
        <div className="search-navigation">
          <span>{currentResult + 1} of {searchResults.length}</span>
          <button 
            onClick={() => navigateToResult(currentResult - 1)}
            disabled={currentResult === 0}
          >
            Previous
          </button>
          <button 
            onClick={() => navigateToResult(currentResult + 1)}
            disabled={currentResult === searchResults.length - 1}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};
```

### Document Loading and Caching

**Document Loading Hook:**
```jsx
const useDocumentContent = (documentId) => {
  const [content, setContent] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const loadDocument = useCallback(async (docId) => {
    if (!docId) return;

    setLoading(true);
    setError(null);

    try {
      // Check cache first
      const cached = documentCache.get(docId);
      if (cached) {
        setContent(cached);
        setLoading(false);
        return;
      }

      const response = await apiClient.getDocumentContent(docId);
      setContent(response.content);
      
      // Cache the content
      documentCache.set(docId, response.content);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDocument(documentId);
  }, [documentId, loadDocument]);

  return { content, loading, error, reload: () => loadDocument(documentId) };
};
```

### Error Handling and Fallbacks

**Error Display Components:**
```jsx
const DocumentErrorDisplay = ({ error, onRetry, onClose }) => (
  <div className="document-error">
    <h3>Unable to Load Document</h3>
    <p className="error-message">{error}</p>
    <div className="error-actions">
      <button onClick={onRetry} className="retry-button">
        Try Again
      </button>
      <button onClick={onClose} className="close-button">
        Close
      </button>
    </div>
  </div>
);

const UnsupportedDocumentType = ({ fileType, onClose }) => (
  <div className="unsupported-document">
    <h3>Unsupported Document Type</h3>
    <p>Cannot display {fileType} files in the viewer.</p>
    <button onClick={onClose}>Close</button>
  </div>
);
```

### Integration with Answer Detail View

**Enhanced AnswerDetailSideView Integration:**
```jsx
const AnswerDetailSideView = ({ answer, isOpen, onClose }) => {
  const [documentViewerOpen, setDocumentViewerOpen] = useState(false);
  const [selectedDocument, setSelectedDocument] = useState(null);
  const [highlightSnippet, setHighlightSnippet] = useState(null);

  const handleViewFullDocument = (source) => {
    setSelectedDocument({
      id: source.documentId,
      fileName: source.fileName,
      fileType: source.fileType,
      metadata: source.metadata
    });
    setHighlightSnippet(source.snippet);
    setDocumentViewerOpen(true);
  };

  return (
    <>
      <div className="answer-detail-side-view">
        {/* Existing side view content */}
        <SourceSnippetDisplay
          source={selectedSource}
          onViewFullDocument={handleViewFullDocument}
        />
      </div>

      <DocumentViewerModal
        isOpen={documentViewerOpen}
        onClose={() => setDocumentViewerOpen(false)}
      >
        <DocumentViewer
          document={selectedDocument}
          highlightSnippet={highlightSnippet}
          onClose={() => setDocumentViewerOpen(false)}
        />
      </DocumentViewerModal>
    </>
  );
};
```

## Files / Modules Impacted

- `frontend/src/components/chat/DocumentViewer.jsx` - Main document viewer component
- `frontend/src/components/chat/DocumentViewerModal.jsx` - Modal wrapper component
- `frontend/src/components/chat/DocumentContentRenderer.jsx` - Content type router
- `frontend/src/components/chat/PDFViewer.jsx` - PDF-specific viewer
- `frontend/src/components/chat/TextViewer.jsx` - Text/markdown viewer
- `frontend/src/components/chat/DocumentSearch.jsx` - Search functionality
- `frontend/src/components/chat/DocumentViewer.module.css` - Viewer styles
- `frontend/src/hooks/useDocumentContent.js` - Document loading hook
- `frontend/src/utils/documentCache.js` - Document caching utility
- `frontend/src/utils/textHighlight.js` - Text highlighting utilities
- `frontend/src/components/chat/AnswerDetailSideView.jsx` - Integration updates

## Acceptance Criteria

**Given** a user clicks "View Full Document" from a source snippet
**When** the document is available and accessible
**Then** a document viewer should open displaying the full document content

**Given** the document viewer is open with a highlighted snippet
**When** the document loads
**Then** the viewer should automatically navigate to and highlight the relevant snippet

**Given** a user is viewing a PDF document
**When** they use the page navigation controls
**Then** they should be able to navigate between pages smoothly

**Given** a user searches for text within a document
**When** they enter a search term
**Then** the viewer should highlight all occurrences and allow navigation between them

**Given** a document fails to load
**When** the viewer attempts to display it
**Then** a clear error message should be shown with retry options

**Given** a user is viewing a document on a mobile device
**When** they interact with the viewer
**Then** the interface should be touch-friendly and responsive

## Testing Requirements

- Unit tests for document viewer components
- Integration tests for document loading and display
- Tests for different document types (PDF, text, markdown)
- Error handling tests for various failure scenarios
- Performance tests for large document loading
- Accessibility tests for keyboard navigation and screen readers
- Mobile responsiveness tests
- Search functionality tests

## Dependencies / Preconditions

- Backend API for document content retrieval must be available
- PDF.js library for PDF viewing capabilities
- Markdown parsing library for markdown documents
- Document caching infrastructure
- Authentication and authorization system

## Implementation Notes

### Performance Considerations
- Implement lazy loading for large documents
- Use virtual scrolling for very long text documents
- Cache frequently accessed documents
- Optimize PDF rendering performance

### Security Considerations
- Validate document access permissions
- Sanitize document content to prevent XSS
- Implement rate limiting for document requests
- Log document access for audit purposes

### Accessibility Features
- Keyboard navigation for all viewer controls
- Screen reader support for document content
- High contrast mode support
- Zoom functionality for better readability

### Mobile Optimization
- Touch-friendly controls and gestures
- Responsive layout for different screen sizes
- Optimized loading for mobile networks
- Appropriate touch target sizes

### Browser Compatibility
- Ensure PDF.js works across supported browsers
- Fallback options for unsupported features
- Progressive enhancement approach
- Polyfills for older browsers if needed