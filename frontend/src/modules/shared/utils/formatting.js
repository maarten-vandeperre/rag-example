export function formatRole(role) {
  return String(role || 'STANDARD').toLowerCase();
}

export function formatTimestamp(value) {
  if (!value) {
    return '';
  }

  return new Date(value).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}
