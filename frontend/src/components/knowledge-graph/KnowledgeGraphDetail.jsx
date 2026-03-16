function KnowledgeGraphDetail({ graph, nodeDetail, subgraph, onInspectNode, loading, error }) {
  if (loading) {
    return <p>Loading knowledge graph details...</p>;
  }

  if (error) {
    return <p role="alert">{error}</p>;
  }

  if (!graph) {
    return <p>No graph selected.</p>;
  }

  return (
    <section>
      <header style={{ marginBottom: '1rem' }}>
        <p style={{ margin: 0 }}>Knowledge graph / {graph.name}</p>
        <h1 style={{ marginBottom: '0.25rem' }}>{graph.name}</h1>
        <p style={{ marginTop: 0 }}>{graph.metadata?.description || 'No description available.'}</p>
      </header>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem', marginBottom: '1rem' }}>
        <article style={{ border: '1px solid #d9d9d9', borderRadius: '8px', padding: '1rem' }}>
          <h2 style={{ marginTop: 0 }}>Metadata</h2>
          <pre style={{ whiteSpace: 'pre-wrap', margin: 0 }}>{JSON.stringify(graph.metadata?.attributes || {}, null, 2)}</pre>
        </article>
        <article style={{ border: '1px solid #d9d9d9', borderRadius: '8px', padding: '1rem' }}>
          <h2 style={{ marginTop: 0 }}>Subgraph</h2>
          <p style={{ margin: 0 }}>Nodes: {subgraph?.nodes?.length ?? 0}</p>
          <p style={{ margin: '0.5rem 0 0 0' }}>Relationships: {subgraph?.relationships?.length ?? 0}</p>
        </article>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1rem' }}>
        <article>
          <h2>Nodes</h2>
          <table>
            <thead>
              <tr>
                <th>Label</th>
                <th>Type</th>
                <th>Confidence</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {graph.nodes.map((node) => (
                <tr key={node.nodeId}>
                  <td>{node.label}</td>
                  <td>{node.nodeType}</td>
                  <td>{node.confidence}</td>
                  <td><button type="button" onClick={() => onInspectNode(node.nodeId)}>Inspect</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </article>

        <article>
          <h2>Relationships</h2>
          <table>
            <thead>
              <tr>
                <th>Type</th>
                <th>From</th>
                <th>To</th>
              </tr>
            </thead>
            <tbody>
              {graph.relationships.map((relationship) => (
                <tr key={relationship.relationshipId}>
                  <td>{relationship.relationshipType}</td>
                  <td>{relationship.fromNodeId}</td>
                  <td>{relationship.toNodeId}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </article>
      </div>

      {nodeDetail ? (
        <article style={{ marginTop: '1rem', border: '1px solid #d9d9d9', borderRadius: '8px', padding: '1rem' }}>
          <h2 style={{ marginTop: 0 }}>Node detail</h2>
          <p><strong>{nodeDetail.node.label}</strong> ({nodeDetail.node.nodeType})</p>
          <p>Connections: {nodeDetail.connectionCount}</p>
          <p>Relationship types: {nodeDetail.relationshipTypes.join(', ') || 'None'}</p>
        </article>
      ) : null}
    </section>
  );
}

export default KnowledgeGraphDetail;
