import { useEffect, useState } from 'react';
import keycloak, { getUserInfo } from '../../../config/keycloak';
import AdminProgress from '../../../components/AdminProgress/AdminProgress';

function AdminPanel({ apiUrl, userId, userRole }) {
  const [authState, setAuthState] = useState({
    isAuthenticated: keycloak.authenticated,
    userInfo: getUserInfo(),
    isLoading: true
  });

  useEffect(() => {
    // In debug mode, ensure user is logged in
    if (process.env.REACT_APP_DEBUG_MODE === 'true') {
      if (!keycloak.authenticated) {
        keycloak.login();
      }
      setAuthState({
        isAuthenticated: true, // Force authenticated in debug mode
        userInfo: getUserInfo(),
        isLoading: false
      });
    } else {
      setAuthState({
        isAuthenticated: keycloak.authenticated,
        userInfo: getUserInfo(),
        isLoading: false
      });
    }
  }, []);

  const handleLogin = () => {
    keycloak.login();
    setAuthState({
      isAuthenticated: keycloak.authenticated,
      userInfo: getUserInfo(),
      isLoading: false
    });
  };

  if (authState.isLoading) {
    return (
      <main className="admin-shell">
        <section className="admin-shell__hero">
          <div>
            <span className="admin-shell__eyebrow">Loading</span>
            <h1>Initializing...</h1>
            <p>Setting up authentication...</p>
          </div>
        </section>
      </main>
    );
  }

  // Show login prompt if not authenticated (only in production mode)
  if (!authState.isAuthenticated && process.env.REACT_APP_DEBUG_MODE !== 'true') {
    return (
      <main className="admin-shell">
        <section className="admin-shell__hero">
          <div>
            <span className="admin-shell__eyebrow">Authentication Required</span>
            <h1>Please log in</h1>
            <p>You need to be logged in to access the admin panel.</p>
            <button 
              type="button" 
              onClick={handleLogin}
              style={{
                backgroundColor: '#007bff',
                color: 'white',
                border: 'none',
                padding: '0.75rem 1.5rem',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '1rem',
                marginTop: '1rem'
              }}
            >
              Log In
            </button>
          </div>
        </section>
      </main>
    );
  }

  // In debug mode or if user role is ADMIN, allow access
  const isAdmin = process.env.REACT_APP_DEBUG_MODE === 'true' 
    ? (userRole === 'ADMIN' || process.env.REACT_APP_USER_ROLE === 'ADMIN')
    : authState.userInfo?.isAdmin;

  if (!isAdmin) {
    return (
      <main className="admin-shell">
        <section className="admin-shell__hero">
          <div>
            <span className="admin-shell__eyebrow">Access Denied</span>
            <h1>Administrator access required</h1>
            <p>You need administrator privileges to access this area.</p>
            <p><small>Debug mode: {process.env.REACT_APP_DEBUG_MODE}, User role: {userRole}</small></p>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="admin-shell">
      <section className="admin-shell__hero">
        <div>
          <span className="admin-shell__eyebrow">Private knowledge base</span>
          <h1>Admin progress overview</h1>
          <p>
            Monitor ingestion health, investigate failures, and watch active document processing across the platform.
          </p>
          {process.env.REACT_APP_DEBUG_MODE === 'true' && (
            <p><small>Debug mode active - User: {userId}, Role: {userRole}</small></p>
          )}
        </div>
      </section>
      <AdminProgress apiUrl={apiUrl} userId={userId} userRole={userRole} />
    </main>
  );
}

export default AdminPanel;
