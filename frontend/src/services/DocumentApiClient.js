import HttpClient from '../utils/HttpClient';
import { ApiError } from './ErrorHandler';

const MAX_FILE_SIZE_BYTES = Number(process.env.REACT_APP_MAX_FILE_SIZE || 41943040);
const SUPPORTED_EXTENSIONS = (process.env.REACT_APP_SUPPORTED_FILE_TYPES || 'pdf,md,txt')
  .split(',')
  .map((value) => value.trim().toLowerCase())
  .filter(Boolean);

function ensureUserId(options = {}) {
  if (!options.userId) {
    throw new ApiError('A user id is required to access document endpoints.', {
      code: 'MISSING_USER_ID'
    });
  }
}

function validateFile(file) {
  console.log('=== DocumentApiClient Validation ===');
  console.log('File:', file);
  console.log('SUPPORTED_EXTENSIONS:', SUPPORTED_EXTENSIONS);
  console.log('MAX_FILE_SIZE_BYTES:', MAX_FILE_SIZE_BYTES);
  
  if (!file) {
    throw new ApiError('Select a file before uploading.', {
      code: 'MISSING_FILE'
    });
  }

  const extension = file.name?.split('.').pop()?.toLowerCase();
  console.log('File name:', file.name);
  console.log('Extracted extension:', extension);
  console.log('Is extension supported:', SUPPORTED_EXTENSIONS.includes(extension));
  
  if (!extension || !SUPPORTED_EXTENSIONS.includes(extension)) {
    console.log('VALIDATION FAILED - Unsupported file type');
    throw new ApiError(`Supported file types: ${SUPPORTED_EXTENSIONS.join(', ')}.`, {
      code: 'UNSUPPORTED_FILE_TYPE'
    });
  }

  if (file.size > MAX_FILE_SIZE_BYTES) {
    console.log('VALIDATION FAILED - File too large');
    throw new ApiError('The selected file exceeds the maximum upload size.', {
      code: 'FILE_TOO_LARGE'
    });
  }
  
  console.log('VALIDATION PASSED');
}

class DocumentApiClient {
  constructor(httpClient = new HttpClient()) {
    this.httpClient = httpClient;
  }

  uploadDocument(file, onProgress, options = {}) {
    ensureUserId(options);
    validateFile(file);

    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', options.userId);

    return this.httpClient.upload('/documents/upload', {
      body: formData,
      onProgress,
      userId: options.userId,
      authToken: options.authToken
    });
  }

  async getUserDocuments(includeAll = false, options = {}) {
    ensureUserId(options);

    return this.httpClient.request(`/documents?includeAll=${includeAll ? 'true' : 'false'}`, {
      userId: options.userId,
      authToken: options.authToken
    });
  }

  async getAdminProgress(options = {}) {
    ensureUserId(options);

    return this.httpClient.request('/admin/documents/progress', {
      userId: options.userId,
      authToken: options.authToken
    });
  }
}

export default DocumentApiClient;
