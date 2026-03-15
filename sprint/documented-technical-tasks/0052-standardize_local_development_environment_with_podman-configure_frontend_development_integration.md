# Configure Frontend Development Integration

## Related User Story

User Story: standardize_local_development_environment_with_podman

## Objective

Configure the React frontend application to integrate seamlessly with the Podman-based supporting services and native backend while running in development mode, ensuring proper connectivity and authentication.

## Scope

- Update frontend configuration for development services integration
- Configure Keycloak authentication integration
- Configure API client for backend connectivity
- Set up development environment variables
- Create development startup and testing scripts
- Configure hot reload and development debugging

## Out of Scope

- Frontend containerization (runs natively)
- Production configuration
- Advanced debugging tools setup
- Performance optimization

## Clean Architecture Placement

frontend UI, frontend API integration

## Execution Dependencies

- 0048-standardize_local_development_environment_with_podman-configure_keycloak_development_realm.md
- 0051-standardize_local_development_environment_with_podman-configure_backend_development_integration.md

## Implementation Details

Create development environment configuration (frontend/.env.development):
```bash
# Development Environment Configuration
REACT_APP_ENVIRONMENT=development

# Backend API Configuration
REACT_APP_API_URL=http://localhost:8081/api
REACT_APP_BACKEND_URL=http://localhost:8081

# Keycloak Configuration
REACT_APP_KEYCLOAK_URL=http://localhost:8180
REACT_APP_KEYCLOAK_REALM=rag-app-dev
REACT_APP_KEYCLOAK_CLIENT_ID=rag-app-frontend

# Application Configuration
REACT_APP_MAX_FILE_SIZE=41943040
REACT_APP_SUPPORTED_FILE_TYPES=pdf,md,txt
REACT_APP_CHAT_TIMEOUT=20000

# Development Features
REACT_APP_DEBUG_MODE=true
REACT_APP_SHOW_DEV_TOOLS=true
REACT_APP_LOG_LEVEL=debug

# Development Server Configuration
PORT=3000
HOST=localhost
HTTPS=false
BROWSER=true
```

Update Keycloak integration (frontend/src/config/keycloak.js):
```javascript
import Keycloak from 'keycloak-js';

const keycloakConfig = {
  url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8180',
  realm: process.env.REACT_APP_KEYCLOAK_REALM || 'rag-app-dev',
  clientId: process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'rag-app-frontend',
};

const keycloak = new Keycloak(keycloakConfig);

export const initKeycloak = async () => {
  try {
    console.log('Initializing Keycloak with config:', keycloakConfig);
    
    const authenticated = await keycloak.init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
      pkceMethod: 'S256',
      checkLoginIframe: false, // Disable for development
    });
    
    if (authenticated) {
      console.log('User authenticated:', keycloak.tokenParsed);
      
      // Set up token refresh
      setInterval(() => {
        keycloak.updateToken(70).then((refreshed) => {
          if (refreshed) {
            console.log('Token refreshed');
          }
        }).catch(() => {
          console.log('Failed to refresh token');
        });
      }, 60000);
      
      return keycloak;
    } else {
      console.log('User not authenticated');
      return keycloak;
    }
  } catch (error) {
    console.error('Keycloak initialization failed:', error);
    throw error;
  }
};

export const getAuthHeader = () => {
  if (keycloak.token) {
    return {
      'Authorization': `Bearer ${keycloak.token}`
    };
  }
  return {};
};

export const getUserInfo = () => {
  if (keycloak.tokenParsed) {
    return {
      userId: keycloak.tokenParsed.sub,
      username: keycloak.tokenParsed.preferred_username,
      email: keycloak.tokenParsed.email,
      firstName: keycloak.tokenParsed.given_name,
      lastName: keycloak.tokenParsed.family_name,
      roles: keycloak.tokenParsed.realm_access?.roles || [],
      isAdmin: keycloak.tokenParsed.realm_access?.roles?.includes('ADMIN') || false,
    };
  }
  return null;
};

export default keycloak;
```

