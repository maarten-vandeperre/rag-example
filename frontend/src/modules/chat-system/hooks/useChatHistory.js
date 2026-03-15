import { useState } from 'react';

export function useChatHistory(initialMessages = []) {
  const [messages, setMessages] = useState(initialMessages);

  return {
    messages,
    appendMessage: (message) => setMessages((current) => [...current, message]),
    clearMessages: () => setMessages([])
  };
}
