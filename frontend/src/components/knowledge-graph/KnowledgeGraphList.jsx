import { Link } from 'react-router-dom';

function KnowledgeGraphList({
  graphs,
  statistics,
  filter,
  onFilterChange,
  page,
  pageSize,
  onPreviousPage,
  onNextPage,
  loading,
  error
}) {
  if (loading) {
    return <p>Loading knowledge graphs...</p>;
  }

  if (error) {
    return <p role="alert">{error}</p>;
  }

  return (
    <section>
      <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
        <div>
          <strong>Total graphs:</strong> {statistics?.totalGraphs ?? graphs.length}
        </div>
        <div>
          <strong>Total nodes:</strong> {statistics?.totalNodes ?? 0}
        </div>
        <div>
          <strong>Total relationships:</strong> {statistics?.totalRelationships ?? 0}
        </div>
      </div>
      <label htmlFor="knowledge-graph-filter">
        Filter graphs
        <input
          id="knowledge-graph-filter"
          type="search"
          value={filter}
          onChange={(event) => onFilterChange(event.target.value)}
          placeholder="Search by graph name"
          style={{ display: 'block', marginTop: '0.5rem', minWidth: '18rem' }}
        />
      </label>
      <ul style={{ listStyle: 'none', padding: 0, marginTop: '1rem' }}>
        {graphs.map((graph) => (
          <li key={graph.graphId} style={{ border: '1px solid #d9d9d9', borderRadius: '8px', padding: '1rem', marginBottom: '0.75rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem', alignItems: 'center' }}>
              <div>
                <h2 style={{ margin: 0 }}>{graph.name}</h2>
                <p style={{ margin: '0.5rem 0 0 0' }}>
                  Nodes: {graph.totalNodes} · Relationships: {graph.totalRelationships}
                </p>
              </div>
              <Link to={`/admin/knowledge-graph/${graph.graphId}`}>Open graph</Link>
            </div>
          </li>
        ))}
      </ul>
      <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
        <button type="button" onClick={onPreviousPage} disabled={page === 0}>Previous</button>
        <span>Page {page + 1}</span>
        <button type="button" onClick={onNextPage} disabled={graphs.length < pageSize}>Next</button>
      </div>
    </section>
  );
}

export default KnowledgeGraphList;