Create development API client (frontend/src/services/apiClient.js):
```javascript
import axios from 'axios';
import { getAuthHeader } from '../config/keycloak';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081/api';
const DEBUG_MODE = process.env.REACT_APP_DEBUG_MODE === 'true';

// Create axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: parseInt(process.env.REACT_APP_CHAT_TIMEOUT) || 20000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Add authentication header
    const authHeader = getAuthHeader();
    config.headers = { ...config.headers, ...authHeader };
    
    // Debug logging
    if (DEBUG_MODE) {
      console.log('API Request:', {
        method: config.method?.toUpperCase(),
        url: config.url,
        baseURL: config.baseURL,
        headers: config.headers,
        data: config.data,
      });
    }
    
    return config;
  },
  (error) => {
    console.error('Request interceptor error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => {
    // Debug logging
    if (DEBUG_MODE) {
      console.log('API Response:', {
        status: response.status,
        statusText: response.statusText,
        url: response.config.url,
        data: response.data,
      });
    }
    
    return response;
  },
  (error) => {
    // Enhanced error handling for development
    if (DEBUG_MODE) {
      console.error('API Error:', {
        message: error.message,
        status: error.response?.status,
        statusText: error.response?.statusText,
        url: error.config?.url,
        data: error.response?.data,
      });
    }
    
    // Handle specific error cases
    if (error.response?.status === 401) {
      console.warn('Authentication required - redirecting to login');
      // Handle authentication error
    } else if (error.response?.status === 403) {
      console.warn('Access forbidden - insufficient permissions');
    } else if (error.response?.status >= 500) {
      console.error('Server error - please try again later');
    }
    
    return Promise.reject(error);
  }
);

// Health check function
export const checkBackendHealth = async () => {
  try {
    const response = await axios.get(`${process.env.REACT_APP_BACKEND_URL}/q/health`);
    return response.data;
  } catch (error) {
    console.error('Backend health check failed:', error);
    throw error;
  }
};

// API methods
export const documentApi = {
  uploadDocument: async (file, onProgress) => {
    const formData = new FormData();
    formData.append('file', file);
    
    return apiClient.post('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress,
    });
  },
  
  getUserDocuments: async (includeAll = false) => {
    return apiClient.get('/documents', {
      params: { includeAll },
    });
  },
  
  getAdminProgress: async () => {
    return apiClient.get('/admin/documents/progress');
  },
};

export const chatApi = {
  submitQuery: async (question, maxResponseTimeMs = 20000) => {
    return apiClient.post('/chat/query', {
      question,
      maxResponseTimeMs,
    });
  },
  
  getChatHistory: async () => {
    return apiClient.get('/chat/history');
  },
};

export default apiClient;
```

Create development startup script (frontend/start-dev.sh):
```bash
#!/bin/bash
set -e

echo "=== Starting RAG Frontend in Development Mode ==="

# Check if backend is running
echo "Checking backend availability..."
if ! curl -f http://localhost:8081/q/health > /dev/null 2>&1; then
    echo "WARNING: Backend is not running"
    echo "Please start the backend first: cd backend && ./start-dev.sh"
    echo "Or start it separately: ./gradlew :backend:dev"
    echo ""
    echo "Continuing with frontend startup..."
fi

# Check if Keycloak is running
echo "Checking Keycloak availability..."
if ! curl -f http://localhost:8180/health/ready > /dev/null 2>&1; then
    echo "WARNING: Keycloak is not running"
    echo "Please start development services first: ./start-dev-services.sh"
    echo ""
    echo "Continuing with frontend startup..."
fi

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Create silent check SSO file for Keycloak
cat > public/silent-check-sso.html << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>Silent Check SSO</title>
</head>
<body>
    <script>
        parent.postMessage(location.href, location.origin);
    </script>
</body>
</html>
EOF

echo "✓ Silent SSO check file created"

# Set development environment
export NODE_ENV=development
export BROWSER=true

echo ""
echo "Starting React development server..."
echo "Frontend will be available at: http://localhost:3000"
echo "Backend API: http://localhost:8081/api"
echo "Keycloak: http://localhost:8180"
echo ""
echo "Press Ctrl+C to stop the frontend"
echo ""

# Start React development server
npm start
```

