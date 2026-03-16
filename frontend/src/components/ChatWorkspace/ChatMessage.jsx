import DocumentReference from './DocumentReference';

function ChatMessage({ message, onAnswerSelect, isSelected = false }) {
  const isAnswerSelectable = message.role === 'assistant' && !message.loading && !message.isSystem;

  const handleAnswerSelect = () => {
    if (isAnswerSelectable) {
      onAnswerSelect?.(message);
    }
  };

  const handleAnswerKeyDown = (event) => {
    if (!isAnswerSelectable) {
      return;
    }

    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      onAnswerSelect?.(message);
    }
  };

  return (
    <article className={`chat-message chat-message-${message.role} ${isSelected ? 'chat-message-selected' : ''}`.trim()}>
      <header className="chat-message-header">
        <span className="chat-message-role">{message.role === 'user' ? 'You' : 'Workspace AI'}</span>
        <time className="chat-message-time">{message.timestamp}</time>
      </header>
      {isAnswerSelectable ? (
        <div
          aria-label="View answer details"
          className="chat-message-body chat-message-body-interactive"
          onClick={handleAnswerSelect}
          onKeyDown={handleAnswerKeyDown}
          role="button"
          tabIndex={0}
        >
          {message.content}
        </div>
      ) : <p className="chat-message-body">{message.content}</p>}
      {message.loading ? <div className="chat-loading">Searching your documents...</div> : null}
      {message.references && message.references.length > 0 ? (
        <div className="reference-list">
          {message.references.map((reference) => (
            <DocumentReference key={`${reference.documentName}-${reference.paragraphReference}`} reference={reference} />
          ))}
        </div>
      ) : null}
    </article>
  );
}

export default ChatMessage;
