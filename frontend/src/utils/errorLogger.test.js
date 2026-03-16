import { logError, logWarning } from './errorLogger';

test('returns structured payloads for logged errors and warnings', () => {
  const errorPayload = logError({ message: 'Source load failed', code: 'NETWORK_ERROR' }, { answerId: 'answer-1' });
  const warningPayload = logWarning({ message: 'No sources', code: 'NO_SOURCES' }, { answerId: 'answer-2' });

  expect(errorPayload.message).toBe('Source load failed');
  expect(errorPayload.code).toBe('NETWORK_ERROR');
  expect(errorPayload.context.answerId).toBe('answer-1');

  expect(warningPayload.message).toBe('No sources');
  expect(warningPayload.code).toBe('NO_SOURCES');
  expect(warningPayload.context.answerId).toBe('answer-2');
});
