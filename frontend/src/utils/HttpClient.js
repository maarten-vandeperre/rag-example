import { mapHttpError, toApiError } from '../services/ErrorHandler';

const DEFAULT_TIMEOUT_MS = 20000;

function mergeHeaders(...headerSets) {
  return headerSets.reduce((result, headers) => {
    if (!headers) {
      return result;
    }

    Object.entries(headers).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        result[key] = value;
      }
    });

    return result;
  }, {});
}

function parseBody(response, responseType = 'json') {
  if (response.status === 204) {
    return Promise.resolve(null);
  }

  if (responseType === 'text') {
    return response.text();
  }

  return response.text().then((text) => {
    if (!text) {
      return null;
    }

    try {
      return JSON.parse(text);
    } catch (error) {
      return text;
    }
  });
}

class HttpClient {
  constructor(options = {}) {
    this.baseUrl = options.baseUrl || process.env.REACT_APP_API_URL || '/api';
    this.timeoutMs = options.timeoutMs || DEFAULT_TIMEOUT_MS;
    this.getAuthToken = options.getAuthToken || (() => null);
    this.getUserId = options.getUserId || (() => null);
    this.requestInterceptors = options.requestInterceptors || [];
    this.responseInterceptors = options.responseInterceptors || [];
    this.errorInterceptors = options.errorInterceptors || [];
  }

  async request(path, options = {}) {
    const timeoutMs = options.timeoutMs || this.timeoutMs;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort('timeout'), timeoutMs);

    try {
      const requestConfig = await this.applyRequestInterceptors({
        method: options.method || 'GET',
        headers: mergeHeaders(
          this.getDefaultHeaders(options),
          options.headers
        ),
        body: options.body,
        signal: options.signal || controller.signal,
        responseType: options.responseType || 'json'
      });

      const response = await fetch(`${this.baseUrl}${path}`, requestConfig);
      const payload = await parseBody(response, requestConfig.responseType);

      if (!response.ok) {
        throw mapHttpError(response, payload);
      }

      return this.applyResponseInterceptors(payload, response);
    } catch (error) {
      const normalizedError = error === 'timeout'
        ? { code: 'TIMEOUT_ERROR' }
        : (error?.name === 'AbortError' && controller.signal.reason === 'timeout'
          ? { code: 'TIMEOUT_ERROR' }
          : toApiError(error));

      throw this.applyErrorInterceptors(normalizedError);
    } finally {
      clearTimeout(timer);
    }
  }

  upload(path, options = {}) {
    const xhr = new XMLHttpRequest();
    const promise = new Promise((resolve, reject) => {
      xhr.open(options.method || 'POST', `${this.baseUrl}${path}`);

      // For FormData uploads, let the browser set Content-Type with boundary
      const headers = mergeHeaders(this.getDefaultHeaders({}), options.headers);
      Object.entries(headers).forEach(([key, value]) => {
        // Skip Content-Type for FormData - browser will set it with proper boundary
        if (options.body instanceof FormData && key.toLowerCase() === 'content-type') {
          return;
        }
        xhr.setRequestHeader(key, value);
      });

      if (typeof options.onProgress === 'function') {
        xhr.upload.onprogress = (event) => {
          if (event.lengthComputable) {
            options.onProgress({
              loaded: event.loaded,
              total: event.total,
              percent: Math.round((event.loaded / event.total) * 100)
            });
          }
        };
      }

      xhr.onload = async () => {
        try {
          const payload = xhr.responseText ? JSON.parse(xhr.responseText) : null;
          if (xhr.status < 200 || xhr.status >= 300) {
            throw mapHttpError({ status: xhr.status }, payload);
          }

          resolve(await this.applyResponseInterceptors(payload, { status: xhr.status }));
        } catch (error) {
          reject(this.applyErrorInterceptors(toApiError(error)));
        }
      };

      xhr.onerror = () => {
        reject(this.applyErrorInterceptors(toApiError(new TypeError('Network error'))));
      };

      xhr.onabort = () => {
        reject(this.applyErrorInterceptors(toApiError(new DOMException('Aborted', 'AbortError'))));
      };

      xhr.send(options.body);
    });

    return {
      cancel: () => xhr.abort(),
      promise
    };
  }

  getDefaultHeaders(options = {}) {
    const authToken = options.authToken || this.getAuthToken();
    const userId = options.userId || this.getUserId();

    return mergeHeaders(
      options.body instanceof FormData ? null : { 'Content-Type': 'application/json' },
      authToken ? { Authorization: `Bearer ${authToken}` } : null,
      userId ? { 'X-User-Id': userId } : null
    );
  }

  async applyRequestInterceptors(config) {
    let nextConfig = config;
    for (const interceptor of this.requestInterceptors) {
      nextConfig = await interceptor(nextConfig);
    }
    return nextConfig;
  }

  async applyResponseInterceptors(payload, response) {
    let nextPayload = payload;
    for (const interceptor of this.responseInterceptors) {
      nextPayload = await interceptor(nextPayload, response);
    }
    return nextPayload;
  }

  applyErrorInterceptors(error) {
    return this.errorInterceptors.reduce((nextError, interceptor) => interceptor(nextError), error);
  }
}

export default HttpClient;
