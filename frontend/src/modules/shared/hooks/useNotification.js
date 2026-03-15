import { useCallback } from 'react';

export function useNotification() {
  const showNotification = useCallback((message, level = 'info') => {
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('rag:notification', {
        detail: { message, level }
      }));
    }
  }, []);

  return { showNotification };
}
