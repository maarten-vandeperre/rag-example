const STATUS_CONFIG = {
  UPLOADED: { label: 'Uploaded', tone: 'uploaded', icon: 'Up' },
  PROCESSING: { label: 'Processing', tone: 'processing', icon: 'Spin' },
  READY: { label: 'Ready', tone: 'ready', icon: 'Ok' },
  FAILED: { label: 'Failed', tone: 'failed', icon: '!' },
};

function StatusBadge({ status }) {
  const config = STATUS_CONFIG[status] || { label: status, tone: 'unknown', icon: '?' };

  return (
    <span className={`status-badge status-badge--${config.tone}`}>
      <span className="status-badge__icon" aria-hidden="true">
        {config.icon}
      </span>
      {config.label}
    </span>
  );
}

export default StatusBadge;
