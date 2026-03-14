# Reorganize Frontend Module Structure

## Related User Story

User Story: improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25

## Objective

Reorganize the React frontend into clearly separated product area modules that align with the backend module structure, improving maintainability and reducing coupling between different UI concerns.

## Scope

- Reorganize React components into product area modules
- Create clear boundaries between frontend modules
- Establish shared component library
- Define module communication patterns
- Update build configuration for modular frontend

## Out of Scope

- Complete UI redesign or new features
- Advanced micro-frontend architecture
- Server-side rendering implementation
- Performance optimization beyond modularity

## Clean Architecture Placement

frontend UI

## Execution Dependencies

- 0039-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-create_document_management_module.md
- 0040-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-create_chat_system_module.md
- 0041-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-create_user_management_module.md

## Implementation Details

Create modular frontend structure:
```
frontend/
├── src/
│   ├── modules/
│   │   ├── document-management/
│   │   │   ├── components/
│   │   │   │   ├── DocumentLibrary/
│   │   │   │   ├── FileUpload/
│   │   │   │   └── DocumentList/
│   │   │   ├── hooks/
│   │   │   │   ├── useDocumentUpload.js
│   │   │   │   └── useDocumentList.js
│   │   │   ├── services/
│   │   │   │   └── documentApi.js
│   │   │   └── index.js
│   │   ├── chat-system/
│   │   │   ├── components/
│   │   │   │   ├── ChatWorkspace/
│   │   │   │   ├── MessageList/
│   │   │   │   └── QueryInput/
│   │   │   ├── hooks/
│   │   │   │   ├── useChatQuery.js
│   │   │   │   └── useChatHistory.js
│   │   │   ├── services/
│   │   │   │   └── chatApi.js
│   │   │   └── index.js
│   │   ├── user-management/
│   │   │   ├── components/
│   │   │   │   ├── UserProfile/
│   │   │   │   ├── AdminPanel/
│   │   │   │   └── LoginForm/
│   │   │   ├── hooks/
│   │   │   │   ├── useAuth.js
│   │   │   │   └── useUserProfile.js
│   │   │   ├── services/
│   │   │   │   └── authApi.js
│   │   │   └── index.js
│   │   └── shared/
│   │       ├── components/
│   │       │   ├── Layout/
│   │       │   ├── Navigation/
│   │       │   ├── ErrorBoundary/
│   │       │   └── LoadingSpinner/
│   │       ├── hooks/
│   │       │   ├── useApi.js
│   │       │   └── useNotification.js
│   │       ├── services/
│   │       │   ├── apiClient.js
│   │       │   └── errorHandler.js
│   │       ├── utils/
│   │       │   ├── validation.js
│   │       │   └── formatting.js
│   │       └── constants/
│   │           └── apiEndpoints.js
│   ├── App.js
│   ├── index.js
│   └── routes/
│       └── AppRoutes.js
├── package.json
└── build.gradle
```

Module boundary enforcement:
```javascript
// eslint-rules/no-cross-module-imports.js
module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description: 'Disallow direct imports between product area modules',
    },
  },
  create(context) {
    return {
      ImportDeclaration(node) {
        const importPath = node.source.value;
        const currentFile = context.getFilename();
        
        // Extract module names
        const currentModule = extractModuleName(currentFile);
        const importModule = extractModuleName(importPath);
        
        // Check for cross-module imports (except shared)
        if (currentModule && importModule && 
            currentModule !== importModule && 
            importModule !== 'shared' &&
            !isAllowedCrossModuleImport(currentModule, importModule)) {
          context.report({
            node,
            message: `Module '${currentModule}' cannot directly import from '${importModule}'. Use shared interfaces or events.`,
          });
        }
      },
    };
  },
};
```

Document Management Module:
```javascript
// modules/document-management/index.js
export { DocumentLibrary } from './components/DocumentLibrary';
export { FileUpload } from './components/FileUpload';
export { useDocumentUpload } from './hooks/useDocumentUpload';
export { useDocumentList } from './hooks/useDocumentList';

// Module facade for external access
export const DocumentManagementModule = {
  name: 'document-management',
  version: '1.0.0',
  components: {
    DocumentLibrary,
    FileUpload,
  },
  hooks: {
    useDocumentUpload,
    useDocumentList,
  },
};

// modules/document-management/services/documentApi.js
import { apiClient } from '../../shared/services/apiClient';

export const documentApi = {
  uploadDocument: async (file, onProgress) => {
    const formData = new FormData();
    formData.append('file', file);
    
    return apiClient.post('/api/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress,
    });
  },
  
  getUserDocuments: async (includeAll = false) => {
    return apiClient.get('/api/documents', {
      params: { includeAll },
    });
  },
  
  getAdminProgress: async () => {
    return apiClient.get('/api/admin/documents/progress');
  },
};

// modules/document-management/hooks/useDocumentUpload.js
import { useState, useCallback } from 'react';
import { documentApi } from '../services/documentApi';
import { useNotification } from '../../shared/hooks/useNotification';

export const useDocumentUpload = () => {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const { showNotification } = useNotification();
  
  const uploadDocument = useCallback(async (file) => {
    if (!file) return;
    
    setUploading(true);
    setProgress(0);
    
    try {
      const result = await documentApi.uploadDocument(file, (progressEvent) => {
        const percentCompleted = Math.round(
          (progressEvent.loaded * 100) / progressEvent.total
        );
        setProgress(percentCompleted);
      });
      
      showNotification('Document uploaded successfully', 'success');
      return result.data;
    } catch (error) {
      showNotification('Failed to upload document', 'error');
      throw error;
    } finally {
      setUploading(false);
      setProgress(0);
    }
  }, [showNotification]);
  
  return {
    uploadDocument,
    uploading,
    progress,
  };
};
```

