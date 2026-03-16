# Implement Error Handling and Warnings

## Related User Story

User Story: view_chat_answer_with_source_details

## Objective

Implement comprehensive error handling and user warnings for the chat answer detail view functionality, ensuring users receive clear feedback when sources are unavailable, cannot be loaded, or when other errors occur.

## Scope

- Create warning components for missing sources
- Implement error handling for source loading failures
- Add user-friendly error messages and recovery options
- Handle network connectivity issues
- Implement graceful degradation when sources are unavailable
- Add logging and monitoring for error tracking
- Create fallback UI states for various error scenarios

## Out of Scope

- Backend error handling (separate concern)
- Global application error boundaries (unless specifically needed)
- Performance monitoring (separate concern)
- Advanced retry mechanisms (basic retry only)

## Clean Architecture Placement

frontend UI

## Execution Dependencies

- 0059-view_chat_answer_with_source_details-create_answer_detail_side_view_component.md
- 0060-view_chat_answer_with_source_details-implement_chat_answer_click_integration.md
- 0061-view_chat_answer_with_source_details-create_backend_api_for_answer_source_details.md

## Implementation Details

### Error Types and Scenarios

**Primary Error Scenarios:**
1. **No Sources Available**: Answer has no linked sources
2. **Sources Not Found**: Sources exist but cannot be retrieved
3. **Network Errors**: API calls fail due to connectivity issues
4. **Permission Errors**: User lacks access to specific sources
5. **Document Unavailable**: Source document has been deleted or moved
6. **Partial Source Failure**: Some sources load, others fail
7. **Timeout Errors**: Source loading takes too long

### Warning Components

**NoSourcesWarning Component:**
```jsx
const NoSourcesWarning = ({ answer, onDismiss }) => {
  return (
    <div className="warning-container no-sources-warning">
      <div className="warning-icon">
        <WarningIcon />
      </div>
      <div className="warning-content">
        <h4>No Source Information Available</h4>
        <p>
          This answer doesn't have any supporting source documents available. 
          The information may have been generated from general knowledge or 
          the source documents may no longer be accessible.
        </p>
      </div>
      <div className="warning-actions">
        <button onClick={onDismiss} className="dismiss-button">
          Understood
        </button>
      </div>
    </div>
  );
};
```

**SourceUnavailableWarning Component:**
```jsx
const SourceUnavailableWarning = ({ source, error, onRetry, onDismiss }) => {
  const getErrorMessage = (error) => {
    switch (error.type) {
      case 'NOT_FOUND':
        return 'This source document could not be found. It may have been removed or you may not have access to it.';
      case 'PERMISSION_DENIED':
        return 'You do not have permission to access this source document.';
      case 'NETWORK_ERROR':
        return 'Unable to load the source due to a network error. Please check your connection and try again.';
      case 'TIMEOUT':
        return 'The source document is taking too long to load. Please try again.';
      default:
        return 'An unexpected error occurred while loading this source.';
    }
  };

  return (
    <div className="warning-container source-unavailable-warning">
      <div className="warning-icon">
        <ErrorIcon />
      </div>
      <div className="warning-content">
        <h4>Source Unavailable</h4>
        <p className="source-name">{source.fileName}</p>
        <p className="error-message">{getErrorMessage(error)}</p>
      </div>
      <div className="warning-actions">
        {error.type === 'NETWORK_ERROR' || error.type === 'TIMEOUT' ? (
          <button onClick={onRetry} className="retry-button">
            Try Again
          </button>
        ) : null}
        <button onClick={onDismiss} className="dismiss-button">
          Close
        </button>
      </div>
    </div>
  );
};
```

**PartialSourceFailureWarning Component:**
```jsx
const PartialSourceFailureWarning = ({ 
  totalSources, 
  availableSources, 
  failedSources, 
  onViewDetails,
  onDismiss 
}) => {
  return (
    <div className="warning-container partial-failure-warning">
      <div className="warning-icon">
        <WarningIcon />
      </div>
      <div className="warning-content">
        <h4>Some Sources Unavailable</h4>
        <p>
          {availableSources} of {totalSources} sources are available. 
          {failedSources.length} source{failedSources.length > 1 ? 's' : ''} could not be loaded.
        </p>
      </div>
      <div className="warning-actions">
        <button onClick={onViewDetails} className="details-button">
          View Details
        </button>
        <button onClick={onDismiss} className="dismiss-button">
          Continue
        </button>
      </div>
    </div>
  );
};
```

### Error State Management

