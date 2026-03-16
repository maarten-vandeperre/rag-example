import { act, renderHook } from '@testing-library/react';

import useErrorHandling from './useErrorHandling';

test('tracks and clears errors and warnings', () => {
  const { result } = renderHook(() => useErrorHandling());

  act(() => {
    result.current.replaceErrors([{ id: 'error-1', type: 'NETWORK_ERROR', message: 'Failed' }]);
    result.current.replaceWarnings([{ id: 'warning-1', type: 'NO_SOURCES' }]);
  });

  expect(result.current.errors).toHaveLength(1);
  expect(result.current.warnings).toHaveLength(1);

  act(() => {
    result.current.removeError('error-1');
    result.current.removeWarning('warning-1');
  });

  expect(result.current.errors).toHaveLength(0);
  expect(result.current.warnings).toHaveLength(0);

  act(() => {
    result.current.replaceErrors([{ id: 'error-2', type: 'TIMEOUT' }]);
    result.current.replaceWarnings([{ id: 'warning-2', type: 'PARTIAL_FAILURE' }]);
    result.current.clearAll();
  });

  expect(result.current.errors).toHaveLength(0);
  expect(result.current.warnings).toHaveLength(0);
});
