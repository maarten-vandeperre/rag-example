import DocumentReference from './DocumentReference';

function ChatMessage({ message }) {
  return (
    <article className={`chat-message chat-message-${message.role}`}>
      <header className="chat-message-header">
        <span className="chat-message-role">{message.role === 'user' ? 'You' : 'Workspace AI'}</span>
        <time className="chat-message-time">{message.timestamp}</time>
      </header>
      <p className="chat-message-body">{message.content}</p>
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
