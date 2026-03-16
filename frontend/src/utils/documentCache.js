const cache = new Map();

export function getCachedDocument(documentId) {
  return cache.get(documentId) || null;
}

export function setCachedDocument(documentId, value) {
  cache.set(documentId, value);
}

export function clearDocumentCache() {
  cache.clear();
}

const documentCache = {
  get: getCachedDocument,
  set: setCachedDocument,
  clear: clearDocumentCache
};

export default documentCache;