Chat System Module:
```javascript
// modules/chat-system/index.js
export { ChatWorkspace } from './components/ChatWorkspace';
export { MessageList } from './components/MessageList';
export { useChatQuery } from './hooks/useChatQuery';

export const ChatSystemModule = {
  name: 'chat-system',
  version: '1.0.0',
  components: {
    ChatWorkspace,
    MessageList,
  },
  hooks: {
    useChatQuery,
  },
};

// modules/chat-system/hooks/useChatQuery.js
import { useState, useCallback } from 'react';
import { chatApi } from '../services/chatApi';
import { useNotification } from '../../shared/hooks/useNotification';

export const useChatQuery = () => {
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState([]);
  const { showNotification } = useNotification();
  
  const submitQuery = useCallback(async (question) => {
    if (!question.trim()) return;
    
    // Add user message immediately
    const userMessage = {
      id: Date.now(),
      type: 'user',
      content: question,
      timestamp: new Date(),
    };
    
    setMessages(prev => [...prev, userMessage]);
    setLoading(true);
    
    try {
      const response = await chatApi.submitQuery(question);
      
      const assistantMessage = {
        id: Date.now() + 1,
        type: 'assistant',
        content: response.data.answer,
        documentReferences: response.data.documentReferences,
        timestamp: new Date(),
      };
      
      setMessages(prev => [...prev, assistantMessage]);
    } catch (error) {
      showNotification('Failed to process query', 'error');
      
      const errorMessage = {
        id: Date.now() + 1,
        type: 'error',
        content: 'Sorry, I encountered an error processing your question.',
        timestamp: new Date(),
      };
      
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setLoading(false);
    }
  }, [showNotification]);
  
  return {
    messages,
    submitQuery,
    loading,
    clearMessages: () => setMessages([]),
  };
};
```

Shared Module:
```javascript
// modules/shared/hooks/useApi.js
import { useState, useEffect, useCallback } from 'react';
import { apiClient } from '../services/apiClient';

export const useApi = (url, options = {}) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  const execute = useCallback(async (customUrl = url, customOptions = {}) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await apiClient.get(customUrl, {
        ...options,
        ...customOptions,
      });
      setData(response.data);
      return response.data;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [url, options]);
  
  useEffect(() => {
    if (options.immediate !== false) {
      execute();
    }
  }, [execute, options.immediate]);
  
  return {
    data,
    loading,
    error,
    execute,
    refetch: () => execute(),
  };
};

// modules/shared/services/apiClient.js
import axios from 'axios';
import { errorHandler } from './errorHandler';

const apiClient = axios.create({
  baseURL: process.env.REACT_APP_API_URL || '/api',
  timeout: 20000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    errorHandler.handleApiError(error);
    return Promise.reject(error);
  }
);

export { apiClient };
```

Module integration in App.js:
```javascript
// App.js
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { Layout } from './modules/shared/components/Layout';
import { ErrorBoundary } from './modules/shared/components/ErrorBoundary';
import { AppRoutes } from './routes/AppRoutes';
import { NotificationProvider } from './modules/shared/contexts/NotificationContext';

function App() {
  return (
    <ErrorBoundary>
      <NotificationProvider>
        <BrowserRouter>
          <Layout>
            <AppRoutes />
          </Layout>
        </BrowserRouter>
      </NotificationProvider>
    </ErrorBoundary>
  );
}

export default App;

// routes/AppRoutes.js
import React, { lazy, Suspense } from 'react';
import { Routes, Route } from 'react-router-dom';
import { LoadingSpinner } from '../modules/shared/components/LoadingSpinner';

// Lazy load module components
const DocumentLibrary = lazy(() => 
  import('../modules/document-management').then(m => ({ default: m.DocumentLibrary }))
);
const ChatWorkspace = lazy(() => 
  import('../modules/chat-system').then(m => ({ default: m.ChatWorkspace }))
);
const UserProfile = lazy(() => 
  import('../modules/user-management').then(m => ({ default: m.UserProfile }))
);

export const AppRoutes = () => {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <Routes>
        <Route path="/documents" element={<DocumentLibrary />} />
        <Route path="/chat" element={<ChatWorkspace />} />
        <Route path="/profile" element={<UserProfile />} />
        <Route path="/" element={<DocumentLibrary />} />
      </Routes>
    </Suspense>
  );
};
```

## Files / Modules Impacted

- frontend/src/modules/document-management/index.js
- frontend/src/modules/chat-system/index.js
- frontend/src/modules/user-management/index.js
- frontend/src/modules/shared/index.js
- frontend/src/App.js
- frontend/src/routes/AppRoutes.js
- frontend/.eslintrc.js (add custom rules)
- frontend/package.json (update scripts)

## Acceptance Criteria

Given the frontend is reorganized into modules
When components are developed within a module
Then they should only access other modules through defined interfaces

Given module boundaries are enforced
When attempting to import across modules inappropriately
Then the build should fail with clear error messages

Given shared components are centralized
When modules need common functionality
Then they should use the shared module

Given the modular structure is implemented
When new developers join the project
Then they should be able to understand the frontend organization quickly

## Testing Requirements

- Test module boundary enforcement
- Test component isolation within modules
- Test shared component reusability
- Test lazy loading of module components
- Test module-specific functionality

## Dependencies / Preconditions

- Backend module structure should be established
- Understanding of React component organization
- Knowledge of module bundling and lazy loading
- ESLint configuration for custom rules