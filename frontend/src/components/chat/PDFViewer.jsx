import styles from './DocumentViewer.module.css';

import TextViewer from './TextViewer';

function PDFViewer({ html }) {
  return (
    <div className={styles.viewerSurface}>
      <div className={styles.viewerHeader}>
        <span className={styles.viewerType}>PDF</span>
        <span className={styles.viewerHint}>Showing extracted text from the uploaded PDF.</span>
      </div>
      <TextViewer fileType="PDF" html={html} />
    </div>
  );
}

export default PDFViewer;
