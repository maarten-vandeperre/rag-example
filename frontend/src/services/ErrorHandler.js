const DEFAULT_MESSAGE = 'Something went wrong while talking to the knowledge base.';

const STATUS_MESSAGES = {
  400: 'Please review the request and try again.',
  401: 'Please sign in before continuing.',
  403: 'You do not have access to this resource.',
  404: 'The requested resource was not found.',
  408: 'The request took too long. Please try again.',
  413: 'The selected file is too large.',
  415: 'This file type is not supported.',
  422: 'The submitted data is invalid.',
  500: 'The server could not complete the request.',
  503: 'The service is temporarily unavailable.'
};

export class ApiError extends Error {
  constructor(message, details = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = details.status ?? null;
    this.code = details.code ?? null;
    this.details = details.details ?? null;
    this.cause = details.cause ?? null;
  }
}

export function toApiError(error) {
  if (error instanceof ApiError) {
    return error;
  }

  if (error?.name === 'AbortError') {
    return new ApiError('The request was cancelled.', {
      code: 'REQUEST_ABORTED',
      cause: error
    });
  }

  if (error?.code === 'TIMEOUT_ERROR') {
    return new ApiError('The request timed out. Please try again.', {
      code: 'TIMEOUT_ERROR',
      cause: error
    });
  }

  if (error?.name === 'TypeError') {
    return new ApiError('The service is unreachable. Check your connection and try again.', {
      code: 'NETWORK_ERROR',
      cause: error
    });
  }

  return new ApiError(error?.message || DEFAULT_MESSAGE, {
    cause: error
  });
}

export function mapHttpError(response, payload) {
  const message = payload?.message || payload?.errorMessage || STATUS_MESSAGES[response.status] || DEFAULT_MESSAGE;

  return new ApiError(message, {
    status: response.status,
    code: payload?.error || payload?.code || null,
    details: payload
  });
}

const ErrorHandler = {
  ApiError,
  mapHttpError,
  toApiError
};

export default ErrorHandler;
