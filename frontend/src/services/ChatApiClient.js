import HttpClient from '../utils/HttpClient';
import { ApiError } from './ErrorHandler';

const DEFAULT_TIMEOUT_MS = 20000;

class ChatApiClient {
  constructor(httpClient = new HttpClient({ timeoutMs: DEFAULT_TIMEOUT_MS })) {
    this.httpClient = httpClient;
  }

  async submitChatQuery(question, maxResponseTimeMs = DEFAULT_TIMEOUT_MS, options = {}) {
    if (!question || !question.trim()) {
      throw new ApiError('Enter a question before starting the chat.', {
        code: 'MISSING_QUESTION'
      });
    }

    return this.httpClient.request('/chat/query', {
      method: 'POST',
      body: JSON.stringify({
        question: question.trim(),
        maxResponseTimeMs
      }),
      timeoutMs: maxResponseTimeMs,
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
