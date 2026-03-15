import ChatMessage from '../../../components/ChatWorkspace/ChatMessage';

function MessageList({ messages }) {
  return messages.map((message, index) => (
    <ChatMessage key={`${message.timestamp || 'message'}-${index}`} message={message} />
  ));
}

export default MessageList;
