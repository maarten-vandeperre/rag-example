import { Link } from 'react-router-dom';

function KnowledgeGraphSearch({
  query,
  nodeTypes,
  relationshipTypes,
  availableNodeTypes,
  availableRelationshipTypes,
  onQueryChange,
  onNodeTypesChange,
  onRelationshipTypesChange,
  onSubmit,
  results,
  loading,
  error
}) {
  return (
    <section>
      <form onSubmit={onSubmit} style={{ display: 'grid', gap: '1rem', marginBottom: '1rem' }}>
        <label htmlFor="knowledge-search-query">
          Search query
          <input id="knowledge-search-query" type="search" value={query} onChange={(event) => onQueryChange(event.target.value)} />
        </label>
        <label htmlFor="knowledge-search-node-types">
          Node types
          <select id="knowledge-search-node-types" multiple value={nodeTypes} onChange={(event) => onNodeTypesChange(Array.from(event.target.selectedOptions).map((option) => option.value))}>
            {availableNodeTypes.map((nodeType) => <option key={nodeType} value={nodeType}>{nodeType}</option>)}
          </select>
        </label>
        <label htmlFor="knowledge-search-relationship-types">
          Relationship types
          <select id="knowledge-search-relationship-types" multiple value={relationshipTypes} onChange={(event) => onRelationshipTypesChange(Array.from(event.target.selectedOptions).map((option) => option.value))}>
            {availableRelationshipTypes.map((relationshipType) => <option key={relationshipType} value={relationshipType}>{relationshipType}</option>)}
          </select>
        </label>
        <button type="submit" disabled={loading}>Search knowledge graphs</button>
      </form>

      {loading ? <p>Searching knowledge graphs...</p> : null}
      {error ? <p role="alert">{error}</p> : null}
      {!loading && !error ? (
        <div>
          <p><strong>Total results:</strong> {results.totalResults}</p>
          <h2>Matching graphs</h2>
          <ul>
            {results.graphs.map((graph) => (
              <li key={graph.graphId}><Link to={`/admin/knowledge-graph/${graph.graphId}`}>{graph.name}</Link></li>
            ))}
          </ul>
          <h2>Matching nodes</h2>
          <ul>
            {results.nodes.map((node) => (
              <li key={node.nodeId}>{node.label} ({node.nodeType})</li>
            ))}
          </ul>
          <h2>Matching relationships</h2>
          <ul>
            {results.relationships.map((relationship) => (
              <li key={relationship.relationshipId}>{relationship.relationshipType}: {relationship.fromNodeId} → {relationship.toNodeId}</li>
            ))}
          </ul>
        </div>
      ) : null}
    </section>
  );
}

export default KnowledgeGraphSearch;
