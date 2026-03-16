import { useEffect, useRef } from 'react';

import styles from './DocumentViewerModal.module.css';

function DocumentViewerModal({ isOpen, title = 'Document viewer', onClose, children }) {
  const closeButtonRef = useRef(null);

  useEffect(() => {
    if (!isOpen) {
      return undefined;
    }

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    closeButtonRef.current?.focus();

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) {
    return null;
  }

  return (
    <div className={styles.backdrop} data-testid="document-viewer-backdrop" onClick={onClose}>
      <section
        aria-modal="true"
        className={styles.modal}
        onClick={(event) => event.stopPropagation()}
        role="dialog"
      >
        <header className={styles.header}>
          <h2 className={styles.title}>{title}</h2>
          <button
            aria-label="Close document viewer"
            className={styles.closeButton}
            onClick={onClose}
            ref={closeButtonRef}
            type="button"
          >
            Close
          </button>
        </header>
        <div className={styles.content}>{children}</div>
      </section>
    </div>
  );
}

export default DocumentViewerModal;
