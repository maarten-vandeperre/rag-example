import ChatApiClient from './ChatApiClient';
import HttpClient from '../utils/HttpClient';

describe('ChatApiClient', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  test('submits chat queries', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"answerId":"answer-1","answer":"Here is the answer","success":true,"documentReferences":[],"responseTimeMs":120}')
    });

    const client = new ChatApiClient(new HttpClient({ baseUrl: '/api' }));
    const response = await client.submitChatQuery('What changed?', 15000, { userId: 'user-1' });

    expect(response).toEqual({
      answerId: 'answer-1',
      answer: 'Here is the answer',
      success: true,
      documentReferences: [],
      responseTimeMs: 120
    });
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/chat/query',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ question: 'What changed?', maxResponseTimeMs: 15000 }),
        signal: expect.any(AbortSignal)
      })
    );
  });

  test('adds client-side timeout buffer beyond server response budget', async () => {
    const request = jest.fn().mockResolvedValue({ success: true });
    const client = new ChatApiClient({ request });

    await client.submitChatQuery('What changed?', 30000, { userId: 'user-1' });

    expect(request).toHaveBeenCalledWith('/chat/query', expect.objectContaining({
      timeoutMs: 35000,
      body: JSON.stringify({ question: 'What changed?', maxResponseTimeMs: 30000 })
    }));
  });

  test('maps timeout responses to API errors', async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 408,
      text: () => Promise.resolve('{"errorMessage":"Query exceeded the allowed response time"}')
    });

    const client = new ChatApiClient(new HttpClient({ baseUrl: '/api' }));

    await expect(client.submitChatQuery('slow question', 20000, { userId: 'user-1' })).rejects.toMatchObject({
      status: 408,
      message: 'Query exceeded the allowed response time'
    });
  });

  test('loads answer source details', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"answerId":"answer-1","sources":[{"sourceId":"source-1","fileName":"guide.pdf","available":true}],"totalSources":1,"availableSources":1}')
    });

    const client = new ChatApiClient(new HttpClient({ baseUrl: '/api' }));
    const response = await client.getAnswerSources('answer-1', { userId: 'user-1' });

    expect(response.totalSources).toBe(1);
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/chat/answers/answer-1/sources',
      expect.objectContaining({ method: 'GET' })
    );
  });

  test('loads document content', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"documentId":"doc-1","fileName":"guide.pdf","content":"Hello"}')
    });

    const client = new ChatApiClient(new HttpClient({ baseUrl: '/api' }));
    const response = await client.getDocumentContent('doc-1', { userId: 'user-1' });

    expect(response.documentId).toBe('doc-1');
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/documents/doc-1/content',
      expect.objectContaining({ method: 'GET' })
    );
  });
});
