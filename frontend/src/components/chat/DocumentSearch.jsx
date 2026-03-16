import styles from './DocumentViewer.module.css';

function DocumentSearch({ searchTerm, totalMatches, currentMatchIndex, onSearchChange, onPrevious, onNext }) {
  return (
    <div className={styles.searchBar}>
      <label className={styles.searchLabel} htmlFor="document-search-input">Search in document</label>
      <div className={styles.searchControls}>
        <input
          className={styles.searchInput}
          id="document-search-input"
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder="Search in document..."
          type="search"
          value={searchTerm}
        />
        <span aria-live="polite" className={styles.searchSummary}>
          {totalMatches > 0 ? `${currentMatchIndex + 1} of ${totalMatches}` : 'No matches'}
        </span>
        <button className={styles.searchButton} disabled={totalMatches === 0} onClick={onPrevious} type="button">
          Previous
        </button>
        <button className={styles.searchButton} disabled={totalMatches === 0} onClick={onNext} type="button">
          Next
        </button>
      </div>
    </div>
  );
}

export default DocumentSearch;
