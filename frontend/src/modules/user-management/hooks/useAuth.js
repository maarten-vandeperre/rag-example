import { useMemo } from 'react';

import { createAuthApi } from '../services/authApi';

export function useAuth(options = {}) {
  const authApi = useMemo(() => createAuthApi(options), [options]);
  const currentUserRole = options.userRole || process.env.REACT_APP_USER_ROLE || 'ADMIN';
  const currentUserId = options.userId || process.env.REACT_APP_USER_ID || '11111111-1111-1111-1111-111111111111';

  return {
    authApi,
    currentUserRole,
    currentUserId,
    isAdmin: currentUserRole === 'ADMIN'
  };
}
