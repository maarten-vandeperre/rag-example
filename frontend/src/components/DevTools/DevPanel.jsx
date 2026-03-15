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
          <pre>{JSON.stringify(backendHealth, null, 2)}</pre>
          <pre>{JSON.stringify(userInfo, null, 2)}</pre>
          <div>{keycloak.token ? `${keycloak.token.slice(0, 20)}...` : 'No token'}</div>
          <div className="dev-panel__actions">
            <button type="button" onClick={() => keycloak.login()}>Login</button>
            <button type="button" onClick={() => keycloak.logout()}>Logout</button>
            <button type="button" onClick={() => window.location.reload()}>Reload</button>
          </div>
        </div>
      ) : null}
    </aside>
  );
}

export default DevPanel;
