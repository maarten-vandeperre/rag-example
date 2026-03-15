import { useMemo } from 'react';

import { createAuthApi } from '../services/authApi';

export function useUserProfile(options = {}) {
  const authApi = useMemo(() => createAuthApi(options), [options]);
  const userId = options.userId || process.env.REACT_APP_USER_ID || '11111111-1111-1111-1111-111111111111';
  const role = options.userRole || process.env.REACT_APP_USER_ROLE || 'ADMIN';

  return {
    authApi,
    profile: {
      userId,
      username: role.toLowerCase(),
      role,
      email: `${role.toLowerCase()}@example.com`
    }
  };
}
