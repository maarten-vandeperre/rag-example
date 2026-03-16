import styles from './warnings.module.css';

function NoSourcesWarning({ onDismiss }) {
  return (
    <div className={styles.card} role="note">
      <h4 className={styles.title}>No source information available</h4>
      <p className={styles.message}>
        This answer does not currently have supporting source details. The source material may be unavailable or the answer was returned without linked references.
      </p>
      <div className={styles.actions}>
        <button className={styles.button} onClick={onDismiss} type="button">Understood</button>
      </div>
    </div>
  );
}

export default NoSourcesWarning;
