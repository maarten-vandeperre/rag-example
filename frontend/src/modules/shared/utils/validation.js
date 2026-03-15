export function requireValue(value, message) {
  if (value === null || value === undefined || value === '') {
    throw new Error(message);
  }
}

export function isNonEmptyString(value) {
  return typeof value === 'string' && value.trim().length > 0;
}
