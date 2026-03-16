import { useCallback, useMemo, useState } from 'react';

function stampEntry(entry, index) {
  return {
    ...entry,
    id: entry.id || `${entry.key || entry.type || 'entry'}-${index}`,
    timestamp: entry.timestamp || new Date().toISOString()
  };
}

function useErrorHandling() {
  const [errors, setErrors] = useState([]);
  const [warnings, setWarnings] = useState([]);

  const replaceErrors = useCallback((nextErrors = []) => {
    setErrors(nextErrors.map(stampEntry));
  }, []);

  const replaceWarnings = useCallback((nextWarnings = []) => {
    setWarnings(nextWarnings.map(stampEntry));
  }, []);

  const removeError = useCallback((errorId) => {
    setErrors((current) => current.filter((entry) => entry.id !== errorId));
  }, []);

  const removeWarning = useCallback((warningId) => {
    setWarnings((current) => current.filter((entry) => entry.id !== warningId));
  }, []);

  const clearAll = useCallback(() => {
    setErrors([]);
    setWarnings([]);
  }, []);

  return useMemo(() => ({
    errors,
    warnings,
    replaceErrors,
    replaceWarnings,
    removeError,
    removeWarning,
    clearAll
  }), [clearAll, errors, removeError, removeWarning, replaceErrors, replaceWarnings, warnings]);
}

export default useErrorHandling;
