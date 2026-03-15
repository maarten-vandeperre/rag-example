const keycloakState = {
  authenticated: false,
  token: null,
  tokenParsed: null,
  config: null,
  initOptions: null,
  login() {
    this.authenticated = true;
    this.token = 'dev-token';
    const role = process.env.REACT_APP_USER_ROLE || 'ADMIN';
    this.tokenParsed = {
      sub: process.env.REACT_APP_USER_ID || (role === 'ADMIN' ? '22222222-2222-2222-2222-222222222222' : '11111111-1111-1111-1111-111111111111'),
      preferred_username: process.env.REACT_APP_DEV_USERNAME || (role === 'ADMIN' ? 'jane.admin' : 'john.doe'),
      email: process.env.REACT_APP_DEV_EMAIL || (role === 'ADMIN' ? 'jane.admin@example.com' : 'john.doe@example.com'),
      given_name: role === 'ADMIN' ? 'Jane' : 'John',
      family_name: role === 'ADMIN' ? 'Admin' : 'Doe',
      realm_access: {
        roles: [role]
      }
    };
  },
  logout() {
    this.authenticated = false;
    this.token = null;
    this.tokenParsed = null;
  },
  updateToken() {
    return Promise.resolve(false);
  }
};

export const keycloakConfig = {
  url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8180',
  realm: process.env.REACT_APP_KEYCLOAK_REALM || 'rag-app-dev',
  clientId: process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'rag-app-frontend'
};

export async function initKeycloak() {
  keycloakState.config = keycloakConfig;
  keycloakState.initOptions = {
    onLoad: 'check-sso',
    silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    pkceMethod: 'S256',
    checkLoginIframe: false
  };

  if ((process.env.REACT_APP_DEBUG_MODE || 'false') === 'true') {
    keycloakState.login();
  }

  return keycloakState;
}

export function getAuthHeader() {
  return keycloakState.token ? { Authorization: `Bearer ${keycloakState.token}` } : {};
}

export function getUserInfo() {
  if (!keycloakState.tokenParsed) {
    return null;
  }

  return {
    userId: keycloakState.tokenParsed.sub,
    username: keycloakState.tokenParsed.preferred_username,
    email: keycloakState.tokenParsed.email,
    firstName: keycloakState.tokenParsed.given_name,
    lastName: keycloakState.tokenParsed.family_name,
    roles: keycloakState.tokenParsed.realm_access?.roles || [],
    isAdmin: (keycloakState.tokenParsed.realm_access?.roles || []).includes('ADMIN')
  };
}

export default keycloakState;
