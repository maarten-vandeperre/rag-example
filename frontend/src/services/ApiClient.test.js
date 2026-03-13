import ApiClient from './ApiClient';

describe('ApiClient', () => {
  beforeEach(() => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"documents":[],"totalCount":0}')
    });
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  test('delegates document requests through the shared client', async () => {
    const client = new ApiClient({ baseUrl: '/api', getUserId: () => 'user-1', getAuthToken: () => 'token-1' });

    await expect(client.getUserDocuments(false, { userId: 'user-1' })).resolves.toEqual({ documents: [], totalCount: 0 });

    expect(global.fetch).toHaveBeenCalledWith(
      '/api/documents?includeAll=false',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer token-1',
          'X-Debug-Client': 'rag-example-frontend',
          'X-User-Id': 'user-1'
        })
      })
    );
  });
});
