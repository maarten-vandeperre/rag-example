import NoSourcesWarning from './warnings/NoSourcesWarning';
import PartialSourceFailureWarning from './warnings/PartialSourceFailureWarning';
import SourceUnavailableWarning from './warnings/SourceUnavailableWarning';
import styles from './warnings/warnings.module.css';

function renderWarning(warning, handlers) {
  switch (warning.type) {
    case 'NO_SOURCES':
      return <NoSourcesWarning onDismiss={() => handlers.onDismissWarning(warning.id)} />;
    case 'PARTIAL_FAILURE':
      return (
        <PartialSourceFailureWarning
          availableSources={warning.availableSources}
          failedSources={warning.failedSources}
          onDismiss={() => handlers.onDismissWarning(warning.id)}
          totalSources={warning.totalSources}
        />
      );
    default:
      return null;
  }
}

function renderError(error, handlers) {
  return (
    <SourceUnavailableWarning
      error={error}
      failedSources={error.failedSources}
      onDismiss={() => handlers.onDismissError(error.id)}
      onRetry={handlers.onRetry}
    />
  );
}

function ErrorWarningDisplay({ errors = [], warnings = [], onDismissError, onDismissWarning, onRetry }) {
  if (!errors.length && !warnings.length) {
    return null;
  }

  const handlers = { onDismissError, onDismissWarning, onRetry };

  return (
    <div className={styles.stack}>
      {warnings.map((warning) => (
        <div key={warning.id}>{renderWarning(warning, handlers)}</div>
      ))}
      {errors.map((error) => (
        <div key={error.id}>{renderError(error, handlers)}</div>
      ))}
    </div>
  );
}

export default ErrorWarningDisplay;
