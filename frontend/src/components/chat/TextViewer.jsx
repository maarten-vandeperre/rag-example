import styles from './DocumentViewer.module.css';

function TextViewer({ html, fileType }) {
  return (
    <div className={styles.viewerSurface}>
      <div className={styles.viewerHeader}>
        <span className={styles.viewerType}>{fileType}</span>
      </div>
      <div className={styles.textViewport}>
        <pre className={styles.textContent} dangerouslySetInnerHTML={{ __html: html }} />
      </div>
    </div>
  );
}

export default TextViewer;
