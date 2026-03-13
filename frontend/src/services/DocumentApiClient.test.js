import DocumentApiClient from './DocumentApiClient';
import HttpClient from '../utils/HttpClient';

class MockXMLHttpRequest {
  static instances = [];

  constructor() {
    this.headers = {};
    this.upload = {};
    MockXMLHttpRequest.instances.push(this);
  }

  open(method, url) {
    this.method = method;
    this.url = url;
  }

  setRequestHeader(key, value) {
    this.headers[key] = value;
  }

  send(body) {
    this.body = body;
  }

  respond(status, payload) {
    this.status = status;
    this.responseText = JSON.stringify(payload);
    this.onload();
  }

  abort() {
    this.onabort();
  }
}

describe('DocumentApiClient', () => {
  const originalXMLHttpRequest = global.XMLHttpRequest;

  beforeEach(() => {
    MockXMLHttpRequest.instances = [];
    global.fetch = jest.fn();
    global.XMLHttpRequest = MockXMLHttpRequest;
  });

  afterEach(() => {
    global.XMLHttpRequest = originalXMLHttpRequest;
    jest.resetAllMocks();
  });

  test('uploads documents with progress tracking and cancellation', async () => {
    const client = new DocumentApiClient(new HttpClient({ baseUrl: '/api' }));
    const onProgress = jest.fn();
    const request = client.uploadDocument(
      new File(['hello'], 'notes.txt', { type: 'text/plain' }),
      onProgress,
      { userId: '7f1d7b11-90c2-45ff-8a7b-cbe13c852bd7' }
    );

    const xhr = MockXMLHttpRequest.instances[0];
    xhr.upload.onprogress({ lengthComputable: true, loaded: 5, total: 10 });
    xhr.respond(201, { documentId: 'doc-1', status: 'UPLOADED' });

    await expect(request.promise).resolves.toEqual({ documentId: 'doc-1', status: 'UPLOADED' });
    expect(onProgress).toHaveBeenCalledWith({ loaded: 5, total: 10, percent: 50 });

    const cancellableRequest = client.uploadDocument(
      new File(['hello'], 'notes.txt', { type: 'text/plain' }),
      jest.fn(),
      { userId: '7f1d7b11-90c2-45ff-8a7b-cbe13c852bd7' }
    );
    cancellableRequest.cancel();

    await expect(cancellableRequest.promise).rejects.toMatchObject({ code: 'REQUEST_ABORTED' });
  });

  test('fetches user documents and admin progress', async () => {
    global.fetch
      .mockResolvedValueOnce({ ok: true, status: 200, text: () => Promise.resolve('{"totalCount":1,"documents":[]}') })
      .mockResolvedValueOnce({ ok: true, status: 200, text: () => Promise.resolve('{"statistics":{"totalDocuments":1}}') });

    const client = new DocumentApiClient(new HttpClient({ baseUrl: '/api' }));

    await expect(client.getUserDocuments(true, { userId: 'admin-id' })).resolves.toEqual({ totalCount: 1, documents: [] });
    await expect(client.getAdminProgress({ userId: 'admin-id' })).resolves.toEqual({ statistics: { totalDocuments: 1 } });

    expect(global.fetch).toHaveBeenNthCalledWith(
      1,
      '/api/documents?includeAll=true',
      expect.objectContaining({
        method: 'GET',
        headers: expect.objectContaining({ 'X-User-Id': 'admin-id' })
      })
    );
  });
});
