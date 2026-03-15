export function createAuthApi(options = {}) {
  return {
    async getCurrentUser() {
      return {
        userId: options.userId || process.env.REACT_APP_USER_ID || '11111111-1111-1111-1111-111111111111',
        role: options.userRole || process.env.REACT_APP_USER_ROLE || 'ADMIN',
        apiUrl: options.apiUrl || process.env.REACT_APP_API_URL || '/api'
      };
    }
  };
}
