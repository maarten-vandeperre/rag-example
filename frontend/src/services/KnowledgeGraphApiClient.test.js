import HttpClient from '../utils/HttpClient';
import KnowledgeGraphApiClient from './KnowledgeGraphApiClient';

describe('KnowledgeGraphApiClient', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  test('lists graphs and unwraps api responses', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve(JSON.stringify({ success: true, data: [{ graphId: 'graph-1', name: 'Main Graph' }] }))
    });

    const client = new KnowledgeGraphApiClient(new HttpClient({ baseUrl: '/api' }));

    await expect(client.listGraphs({ userId: 'admin-1', page: 1, size: 5 })).resolves.toEqual([{ graphId: 'graph-1', name: 'Main Graph' }]);
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/knowledge-graph/graphs?page=1&size=5',
      expect.objectContaining({
        method: 'GET',
        headers: expect.objectContaining({ 'X-User-Id': 'admin-1' })
      })
    );
  });

  test('searches graphs with filters and unwraps errors', async () => {
    global.fetch
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        text: () => Promise.resolve(JSON.stringify({ success: true, data: { query: 'neo4j', nodes: [], relationships: [], graphs: [], totalResults: 0 } }))
      })
      .mockResolvedValueOnce({
        ok: false,
        status: 403,
        text: () => Promise.resolve(JSON.stringify({ success: false, error: { code: 'ADMIN_ACCESS_REQUIRED', message: 'Admin access required' } }))
      });

    const client = new KnowledgeGraphApiClient(new HttpClient({ baseUrl: '/api' }));

    await expect(client.search({
      query: 'neo4j',
      nodeTypes: ['ENTITY'],
      relationshipTypes: ['RELATED_TO'],
      userId: 'admin-1'
    })).resolves.toEqual({ query: 'neo4j', nodes: [], relationships: [], graphs: [], totalResults: 0 });

    await expect(client.getStatistics({ userId: 'user-1' })).rejects.toMatchObject({ code: 'ADMIN_ACCESS_REQUIRED' });
    expect(global.fetch).toHaveBeenNthCalledWith(
      1,
      '/api/knowledge-graph/search?query=neo4j&nodeTypes=ENTITY&relationshipTypes=RELATED_TO&page=0&size=20',
      expect.any(Object)
    );
  });
});
