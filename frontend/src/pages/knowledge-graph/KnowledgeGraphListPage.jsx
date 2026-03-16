import { useEffect, useMemo, useState } from 'react';

import { createKnowledgeGraphApi } from '../../api/knowledge-graph';
import KnowledgeGraphList from '../../components/knowledge-graph/KnowledgeGraphList';

function KnowledgeGraphListPage({ apiUrl, userId }) {
  const api = useMemo(() => createKnowledgeGraphApi({ baseUrl: apiUrl }), [apiUrl]);
  const [graphs, setGraphs] = useState([]);
  const [statistics, setStatistics] = useState(null);
  const [filter, setFilter] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const pageSize = 10;

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    Promise.all([
      api.listGraphs({ page, size: pageSize, userId }),
      api.getStatistics({ userId })
    ])
      .then(([graphList, graphStatistics]) => {
        if (!active) {
          return;
        }
        setGraphs(graphList);
        setStatistics(graphStatistics);
      })
      .catch((requestError) => {
        if (!active) {
          return;
        }
        setError(requestError.message);
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [api, page, userId]);

  const filteredGraphs = graphs.filter((graph) => graph.name.toLowerCase().includes(filter.toLowerCase()));

  return (
    <main>
      <header>
        <h1>Knowledge graph administration</h1>
        <p>Browse graph summaries, inspect graph structure, and explore extracted knowledge.</p>
      </header>
      <KnowledgeGraphList
        graphs={filteredGraphs}
        statistics={statistics}
        filter={filter}
        onFilterChange={setFilter}
        page={page}
        pageSize={pageSize}
        onPreviousPage={() => setPage((currentPage) => Math.max(0, currentPage - 1))}
        onNextPage={() => setPage((currentPage) => currentPage + 1)}
        loading={loading}
        error={error}
      />
    </main>
  );
}

export default KnowledgeGraphListPage;
