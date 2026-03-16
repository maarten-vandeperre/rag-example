export const knowledgeGraphNodeTypes = [
  'CONCEPT',
  'ENTITY',
  'PERSON',
  'ORGANIZATION',
  'LOCATION',
  'EVENT',
  'DOCUMENT_SECTION',
  'TOPIC',
  'KEYWORD'
];

export const knowledgeGraphRelationshipTypes = [
  'RELATED_TO',
  'PART_OF',
  'MENTIONS',
  'DEFINED_IN',
  'SIMILAR_TO',
  'DEPENDS_ON',
  'CONTAINS',
  'REFERENCES',
  'DERIVED_FROM'
];

export const emptyKnowledgeGraphSearchResult = {
  query: '',
  nodes: [],
  relationships: [],
  graphs: [],
  totalResults: 0
};
