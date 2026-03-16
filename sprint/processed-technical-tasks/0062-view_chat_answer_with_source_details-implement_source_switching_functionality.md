# Implement Source Switching Functionality

## Related User Story

User Story: view_chat_answer_with_source_details

## Objective

Implement the ability for users to switch between multiple sources within the answer detail side view, updating the displayed source snippet and metadata when a different source is selected.

## Scope

- Create source selector UI component
- Implement source switching logic
- Update source snippet display dynamically
- Handle source loading states and errors
- Provide visual indicators for source selection
- Ensure smooth transitions between sources

## Out of Scope

- Backend API implementation (separate task)
- Answer detail side view creation (separate task)
- Full document viewing (separate task)
- Chat integration (separate task)

## Clean Architecture Placement

frontend UI

## Execution Dependencies

- 0059-view_chat_answer_with_source_details-create_answer_detail_side_view_component.md
- 0061-view_chat_answer_with_source_details-create_backend_api_for_answer_source_details.md

## Implementation Details

### Source Selector Component

**Component Structure:**
```jsx
// SourceSelector.jsx
const SourceSelector = ({
  sources,
  selectedSource,
  onSourceSelect,
  loading,
  error
}) => {
  // Implementation
};
```

**UI Design Options:**

**Option 1: Tab-based Selector (Recommended for 2-5 sources)**
```jsx
<div className="source-selector-tabs">
  {sources.map((source, index) => (
    <button
      key={source.sourceId}
      className={`source-tab ${selectedSource?.sourceId === source.sourceId ? 'active' : ''}`}
      onClick={() => onSourceSelect(source)}
      disabled={!source.available}
    >
      <span className="source-number">{index + 1}</span>
      <span className="source-title">{source.fileName}</span>
      {!source.available && <span className="unavailable-indicator">⚠</span>}
    </button>
  ))}
</div>
```

**Option 2: Dropdown Selector (For many sources)**
```jsx
<select 
  className="source-dropdown"
  value={selectedSource?.sourceId || ''}
  onChange={(e) => {
    const source = sources.find(s => s.sourceId === e.target.value);
    onSourceSelect(source);
  }}
>
  {sources.map((source, index) => (
    <option 
      key={source.sourceId} 
      value={source.sourceId}
      disabled={!source.available}
    >
      {index + 1}. {source.fileName}
      {!source.available ? ' (Unavailable)' : ''}
    </option>
  ))}
</select>
```

### Source Display Component

**SourceSnippetDisplay Component:**
```jsx
const SourceSnippetDisplay = ({
  source,
  loading,
  error,
  onViewFullDocument
}) => {
  if (loading) {
    return <SourceLoadingSkeleton />;
  }

  if (error) {
    return <SourceErrorDisplay error={error} />;
  }

  if (!source || !source.available) {
    return <SourceUnavailableDisplay />;
  }

  return (
    <div className="source-snippet-display">
      <div className="source-metadata">
        <h4 className="source-title">{source.metadata.title || source.fileName}</h4>
        <div className="source-details">
          <span className="file-type">{source.fileType}</span>
          {source.metadata.pageNumber && (
            <span className="page-number">Page {source.metadata.pageNumber}</span>
          )}
          <span className="relevance-score">
            Relevance: {Math.round(source.relevanceScore * 100)}%
          </span>
        </div>
      </div>
      
      <div className="source-snippet">
        <p className="snippet-content">{source.snippet.content}</p>
        {source.snippet.context && (
          <p className="snippet-context">...{source.snippet.context}...</p>
        )}
      </div>
      
      <div className="source-actions">
        <button 
          className="view-full-document-btn"
          onClick={() => onViewFullDocument(source)}
        >
          View Full Document
        </button>
      </div>
    </div>
  );
};
```

### State Management Integration

**Enhanced AnswerDetailSideView State:**
```jsx
const AnswerDetailSideView = ({ answer, isOpen, onClose }) => {
  const [sources, setSources] = useState([]);
  const [selectedSource, setSelectedSource] = useState(null);
  const [sourcesLoading, setSourcesLoading] = useState(false);
  const [sourcesError, setSourcesError] = useState(null);

  // Load sources when answer changes
  useEffect(() => {
    if (answer && isOpen) {
      loadAnswerSources(answer.id);
    }
  }, [answer, isOpen]);

  const loadAnswerSources = async (answerId) => {
    setSourcesLoading(true);
    setSourcesError(null);
    
    try {
      const response = await apiClient.getAnswerSources(answerId);
      setSources(response.sources);
      
      // Auto-select first available source
      const firstAvailable = response.sources.find(s => s.available);
      if (firstAvailable) {
        setSelectedSource(firstAvailable);
      }
    } catch (error) {
      setSourcesError(error.message);
    } finally {
      setSourcesLoading(false);
    }
  };

  const handleSourceSelect = (source) => {
    if (source.available) {
      setSelectedSource(source);
    }
  };

  // Component render logic
};
```

