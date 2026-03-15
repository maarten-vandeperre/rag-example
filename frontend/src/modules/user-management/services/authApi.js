export function createAuthApi(options = {}) {
  return {
    async getCurrentUser() {
      const role = options.userRole || process.env.REACT_APP_USER_ROLE || 'ADMIN';
      return {
        userId: options.userId
          || process.env.REACT_APP_USER_ID
          || (role === 'ADMIN' ? '22222222-2222-2222-2222-222222222222' : '11111111-1111-1111-1111-111111111111'),
        role,
        apiUrl: options.apiUrl || process.env.REACT_APP_API_URL || '/api'
      };
    }
  };
}