**Error Handling Hook:**
```jsx
const useErrorHandling = () => {
  const [errors, setErrors] = useState([]);
  const [warnings, setWarnings] = useState([]);

  const addError = useCallback((error) => {
    const errorId = Date.now().toString();
    setErrors(prev => [...prev, { ...error, id: errorId, timestamp: new Date() }]);
    
    // Log error for monitoring
    logError(error);
    
    return errorId;
  }, []);

  const removeError = useCallback((errorId) => {
    setErrors(prev => prev.filter(error => error.id !== errorId));
  }, []);

  const addWarning = useCallback((warning) => {
    const warningId = Date.now().toString();
    setWarnings(prev => [...prev, { ...warning, id: warningId, timestamp: new Date() }]);
    return warningId;
  }, []);

  const removeWarning = useCallback((warningId) => {
    setWarnings(prev => prev.filter(warning => warning.id !== warningId));
  }, []);

  const clearAll = useCallback(() => {
    setErrors([]);
    setWarnings([]);
  }, []);

  return {
    errors,
    warnings,
    addError,
    removeError,
    addWarning,
    removeWarning,
    clearAll
  };
};
```

### Enhanced Answer Detail View with Error Handling

**Updated AnswerDetailSideView:**
```jsx
const AnswerDetailSideView = ({ answer, isOpen, onClose }) => {
  const [sources, setSources] = useState([]);
  const [selectedSource, setSelectedSource] = useState(null);
  const [loading, setLoading] = useState(false);
  const { errors, warnings, addError, addWarning, removeWarning } = useErrorHandling();

  const loadAnswerSources = async (answerId) => {
    setLoading(true);
    
    try {
      const response = await apiClient.getAnswerSources(answerId);
      
      if (response.sources.length === 0) {
        addWarning({
          type: 'NO_SOURCES',
          message: 'No source information available for this answer',
          answer: answer
        });
      } else {
        setSources(response.sources);
        
        // Check for partial failures
        const availableSources = response.sources.filter(s => s.available);
        const failedSources = response.sources.filter(s => !s.available);
        
        if (failedSources.length > 0 && availableSources.length > 0) {
          addWarning({
            type: 'PARTIAL_FAILURE',
            totalSources: response.sources.length,
            availableSources: availableSources.length,
            failedSources: failedSources
          });
        }
        
        // Auto-select first available source
        const firstAvailable = availableSources[0];
        if (firstAvailable) {
          setSelectedSource(firstAvailable);
        }
      }
    } catch (error) {
      addError({
        type: 'LOAD_SOURCES_FAILED',
        message: 'Failed to load source information',
        originalError: error,
        answerId: answerId
      });
    } finally {
      setLoading(false);
    }
  };

  // Render logic with error/warning handling
  return (
    <div className="answer-detail-side-view">
      {/* Error and Warning Display */}
      <ErrorWarningDisplay 
        errors={errors}
        warnings={warnings}
        onDismissWarning={removeWarning}
      />
      
      {/* Rest of component */}
    </div>
  );
};
```

### Error Display Components

**ErrorWarningDisplay Component:**
```jsx
const ErrorWarningDisplay = ({ errors, warnings, onDismissWarning }) => {
  if (errors.length === 0 && warnings.length === 0) {
    return null;
  }

  return (
    <div className="error-warning-display">
      {warnings.map(warning => (
        <WarningRenderer
          key={warning.id}
          warning={warning}
          onDismiss={() => onDismissWarning(warning.id)}
        />
      ))}
      
      {errors.map(error => (
        <ErrorRenderer
          key={error.id}
          error={error}
        />
      ))}
    </div>
  );
};

const WarningRenderer = ({ warning, onDismiss }) => {
  switch (warning.type) {
    case 'NO_SOURCES':
      return <NoSourcesWarning warning={warning} onDismiss={onDismiss} />;
    case 'PARTIAL_FAILURE':
      return <PartialSourceFailureWarning warning={warning} onDismiss={onDismiss} />;
    default:
      return <GenericWarning warning={warning} onDismiss={onDismiss} />;
  }
};
```

### Network Error Handling

**API Client with Error Handling:**
```javascript
const apiClientWithErrorHandling = {
  async getAnswerSources(answerId) {
    try {
      const response = await fetch(`/api/chat/answers/${answerId}/sources`, {
        headers: {
          'Authorization': `Bearer ${getAuthToken()}`,
          'Content-Type': 'application/json'
        },
        timeout: 10000 // 10 second timeout
      });

      if (!response.ok) {
        throw new APIError(response.status, response.statusText);
      }

      return await response.json();
    } catch (error) {
      if (error.name === 'AbortError') {
        throw new TimeoutError('Request timed out');
      } else if (error instanceof TypeError) {
        throw new NetworkError('Network connection failed');
      } else {
        throw error;
      }
    }
  }
};

// Custom Error Classes
class APIError extends Error {
  constructor(status, statusText) {
    super(`API Error: ${status} ${statusText}`);
    this.name = 'APIError';
    this.status = status;
    this.statusText = statusText;
  }
}

class NetworkError extends Error {
  constructor(message) {
    super(message);
    this.name = 'NetworkError';
  }
}

class TimeoutError extends Error {
  constructor(message) {
    super(message);
    this.name = 'TimeoutError';
  }
}
```

### Chat Integration Error Handling