Create development testing script (frontend/test-dev-integration.sh):
```bash
#!/bin/bash
set -e

FRONTEND_URL="http://localhost:3000"
BACKEND_URL="http://localhost:8081"
KEYCLOAK_URL="http://localhost:8180"

echo "=== Testing Frontend Development Integration ==="

# Check if frontend is running
echo "Checking frontend availability..."
if curl -f "$FRONTEND_URL" > /dev/null 2>&1; then
    echo "✓ Frontend is accessible"
else
    echo "✗ Frontend is not accessible"
    echo "Please start the frontend: ./start-dev.sh"
    exit 1
fi

# Check backend connectivity
echo "Checking backend connectivity..."
if curl -f "$BACKEND_URL/q/health" > /dev/null 2>&1; then
    echo "✓ Backend is accessible"
else
    echo "✗ Backend is not accessible"
fi

# Check Keycloak connectivity
echo "Checking Keycloak connectivity..."
if curl -f "$KEYCLOAK_URL/health/ready" > /dev/null 2>&1; then
    echo "✓ Keycloak is accessible"
else
    echo "✗ Keycloak is not accessible"
fi

# Test CORS configuration
echo "Testing CORS configuration..."
CORS_RESPONSE=$(curl -s -H "Origin: http://localhost:3000" \
    -H "Access-Control-Request-Method: GET" \
    -H "Access-Control-Request-Headers: Content-Type,Authorization" \
    -X OPTIONS "$BACKEND_URL/api/documents" 2>/dev/null || echo "")

if echo "$CORS_RESPONSE" | grep -q "Access-Control-Allow-Origin"; then
    echo "✓ CORS configured correctly"
else
    echo "✗ CORS not configured properly"
fi

# Test API endpoints (without authentication)
echo "Testing public API endpoints..."

echo -n "Health endpoint: "
if curl -f "$BACKEND_URL/q/health" > /dev/null 2>&1; then
    echo "✓"
else
    echo "✗"
fi

echo -n "OpenAPI spec: "
if curl -f "$BACKEND_URL/q/openapi" > /dev/null 2>&1; then
    echo "✓"
else
    echo "✗"
fi

# Test Keycloak realm
echo "Testing Keycloak realm configuration..."
REALM_RESPONSE=$(curl -s "$KEYCLOAK_URL/realms/rag-app-dev" 2>/dev/null || echo "")
if echo "$REALM_RESPONSE" | grep -q "rag-app-dev"; then
    echo "✓ Keycloak realm configured"
else
    echo "✗ Keycloak realm not found"
fi

echo ""
echo "=== Frontend Development Integration Test Complete ==="
echo ""
echo "To test the full application:"
echo "1. Open http://localhost:3000 in your browser"
echo "2. Try logging in with: john.doe / password123"
echo "3. Test document upload and chat functionality"
```

Create development configuration component (frontend/src/components/DevTools/DevPanel.jsx):
```javascript
import React, { useState, useEffect } from 'react';
import { checkBackendHealth } from '../../services/apiClient';
import keycloak, { getUserInfo } from '../../config/keycloak';

const DevPanel = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [backendHealth, setBackendHealth] = useState(null);
  const [userInfo, setUserInfo] = useState(null);

  useEffect(() => {
    if (process.env.REACT_APP_SHOW_DEV_TOOLS === 'true') {
      // Load user info
      setUserInfo(getUserInfo());
      
      // Check backend health
      checkBackendHealth()
        .then(setBackendHealth)
        .catch(error => setBackendHealth({ error: error.message }));
    }
  }, []);

  if (process.env.REACT_APP_SHOW_DEV_TOOLS !== 'true') {
    return null;
  }

  return (
    <div style={{
      position: 'fixed',
      bottom: '20px',
      right: '20px',
      zIndex: 9999,
      backgroundColor: '#f0f0f0',
      border: '1px solid #ccc',
      borderRadius: '8px',
      padding: '10px',
      maxWidth: '400px',
      fontSize: '12px',
      fontFamily: 'monospace',
    }}>
      <button 
        onClick={() => setIsOpen(!isOpen)}
        style={{
          backgroundColor: '#007bff',
          color: 'white',
          border: 'none',
          padding: '5px 10px',
          borderRadius: '4px',
          cursor: 'pointer',
        }}
      >
        {isOpen ? 'Hide' : 'Show'} Dev Tools
      </button>
      
      {isOpen && (
        <div style={{ marginTop: '10px' }}>
          <h4>Development Information</h4>
          
          <div>
            <strong>Environment:</strong> {process.env.REACT_APP_ENVIRONMENT}
          </div>
          
          <div>
            <strong>Backend URL:</strong> {process.env.REACT_APP_BACKEND_URL}
          </div>
          
          <div>
            <strong>Keycloak URL:</strong> {process.env.REACT_APP_KEYCLOAK_URL}
          </div>
          
          <div style={{ marginTop: '10px' }}>
            <strong>Backend Health:</strong>
            <pre style={{ fontSize: '10px', backgroundColor: '#f8f9fa', padding: '5px' }}>
              {backendHealth ? JSON.stringify(backendHealth, null, 2) : 'Loading...'}
            </pre>
          </div>
          
          <div style={{ marginTop: '10px' }}>
            <strong>User Info:</strong>
            <pre style={{ fontSize: '10px', backgroundColor: '#f8f9fa', padding: '5px' }}>
              {userInfo ? JSON.stringify(userInfo, null, 2) : 'Not authenticated'}
            </pre>
          </div>
          
          <div style={{ marginTop: '10px' }}>
            <strong>Keycloak Token:</strong>
            <div style={{ fontSize: '10px', wordBreak: 'break-all' }}>
              {keycloak.token ? `${keycloak.token.substring(0, 50)}...` : 'No token'}
            </div>
          </div>
          
          <div style={{ marginTop: '10px' }}>
            <button 
              onClick={() => keycloak.login()}
              style={{ marginRight: '5px', padding: '3px 6px', fontSize: '10px' }}
            >
              Login
            </button>
            <button 
              onClick={() => keycloak.logout()}
              style={{ marginRight: '5px', padding: '3px 6px', fontSize: '10px' }}
            >
              Logout
            </button>
            <button 
              onClick={() => window.location.reload()}
              style={{ padding: '3px 6px', fontSize: '10px' }}
            >
              Reload
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default DevPanel;
```

