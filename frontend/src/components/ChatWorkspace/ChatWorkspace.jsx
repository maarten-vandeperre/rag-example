import { useMemo, useState } from 'react';

import ApiClient from '../../services/ApiClient';
import './ChatWorkspace.css';
import ChatMessage from './ChatMessage';
import QuestionInput from './QuestionInput';

const DEFAULT_USER_ID = process.env.REACT_APP_DEFAULT_USER_ID
  || (process.env.REACT_APP_USER_ROLE === 'ADMIN'
    ? '22222222-2222-2222-2222-222222222222'
    : '11111111-1111-1111-1111-111111111111');
const DEFAULT_TIMEOUT_MS = 20000;

function currentTimeLabel() {
  return new Intl.DateTimeFormat('en', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date());
}

function ChatWorkspace({ apiBaseUrl = process.env.REACT_APP_API_URL || '/api', userId = DEFAULT_USER_ID }) {
  const [messages, setMessages] = useState([]);
  const [question, setQuestion] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const apiClient = useMemo(() => new ApiClient({
    baseUrl: apiBaseUrl,
    getUserId: () => userId
  }), [apiBaseUrl, userId]);

  const introMessage = useMemo(
    () => ({
      role: 'assistant',
      content: 'Ask a question about your private knowledge base and I will answer with source references from your uploaded documents.',
      timestamp: currentTimeLabel(),
      references: []
    }),
    []
  );

  const visibleMessages = useMemo(() => [introMessage, ...messages], [introMessage, messages]);

  const handleSubmit = async () => {
    const trimmedQuestion = question.trim();

    if (!trimmedQuestion || isLoading) {
      return;
    }

    const askedAt = currentTimeLabel();
    const pendingId = `pending-${Date.now()}`;

    setError('');
    setIsLoading(true);
    setQuestion('');
    setMessages((current) => [
      ...current,
      { role: 'user', content: trimmedQuestion, timestamp: askedAt },
      { role: 'assistant', content: 'Working on your answer...', timestamp: askedAt, loading: true, references: [], id: pendingId }
    ]);

    try {
      const response = await apiClient.submitChatQuery(trimmedQuestion, DEFAULT_TIMEOUT_MS, { userId });
      setMessages((current) => {
        const withoutPending = current.filter((message) => message.id !== pendingId);

        if (!response.success) {
          setError(response.errorMessage || 'Unable to process chat query');
          return withoutPending;
        }

        return [
          ...withoutPending,
          {
            role: 'assistant',
            content: response.answer,
            timestamp: currentTimeLabel(),
            references: response.documentReferences || []
          }
        ];
      });
    } catch (requestError) {
      setMessages((current) => current.filter((message) => message.id !== pendingId));
      setError(requestError.message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <section className="workspace-shell">
      <header className="workspace-hero">
        <span className="workspace-eyebrow">Private Knowledge Base</span>
        <h1>Chat with the documents your team trusts.</h1>
        <p>
          Review answers, inspect source references, and keep every response anchored to your uploaded files.
        </p>
      </header>

      <div className="workspace-board">
        <div className="workspace-messages">
          {visibleMessages.map((message, index) => (
            <ChatMessage key={`${message.timestamp}-${index}`} message={message} />
          ))}
          {error ? <div className="workspace-error">{error}</div> : null}
        </div>

        <QuestionInput value={question} onChange={setQuestion} onSubmit={handleSubmit} disabled={isLoading} />
      </div>
    </section>
  );
}

export default ChatWorkspace;