**Enhanced Chat Message Component:**
```jsx
const ChatMessage = ({ message, onAnswerSelect }) => {
  const [hasSourceError, setHasSourceError] = useState(false);

  const handleAnswerClick = async (answer) => {
    try {
      // Quick check if sources are available
      const sourcesAvailable = await checkSourcesAvailability(answer.id);
      
      if (!sourcesAvailable) {
        setHasSourceError(true);
        // Show inline warning in chat
        return;
      }
      
      onAnswerSelect(answer);
    } catch (error) {
      setHasSourceError(true);
    }
  };

  return (
    <div className="chat-message">
      <div className="message-content">
        {message.content}
      </div>
      
      {message.type === 'answer' && (
        <div className="answer-actions">
          <button 
            onClick={() => handleAnswerClick(message)}
            className="view-details-button"
          >
            View Details
          </button>
          
          {hasSourceError && (
            <div className="inline-warning">
              <WarningIcon />
              <span>Source information unavailable</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
```

### Error Logging and Monitoring

**Error Logging Utility:**
```javascript
const errorLogger = {
  logError(error, context = {}) {
    const errorData = {
      timestamp: new Date().toISOString(),
      error: {
        name: error.name,
        message: error.message,
        stack: error.stack
      },
      context,
      userAgent: navigator.userAgent,
      url: window.location.href
    };

    // Log to console in development
    if (process.env.NODE_ENV === 'development') {
      console.error('Error logged:', errorData);
    }

    // Send to monitoring service in production
    if (process.env.NODE_ENV === 'production') {
      this.sendToMonitoring(errorData);
    }
  },

  async sendToMonitoring(errorData) {
    try {
      await fetch('/api/monitoring/errors', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(errorData)
      });
    } catch (err) {
      // Silently fail - don't let monitoring errors break the app
      console.warn('Failed to send error to monitoring:', err);
    }
  }
};
```

## Files / Modules Impacted

- `frontend/src/components/chat/warnings/NoSourcesWarning.jsx` - No sources warning
- `frontend/src/components/chat/warnings/SourceUnavailableWarning.jsx` - Source unavailable warning
- `frontend/src/components/chat/warnings/PartialSourceFailureWarning.jsx` - Partial failure warning
- `frontend/src/components/chat/ErrorWarningDisplay.jsx` - Error/warning container
- `frontend/src/components/chat/AnswerDetailSideView.jsx` - Enhanced with error handling
- `frontend/src/components/chat/ChatMessage.jsx` - Enhanced with inline warnings
- `frontend/src/hooks/useErrorHandling.js` - Error state management hook
- `frontend/src/services/apiClient.js` - Enhanced with error handling
- `frontend/src/utils/errorLogger.js` - Error logging utility
- `frontend/src/utils/errorTypes.js` - Error type definitions
- `frontend/src/components/chat/warnings/warnings.module.css` - Warning styles

## Acceptance Criteria

**Given** an answer has no source information
**When** the user clicks to view answer details
**Then** a clear warning should be displayed explaining that no sources are available

**Given** some sources for an answer cannot be loaded
**When** the answer detail view opens
**Then** available sources should be shown with a warning about unavailable sources

**Given** a network error occurs while loading sources
**When** the user attempts to view source details
**Then** an appropriate error message should be displayed with a retry option

**Given** a user lacks permission to access a source
**When** they try to view the source
**Then** a clear permission error message should be displayed

**Given** source loading times out
**When** the timeout occurs
**Then** a timeout error should be displayed with retry functionality

**Given** errors occur in the answer detail functionality
**When** these errors happen
**Then** they should be logged for monitoring and debugging purposes

## Testing Requirements

- Unit tests for all warning components
- Unit tests for error handling hook
- Integration tests for error scenarios
- Network error simulation tests
- Timeout handling tests
- Permission error tests
- Error logging tests
- Accessibility tests for error messages
- User experience tests for error recovery

## Dependencies / Preconditions

- Error monitoring infrastructure (optional)
- Network connectivity testing utilities
- Authentication and authorization system
- Existing chat and answer detail components

## Implementation Notes

### User Experience Considerations
- Keep error messages clear and actionable
- Provide recovery options where possible
- Don't overwhelm users with technical details
- Use consistent visual design for all warnings

### Performance Considerations
- Avoid blocking UI with error handling
- Implement efficient error state management
- Cache error states to prevent repeated failures
- Use debouncing for retry mechanisms

### Accessibility
- Ensure error messages are screen reader accessible
- Use appropriate ARIA roles and labels
- Provide keyboard navigation for error actions
- Use sufficient color contrast for warning indicators

### Monitoring and Analytics
- Track error frequencies and patterns
- Monitor user recovery actions
- Measure impact of errors on user experience
- Set up alerts for critical error thresholds

### Graceful Degradation
- Always provide fallback content when possible
- Maintain core functionality even with source errors
- Progressive enhancement for error features
- Ensure app remains stable despite errors