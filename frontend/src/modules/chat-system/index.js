import ChatWorkspace from './components/ChatWorkspace';
import MessageList from './components/MessageList';
import QueryInput from './components/QueryInput';
import { useChatQuery } from './hooks/useChatQuery';
import { useChatHistory } from './hooks/useChatHistory';

export { ChatWorkspace, MessageList, QueryInput, useChatQuery, useChatHistory };

export const ChatSystemModule = {
  name: 'chat-system',
  version: '1.0.0',
  components: { ChatWorkspace, MessageList, QueryInput },
  hooks: { useChatQuery, useChatHistory }
};
