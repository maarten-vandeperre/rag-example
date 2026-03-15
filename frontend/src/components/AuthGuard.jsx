import { useEffect, useState } from 'react';
import keycloak, { getUserInfo } from '../config/keycloak';
import './AuthGuard.css';

function AuthGuard({ children, requireAdmin = false }) {
  const [isAuthenticated, setIsAuthenticated] = useState(keycloak.authenticated);
  const [userInfo, setUserInfo] = useState(getUserInfo());

  useEffect(() => {
    // Check authentication status
    const checkAuth = () => {
      setIsAuthenticated(keycloak.authenticated);
      setUserInfo(getUserInfo());
    };

    // If not authenticated and in debug mode, auto-login
    if (!keycloak.authenticated && process.env.REACT_APP_DEBUG_MODE === 'true') {
      keycloak.login();
      checkAuth();
    }

    checkAuth();
  }, []);

  // Show login prompt if not authenticated
  if (!isAuthenticated) {
    return (
      <div className="auth-guard">
        <div className="auth-guard__content">
          <h2>Authentication Required</h2>
          <p>Please log in to access this area.</p>
          <button 
            type="button" 
            onClick={() => {
              keycloak.login();
              setIsAuthenticated(keycloak.authenticated);
              setUserInfo(getUserInfo());
            }}
            className="auth-guard__login-btn"
          >
            Log In
          </button>
        </div>
      </div>
    );
  }

  // Check admin access if required
  if (requireAdmin && !userInfo?.isAdmin) {
    return (
      <div className="auth-guard">
        <div className="auth-guard__content">
          <h2>Access Denied</h2>
          <p>Administrator privileges are required to access this area.</p>
        </div>
      </div>
    );
  }

  return children;
}

export default AuthGuard;