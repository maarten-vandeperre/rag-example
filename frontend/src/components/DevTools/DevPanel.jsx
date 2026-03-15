import { useEffect, useState } from 'react';

import keycloak, { getUserInfo } from '../../config/keycloak';
import { checkBackendHealth } from '../../services/ApiClient';

function DevPanel() {
  const [isOpen, setIsOpen] = useState(false);
  const [backendHealth, setBackendHealth] = useState(null);
  const [userInfo, setUserInfo] = useState(null);

  useEffect(() => {
    if (process.env.REACT_APP_SHOW_DEV_TOOLS !== 'true') {
      return;
    }

    setUserInfo(getUserInfo());
    checkBackendHealth().then(setBackendHealth).catch((error) => {
      setBackendHealth({ error: error.message });
    });
  }, []);

  if (process.env.REACT_APP_SHOW_DEV_TOOLS !== 'true') {
    return null;
  }

  return (
    <aside className="dev-panel">
      <button type="button" onClick={() => setIsOpen((current) => !current)}>
        {isOpen ? 'Hide' : 'Show'} Dev Tools
      </button>
      {isOpen ? (
        <div className="dev-panel__content">
          <div><strong>Environment:</strong> {process.env.REACT_APP_ENVIRONMENT}</div>
          <div><strong>Backend URL:</strong> {process.env.REACT_APP_BACKEND_URL}</div>
          <div><strong>Keycloak URL:</strong> {process.env.REACT_APP_KEYCLOAK_URL}</div>
          <div><strong>Authenticated:</strong> {keycloak.authenticated ? 'Yes' : 'No'}</div>
          <div><strong>Debug Mode:</strong> {process.env.REACT_APP_DEBUG_MODE}</div>
          <div><strong>Supported File Types:</strong> {process.env.REACT_APP_SUPPORTED_FILE_TYPES}</div>
          <div><strong>Max File Size:</strong> {process.env.REACT_APP_MAX_FILE_SIZE}</div>
          <pre>{JSON.stringify(backendHealth, null, 2)}</pre>
          <pre>{JSON.stringify(userInfo, null, 2)}</pre>
          <div>{keycloak.token ? `Token: ${keycloak.token.slice(0, 20)}...` : 'No token'}</div>
          <div className="dev-panel__actions">
            <button type="button" onClick={() => {
              keycloak.login();
              setUserInfo(getUserInfo());
            }}>Login</button>
            <button type="button" onClick={() => {
              keycloak.logout();
              setUserInfo(getUserInfo());
            }}>Logout</button>
            <button type="button" onClick={() => window.location.reload()}>Reload</button>
          </div>
        </div>
      ) : null}
    </aside>
  );
}

export default DevPanel;
