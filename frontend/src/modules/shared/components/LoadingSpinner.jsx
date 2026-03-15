function LoadingSpinner({ label = 'Loading module...' }) {
  return (
    <div className="module-spinner" role="status" aria-live="polite">
      <span className="module-spinner__dot" />
      <span>{label}</span>
    </div>
  );
}

export default LoadingSpinner;
