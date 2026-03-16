import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { createKnowledgeGraphApi } from '../../api/knowledge-graph';
import KnowledgeGraphDetail from '../../components/knowledge-graph/KnowledgeGraphDetail';

function KnowledgeGraphDetailPage({ apiUrl, userId }) {
  const { graphId } = useParams();
  const api = useMemo(() => createKnowledgeGraphApi({ baseUrl: apiUrl }), [apiUrl]);
  const [graph, setGraph] = useState(null);
  const [nodeDetail, setNodeDetail] = useState(null);
  const [subgraph, setSubgraph] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    api.getGraph(graphId, { userId })
      .then((graphPayload) => {
        if (!active) {
          return;
        }
        setGraph(graphPayload);
        const firstNodeId = graphPayload.nodes?.[0]?.nodeId;
        if (!firstNodeId) {
          return Promise.resolve();
        }
        return Promise.all([
          api.getNodeDetails(graphId, firstNodeId, { userId }),
          api.getSubgraph(graphId, firstNodeId, { userId, depth: 2 })
        ]).then(([nodePayload, subgraphPayload]) => {
          if (!active) {
            return;
          }
          setNodeDetail(nodePayload);
          setSubgraph(subgraphPayload);
        });
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
  }, [api, graphId, userId]);

  const handleInspectNode = (nodeId) => {
    Promise.all([
      api.getNodeDetails(graphId, nodeId, { userId }),
      api.getSubgraph(graphId, nodeId, { userId, depth: 2 })
    ])
      .then(([nodePayload, subgraphPayload]) => {
        setNodeDetail(nodePayload);
        setSubgraph(subgraphPayload);
      })
      .catch((requestError) => setError(requestError.message));
  };

  return (
    <main>
      <p><Link to="/admin/knowledge-graph">← Back to all knowledge graphs</Link></p>
      <KnowledgeGraphDetail
        graph={graph}
        nodeDetail={nodeDetail}
        subgraph={subgraph}
        onInspectNode={handleInspectNode}
        loading={loading}
        error={error}
      />
    </main>
  );
}

export default KnowledgeGraphDetailPage;
