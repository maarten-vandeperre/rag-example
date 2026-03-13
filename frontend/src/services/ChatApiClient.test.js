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
      text: () => Promise.resolve('{"answer":"Here is the answer","success":true,"documentReferences":[],"responseTimeMs":120}')
    });

    const client = new ChatApiClient(new HttpClient({ baseUrl: '/api' }));
    const response = await client.submitChatQuery('What changed?', 15000, { userId: 'user-1' });

    expect(response).toEqual({
      answer: 'Here is the answer',
      success: true,
      documentReferences: [],
      responseTimeMs: 120
    });
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/chat/query',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ question: 'What changed?', maxResponseTimeMs: 15000 })
      })
    );
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
});
