import { getSourceErrorMessage, getSourceErrorType, SOURCE_ERROR_TYPES } from '../../../utils/errorTypes';
import styles from './warnings.module.css';

function SourceUnavailableWarning({ error, failedSources = [], onDismiss, onRetry }) {
  const errorType = getSourceErrorType(error);
  const retryAllowed = errorType === SOURCE_ERROR_TYPES.NETWORK_ERROR || errorType === SOURCE_ERROR_TYPES.TIMEOUT;

  return (
    <div className={`${styles.card} ${styles.errorCard}`.trim()} role="alert">
      <h4 className={styles.title}>Source details unavailable</h4>
      <p className={styles.message}>{getSourceErrorMessage(error)}</p>
      {failedSources.length > 0 ? (
        <p className={styles.meta}>
          Affected source{failedSources.length > 1 ? 's' : ''}: {failedSources.map((source) => source.fileName).join(', ')}
        </p>
      ) : null}
      <div className={styles.actions}>
        {retryAllowed && onRetry ? (
          <button className={styles.button} onClick={onRetry} type="button">Try again</button>
        ) : null}
        <button className={styles.subtleButton} onClick={onDismiss} type="button">Dismiss</button>
      </div>
    </div>
  );
}

export default SourceUnavailableWarning;
