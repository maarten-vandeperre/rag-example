import { useEffect, useMemo, useState } from 'react';

import { getAuthHeader } from '../../config/keycloak';
import FailedDocumentsList from './FailedDocumentsList';
import ProcessingDocumentsList from './ProcessingDocumentsList';
import ProcessingStatistics from './ProcessingStatistics';
import './AdminProgress.css';

function AdminProgress({ apiUrl, userId, userRole, fetchImpl = fetch }) {
  const [progress, setProgress] = useState(null);
  const [loading, setLoading] = useState(userRole === 'ADMIN');
  const [error, setError] = useState('');
  const [refreshKey, setRefreshKey] = useState(0);

  const endpoint = useMemo(() => `${apiUrl}/admin/documents/progress`, [apiUrl]);

  useEffect(() => {
    if (userRole !== 'ADMIN') {
      setLoading(false);
      return undefined;
    }

    let cancelled = false;

    async function loadProgress() {
      setLoading(true);
      setError('');

      try {
        const response = await fetchImpl(endpoint, {
          headers: {
            'Content-Type': 'application/json',
            'X-User-Id': userId,
            ...getAuthHeader(),
          },
        });
        const payload = await response.json();

        if (!response.ok) {
          throw new Error(payload.message || 'Unable to load admin progress');
        }

        if (!cancelled) {
          setProgress(payload);
        }
      } catch (requestError) {
        if (!cancelled) {
          setError(requestError.message || 'Unable to load admin progress');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadProgress();

    return () => {
      cancelled = true;
    };
  }, [endpoint, fetchImpl, refreshKey, userId, userRole]);

  if (userRole !== 'ADMIN') {
    return (
      <section className="admin-progress admin-progress--denied">
        <div className="admin-progress__intro">
          <span className="admin-progress__eyebrow">Restricted area</span>
          <h2>Access denied</h2>
          <p>
            This progress screen is only available to administrators. Use the document library instead.
          </p>
        </div>
      </section>
    );
  }

  return (
    <section className="admin-progress">
      <div className="admin-progress__header">
        <div className="admin-progress__intro">
          <span className="admin-progress__eyebrow">Admin operations</span>
          <h2>Document processing progress</h2>
          <p>
            Track ingestion health across the knowledge base, review failures, and watch active processing work.
          </p>
        </div>
        <button
          className="admin-progress__refresh"
          type="button"
          onClick={() => setRefreshKey((current) => current + 1)}
        >
          Refresh data
        </button>
      </div>

      {loading ? <div className="admin-progress__state">Loading admin progress...</div> : null}
      {!loading && error ? <div className="admin-progress__state admin-progress__state--error">{error}</div> : null}

      {!loading && !error && progress ? (
        <div className="admin-progress__content">
          <ProcessingStatistics statistics={progress.statistics} />
          <div className="admin-progress__grid">
            <FailedDocumentsList failedDocuments={progress.failedDocuments} />
            <ProcessingDocumentsList processingDocuments={progress.processingDocuments} />
          </div>
        </div>
      ) : null}
    </section>
  );
}

export default AdminProgress;
