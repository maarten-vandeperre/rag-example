import styles from './warnings.module.css';

function PartialSourceFailureWarning({ totalSources, availableSources, failedSources = [], onDismiss }) {
  return (
    <div className={styles.card} role="note">
      <h4 className={styles.title}>Some sources are unavailable</h4>
      <p className={styles.message}>
        {availableSources} of {totalSources} source{totalSources === 1 ? '' : 's'} can be shown right now.
      </p>
      {failedSources.length > 0 ? (
        <p className={styles.meta}>{failedSources.map((source) => source.fileName).join(', ')} could not be loaded.</p>
      ) : null}
      <div className={styles.actions}>
        <button className={styles.subtleButton} onClick={onDismiss} type="button">Continue</button>
      </div>
    </div>
  );
}

export default PartialSourceFailureWarning;
