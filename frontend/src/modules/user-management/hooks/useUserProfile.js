import { useMemo } from 'react';

import { createAuthApi } from '../services/authApi';

export function useUserProfile(options = {}) {
  const authApi = useMemo(() => createAuthApi(options), [options]);
  const role = options.userRole || process.env.REACT_APP_USER_ROLE || 'ADMIN';
  const userId = options.userId
    || process.env.REACT_APP_USER_ID
    || (role === 'ADMIN'
      ? '22222222-2222-2222-2222-222222222222'
      : '11111111-1111-1111-1111-111111111111');

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
