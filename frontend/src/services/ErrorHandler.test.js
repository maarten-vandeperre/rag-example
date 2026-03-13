import { ApiError, mapHttpError, toApiError } from './ErrorHandler';

describe('ErrorHandler', () => {
  test('maps HTTP errors to friendly messages', () => {
    const error = mapHttpError({ status: 413 }, {});

    expect(error).toBeInstanceOf(ApiError);
    expect(error.message).toBe('The selected file is too large.');
    expect(error.status).toBe(413);
  });

  test('normalizes timeout errors', () => {
    const error = toApiError({ code: 'TIMEOUT_ERROR' });

    expect(error.message).toBe('The request timed out. Please try again.');
    expect(error.code).toBe('TIMEOUT_ERROR');
  });
});
