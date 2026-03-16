import { useMemo, useRef } from 'react';

import styles from './SourceSelector.module.css';

function sourceLabel(source, index) {
  return `${index + 1}. ${source.fileName}${source.available ? '' : ' unavailable'}`;
}

function nextEnabledIndex(sources, currentIndex, step) {
  const total = sources.length;

  for (let offset = 1; offset <= total; offset += 1) {
    const candidate = (currentIndex + (offset * step) + total) % total;
    if (sources[candidate]?.available) {
      return candidate;
    }
  }

  return currentIndex;
}

function SourceSelector({ sources, selectedSource, onSourceSelect, loading = false, error = '', onRetry }) {
  const buttonRefs = useRef([]);
  const selectedIndex = useMemo(
    () => sources.findIndex((source) => source.sourceId === selectedSource?.sourceId),
    [selectedSource?.sourceId, sources]
  );

  const handleKeyDown = (event, index, source) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      if (source.available) {
        onSourceSelect(source);
      }
      return;
    }

    if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
      event.preventDefault();
      buttonRefs.current[nextEnabledIndex(sources, index, 1)]?.focus();
      return;
    }

    if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
      event.preventDefault();
      buttonRefs.current[nextEnabledIndex(sources, index, -1)]?.focus();
      return;
    }

    if (event.key === 'Home') {
      event.preventDefault();
      buttonRefs.current[nextEnabledIndex(sources, -1, 1)]?.focus();
      return;
    }

    if (event.key === 'End') {
      event.preventDefault();
      buttonRefs.current[nextEnabledIndex(sources, 0, -1)]?.focus();
    }
  };

  if (loading) {
    return (
      <div aria-live="polite" className={styles.stateCard} role="status">
        <p className={styles.stateTitle}>Loading sources</p>
        <p className={styles.stateMessage}>Fetching source details for this answer.</p>
      </div>
    );
  }

  if (error) {
    return (
      <div aria-live="assertive" className={styles.errorCard} role="alert">
        <p className={styles.stateTitle}>Unable to load all sources</p>
        <p className={styles.stateMessage}>{error}</p>
        {onRetry ? (
          <button className={styles.retryButton} onClick={onRetry} type="button">
            Try again
          </button>
        ) : null}
      </div>
    );
  }

  if (!sources.length) {
    return (
      <div className={styles.stateCard}>
        <p className={styles.stateTitle}>No sources available</p>
        <p className={styles.stateMessage}>This answer does not have any supporting sources yet.</p>
      </div>
    );
  }

  return (
    <div aria-label="Available sources" className={styles.tabList} role="tablist">
      {sources.map((source, index) => {
        const isSelected = (selectedIndex >= 0 ? selectedIndex : 0) === index;

        return (
          <button
            aria-label={sourceLabel(source, index)}
            aria-selected={isSelected}
            className={`${styles.tab} ${isSelected ? styles.tabSelected : ''} ${!source.available ? styles.tabDisabled : ''}`.trim()}
            disabled={!source.available}
            key={source.sourceId}
            onClick={() => onSourceSelect(source)}
            onKeyDown={(event) => handleKeyDown(event, index, source)}
            ref={(element) => {
              buttonRefs.current[index] = element;
            }}
            role="tab"
            tabIndex={isSelected ? 0 : -1}
            type="button"
          >
            <span className={styles.badge}>{index + 1}</span>
            <span className={styles.meta}>
              <span className={styles.title}>{source.fileName}</span>
              <span className={styles.subtitle}>{source.paragraphReference}</span>
            </span>
            {!source.available ? <span className={styles.warning}>Unavailable</span> : null}
          </button>
        );
      })}
    </div>
  );
}

export default SourceSelector;
