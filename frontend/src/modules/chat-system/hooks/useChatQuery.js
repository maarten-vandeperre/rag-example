import { useCallback, useMemo, useState } from 'react';

import { useNotification } from '../../shared/hooks/useNotification';
import { createChatApi } from '../services/chatApi';

export function useChatQuery(options = {}) {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const chatApi = useMemo(() => createChatApi(options), [options]);
  const { showNotification } = useNotification();

  const submitQuery = useCallback(async (question, maxResponseTimeMs, requestOptions = {}) => {
    setLoading(true);
    try {
      const payload = await chatApi.submitQuery(question, maxResponseTimeMs, requestOptions);
      setMessages((current) => [...current, payload]);
      return payload;
    } catch (error) {
      showNotification('Failed to process query', 'error');
      throw error;
    } finally {
      setLoading(false);
    }
  }, [chatApi, showNotification]);

  return { messages, loading, submitQuery, clearMessages: () => setMessages([]) };
}