Update main App component (frontend/src/App.js):
```javascript
import React, { useEffect, useState } from 'react';
import { BrowserRouter } from 'react-router-dom';
import { initKeycloak } from './config/keycloak';
import { Layout } from './modules/shared/components/Layout';
import { ErrorBoundary } from './modules/shared/components/ErrorBoundary';
import { AppRoutes } from './routes/AppRoutes';
import { NotificationProvider } from './modules/shared/contexts/NotificationContext';
import DevPanel from './components/DevTools/DevPanel';
import './App.css';

function App() {
  const [keycloakInitialized, setKeycloakInitialized] = useState(false);
  const [initError, setInitError] = useState(null);

  useEffect(() => {
    initKeycloak()
      .then(() => {
        console.log('Keycloak initialized successfully');
        setKeycloakInitialized(true);
      })
      .catch((error) => {
        console.error('Keycloak initialization failed:', error);
        setInitError(error.message);
        setKeycloakInitialized(true); // Continue without auth for development
      });
  }, []);

  if (!keycloakInitialized) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh',
        flexDirection: 'column'
      }}>
        <div>Initializing authentication...</div>
        {process.env.REACT_APP_DEBUG_MODE === 'true' && (
          <div style={{ marginTop: '20px', fontSize: '12px', color: '#666' }}>
            Debug mode: Check console for details
          </div>
        )}
      </div>
    );
  }

  if (initError && process.env.REACT_APP_DEBUG_MODE === 'true') {
    console.warn('Running in development mode without authentication:', initError);
  }

  return (
    <ErrorBoundary>
      <NotificationProvider>
        <BrowserRouter>
          <Layout>
            <AppRoutes />
          </Layout>
          <DevPanel />
        </BrowserRouter>
      </NotificationProvider>
    </ErrorBoundary>
  );
}

export default App;
```

Update package.json scripts (frontend/package.json):
```json
{
  "scripts": {
    "start": "react-scripts start",
    "start:dev": "./start-dev.sh",
    "build": "react-scripts build",
    "test": "react-scripts test",
    "test:dev": "./test-dev-integration.sh",
    "eject": "react-scripts eject",
    "lint": "eslint src --ext .js,.jsx,.ts,.tsx",
    "lint:fix": "eslint src --ext .js,.jsx,.ts,.tsx --fix"
  }
}
```

## Files / Modules Impacted

- frontend/.env.development
- frontend/src/config/keycloak.js
- frontend/src/services/apiClient.js
- frontend/start-dev.sh
- frontend/test-dev-integration.sh
- frontend/src/components/DevTools/DevPanel.jsx
- frontend/src/App.js
- frontend/package.json
- frontend/public/silent-check-sso.html

## Acceptance Criteria

Given the frontend is configured for development
When ./start-dev.sh is executed
Then the frontend should start and connect to backend and Keycloak

Given Keycloak is properly configured
When a user attempts to authenticate
Then they should be able to login with development credentials

Given the backend API is accessible
When API requests are made from the frontend
Then they should include proper authentication headers

Given development tools are enabled
When the application is running in debug mode
Then development information should be available

## Testing Requirements

- Test frontend startup with supporting services
- Test Keycloak authentication integration
- Test API connectivity and CORS configuration
- Test development tools functionality
- Test hot reload and development features

## Dependencies / Preconditions

- Supporting services must be running (Keycloak)
- Backend must be running and accessible
- Node.js and npm must be available
- Network connectivity between services must be established