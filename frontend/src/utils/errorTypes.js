export const SOURCE_ERROR_TYPES = {
  NETWORK_ERROR: 'NETWORK_ERROR',
  TIMEOUT: 'TIMEOUT',
  PERMISSION_DENIED: 'PERMISSION_DENIED',
  NOT_FOUND: 'NOT_FOUND',
  UNKNOWN: 'UNKNOWN'
};

export function getSourceErrorType(error) {
  if (error?.code === 'NETWORK_ERROR') {
    return SOURCE_ERROR_TYPES.NETWORK_ERROR;
  }

  if (error?.code === 'TIMEOUT_ERROR' || error?.status === 408) {
    return SOURCE_ERROR_TYPES.TIMEOUT;
  }

  if (error?.status === 401 || error?.status === 403) {
    return SOURCE_ERROR_TYPES.PERMISSION_DENIED;
  }

  if (error?.status === 404) {
    return SOURCE_ERROR_TYPES.NOT_FOUND;
  }

  return SOURCE_ERROR_TYPES.UNKNOWN;
}

export function getSourceErrorMessage(error) {
  switch (getSourceErrorType(error)) {
    case SOURCE_ERROR_TYPES.NETWORK_ERROR:
      return 'Unable to load source details because the service is unreachable. Check your connection and try again.';
    case SOURCE_ERROR_TYPES.TIMEOUT:
      return 'Source details are taking too long to load. Please try again.';
    case SOURCE_ERROR_TYPES.PERMISSION_DENIED:
      return 'You do not have permission to access these source details.';
    case SOURCE_ERROR_TYPES.NOT_FOUND:
      return 'The requested source details could not be found.';
    default:
      return error?.message || 'An unexpected source error occurred.';
  }
}
