import HttpClient from '../utils/HttpClient';
import { ApiError } from './ErrorHandler';

export const DEFAULT_CHAT_RESPONSE_TIME_MS = 30000;
export const CHAT_REQUEST_TIMEOUT_BUFFER_MS = 5000;

class ChatApiClient {
  constructor(httpClient = new HttpClient({ timeoutMs: DEFAULT_CHAT_RESPONSE_TIME_MS + CHAT_REQUEST_TIMEOUT_BUFFER_MS })) {
    this.httpClient = httpClient;
  }

  async submitChatQuery(question, maxResponseTimeMs = DEFAULT_CHAT_RESPONSE_TIME_MS, options = {}) {
    if (!question || !question.trim()) {
      throw new ApiError('Enter a question before starting the chat.', {
        code: 'MISSING_QUESTION'
      });
    }

    const resolvedMaxResponseTimeMs = maxResponseTimeMs || DEFAULT_CHAT_RESPONSE_TIME_MS;

    return this.httpClient.request('/chat/query', {
      method: 'POST',
      body: JSON.stringify({
        question: question.trim(),
        maxResponseTimeMs: resolvedMaxResponseTimeMs
      }),
      timeoutMs: resolvedMaxResponseTimeMs + CHAT_REQUEST_TIMEOUT_BUFFER_MS,
      userId: options.userId,
      authToken: options.authToken
    });
  }

  async getAnswerSources(answerId, options = {}) {
    if (!answerId || !String(answerId).trim()) {
      throw new ApiError('Select an answer before loading sources.', {
        code: 'MISSING_ANSWER_ID'
      });
    }

    return this.httpClient.request(`/chat/answers/${answerId}/sources`, {
      method: 'GET',
      userId: options.userId,
      authToken: options.authToken
    });
  }

  async getDocumentContent(documentId, options = {}) {
    if (!documentId || !String(documentId).trim()) {
      throw new ApiError('Select a document before loading its content.', {
        code: 'MISSING_DOCUMENT_ID'
      });
    }

    return this.httpClient.request(`/documents/${documentId}/content`, {
      method: 'GET',
      userId: options.userId,
      authToken: options.authToken
    });
  }
}

export default ChatApiClient;
