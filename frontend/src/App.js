import './App.css';
import { useEffect, useState } from 'react';
import { BrowserRouter } from 'react-router-dom';

import { ErrorBoundary, Layout } from './modules/shared';
import { NotificationProvider } from './modules/shared/contexts/NotificationContext';
import DevPanel from './components/DevTools/DevPanel';
import { initKeycloak } from './config/keycloak';
import AppRoutes from './routes/AppRoutes';

function App() {
  const [keycloakInitialized, setKeycloakInitialized] = useState(false);
  const [initError, setInitError] = useState(null);
  const apiUrl = process.env.REACT_APP_API_URL || '/api';
  const currentUserRole = process.env.REACT_APP_USER_ROLE || 'ADMIN';
  const currentUserId = process.env.REACT_APP_USER_ID
    || (currentUserRole === 'ADMIN'
      ? '22222222-2222-2222-2222-222222222222'
      : '11111111-1111-1111-1111-111111111111');

  useEffect(() => {
    initKeycloak()
      .then(() => setKeycloakInitialized(true))
      .catch((error) => {
        setInitError(error.message);
        setKeycloakInitialized(true);
      });
  }, []);

  if (!keycloakInitialized) {
    return <div className="app-loading">Initializing authentication...</div>;
  }

  return (
    <ErrorBoundary>
      <NotificationProvider>
        <BrowserRouter>
          <Layout userRole={currentUserRole}>
            {initError && process.env.REACT_APP_DEBUG_MODE === 'true' ? (
              <div className="app-dev-warning">Authentication fallback active: {initError}</div>
            ) : null}
            <AppRoutes apiUrl={apiUrl} userId={currentUserId} userRole={currentUserRole} />
          </Layout>
          <DevPanel />
        </BrowserRouter>
      </NotificationProvider>
    </ErrorBoundary>
  );
}

export default App;
