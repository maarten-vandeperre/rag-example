import { useMemo, useState } from 'react';

import { createKnowledgeGraphApi } from '../../api/knowledge-graph';
import KnowledgeGraphSearch from '../../components/knowledge-graph/KnowledgeGraphSearch';
import {
  emptyKnowledgeGraphSearchResult,
  knowledgeGraphNodeTypes,
  knowledgeGraphRelationshipTypes
} from '../../types/knowledge-graph';

function KnowledgeGraphSearchPage({ apiUrl, userId }) {
  const api = useMemo(() => createKnowledgeGraphApi({ baseUrl: apiUrl }), [apiUrl]);
  const [query, setQuery] = useState('');
  const [nodeTypes, setNodeTypes] = useState([]);
  const [relationshipTypes, setRelationshipTypes] = useState([]);
  const [results, setResults] = useState(emptyKnowledgeGraphSearchResult);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = (event) => {
    event.preventDefault();
    setLoading(true);
    setError(null);

    api.search({ query, nodeTypes, relationshipTypes, userId })
      .then((searchResults) => setResults(searchResults))
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoading(false));
  };

  return (
    <main>
      <header>
        <h1>Search knowledge graphs</h1>
        <p>Search across graphs, nodes, and relationships with type-based filters.</p>
      </header>
      <KnowledgeGraphSearch
        query={query}
        nodeTypes={nodeTypes}
        relationshipTypes={relationshipTypes}
        availableNodeTypes={knowledgeGraphNodeTypes}
        availableRelationshipTypes={knowledgeGraphRelationshipTypes}
        onQueryChange={setQuery}
        onNodeTypesChange={setNodeTypes}
        onRelationshipTypesChange={setRelationshipTypes}
        onSubmit={handleSubmit}
        results={results}
        loading={loading}
        error={error}
      />
    </main>
  );
}

export default KnowledgeGraphSearchPage;
