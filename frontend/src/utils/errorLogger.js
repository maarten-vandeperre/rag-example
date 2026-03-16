function buildPayload(entry, context = {}) {
  return {
    timestamp: new Date().toISOString(),
    name: entry?.name || 'UnknownError',
    message: entry?.message || 'Unknown error',
    code: entry?.code || null,
    status: entry?.status || null,
    context,
    userAgent: typeof navigator === 'undefined' ? null : navigator.userAgent,
    url: typeof window === 'undefined' ? null : window.location.href
  };
}

async function sendToMonitoring(payload) {
  if (typeof fetch !== 'function') {
    return;
  }

  try {
    await fetch('/api/monitoring/errors', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    });
  } catch (monitoringError) {
    console.warn('Failed to send error to monitoring:', monitoringError);
  }
}

export function logError(error, context = {}) {
  const payload = buildPayload(error, context);

  if (process.env.NODE_ENV === 'development') {
    console.error('Answer detail error:', payload);
    return payload;
  }

  if (process.env.NODE_ENV === 'test') {
    return payload;
  }

  void sendToMonitoring(payload);
  return payload;
}

export function logWarning(warning, context = {}) {
  const payload = buildPayload(warning, context);

  if (process.env.NODE_ENV === 'development') {
    console.warn('Answer detail warning:', payload);
  }

  return payload;
}

const errorLogger = {
  logError,
  logWarning
};

export default errorLogger;