### API Integration

**API Client Methods:**
```javascript
// apiClient.js
export const apiClient = {
  async getAnswerSources(answerId) {
    const response = await fetch(`/api/chat/answers/${answerId}/sources`, {
      headers: {
        'Authorization': `Bearer ${getAuthToken()}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      throw new Error(`Failed to load sources: ${response.statusText}`);
    }
    
    return response.json();
  },

  async getDocumentContent(documentId) {
    const response = await fetch(`/api/documents/${documentId}/content`, {
      headers: {
        'Authorization': `Bearer ${getAuthToken()}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      throw new Error(`Failed to load document: ${response.statusText}`);
    }
    
    return response.json();
  }
};
```

### Error Handling and Loading States

**Loading States:**
- Skeleton loader for source list while loading
- Spinner for individual source content loading
- Progressive loading for better user experience

**Error Handling:**
- Network errors with retry functionality
- Source unavailable warnings
- Graceful degradation when some sources fail

**Error Display Components:**
```jsx
const SourceErrorDisplay = ({ error, onRetry }) => (
  <div className="source-error">
    <p className="error-message">Failed to load source: {error}</p>
    <button onClick={onRetry} className="retry-button">
      Try Again
    </button>
  </div>
);

const SourceUnavailableDisplay = () => (
  <div className="source-unavailable">
    <p>This source is no longer available.</p>
    <p className="help-text">
      The document may have been removed or you may not have access to it.
    </p>
  </div>
);
```

### Accessibility Features

**Keyboard Navigation:**
- Arrow keys to navigate between source tabs
- Enter/Space to select sources
- Proper focus management

**Screen Reader Support:**
- ARIA labels for source selector
- Live regions for dynamic content updates
- Descriptive text for source metadata

**Visual Accessibility:**
- High contrast indicators for selected sources
- Clear visual hierarchy
- Sufficient color contrast ratios

## Files / Modules Impacted

- `frontend/src/components/chat/SourceSelector.jsx` - New source selector component
- `frontend/src/components/chat/SourceSnippetDisplay.jsx` - New snippet display component
- `frontend/src/components/chat/AnswerDetailSideView.jsx` - Enhanced with source switching
- `frontend/src/components/chat/SourceSelector.module.css` - Source selector styles
- `frontend/src/components/chat/SourceSnippetDisplay.module.css` - Snippet display styles
- `frontend/src/hooks/useAnswerSources.js` - Custom hook for source management
- `frontend/src/services/apiClient.js` - API integration methods
- `frontend/src/types/chat.ts` - Type definitions for sources
- `frontend/src/components/common/LoadingSkeleton.jsx` - Reusable loading component

## Acceptance Criteria

**Given** an answer has multiple sources
**When** the answer detail side view is opened
**Then** all available sources should be displayed in the source selector

**Given** multiple sources are available
**When** the user selects a different source
**Then** the displayed snippet should update to show content from the selected source

**Given** a source is unavailable
**When** the source selector is displayed
**Then** unavailable sources should be clearly marked and disabled

**Given** the user switches between sources
**When** the source content is loading
**Then** appropriate loading indicators should be shown

**Given** a source fails to load
**When** the user tries to view it
**Then** a clear error message should be displayed with retry option

**Given** the user is navigating with keyboard only
**When** they use the source selector
**Then** all sources should be accessible via keyboard navigation

## Testing Requirements

- Unit tests for source selector component
- Unit tests for source snippet display component
- Integration tests for source switching flow
- API integration tests for source loading
- Accessibility tests for keyboard navigation
- Error handling tests for various failure scenarios
- Performance tests for source switching speed
- Visual regression tests for UI consistency

## Dependencies / Preconditions

- AnswerDetailSideView component must be implemented
- Backend API for source details must be available
- Authentication system must be functional
- Error handling infrastructure must be in place

## Implementation Notes

### Performance Optimization
- Implement source content caching to avoid repeated API calls
- Use React.memo for source components to prevent unnecessary re-renders
- Consider virtual scrolling for large numbers of sources

### User Experience Enhancements
- Smooth transitions between source content
- Preload adjacent sources for faster switching
- Remember last selected source for user convenience
- Provide source preview on hover

### Mobile Considerations
- Touch-friendly source selector for mobile devices
- Swipe gestures for source navigation
- Responsive design for different screen sizes
- Appropriate touch target sizes

### Error Recovery
- Automatic retry for transient network errors
- Fallback content when sources are unavailable
- Clear user guidance for resolving issues
- Graceful degradation of functionality

### Analytics and Monitoring
- Track source switching patterns
- Monitor source loading performance
- Log errors for debugging
- Measure user engagement with sources