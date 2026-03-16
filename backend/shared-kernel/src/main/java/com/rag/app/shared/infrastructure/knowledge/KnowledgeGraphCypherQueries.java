package com.rag.app.shared.infrastructure.knowledge;

public final class KnowledgeGraphCypherQueries {
    private KnowledgeGraphCypherQueries() {
    }

    public static String mergeGraph() {
        return "MERGE (g:Graph {graphId: $graphId}) SET g += $properties";
    }

    public static String mergeNode() {
        return "MERGE (n:KnowledgeNode {nodeId: $nodeId}) SET n += $properties";
    }

    public static String mergeGraphContainsNode() {
        return "MATCH (g:Graph {graphId: $graphId}), (n:KnowledgeNode {nodeId: $nodeId}) MERGE (g)-[:CONTAINS_NODE]->(n)";
    }

    public static String mergeRelationship() {
        return "MATCH (a:KnowledgeNode {nodeId: $fromNodeId}), (b:KnowledgeNode {nodeId: $toNodeId}) " +
            "MERGE (a)-[r:KNOWLEDGE_RELATIONSHIP {relationshipId: $relationshipId}]->(b) SET r += $properties";
    }

    public static String findGraphById() {
        return "MATCH (g:Graph {graphId: $graphId}) RETURN g";
    }

    public static String findGraphIdByName() {
        return "MATCH (g:Graph {name: $name}) RETURN g.graphId AS graphId";
    }

    public static String findAllGraphIds() {
        return "MATCH (g:Graph) RETURN g.graphId AS graphId ORDER BY g.name";
    }

    public static String graphExistsByName() {
        return "MATCH (g:Graph {name: $name}) RETURN COUNT(g) AS total";
    }

    public static String findNodesByGraphId() {
        return "MATCH (:Graph {graphId: $graphId})-[:CONTAINS_NODE]->(n:KnowledgeNode) RETURN n ORDER BY n.label";
    }

    public static String findRelationshipsByGraphId() {
        return "MATCH (:Graph {graphId: $graphId})-[:CONTAINS_NODE]->(a:KnowledgeNode)-[r:KNOWLEDGE_RELATIONSHIP]->(b:KnowledgeNode) " +
            "WHERE b.graphId = $graphId OR EXISTS((:Graph {graphId: $graphId})-[:CONTAINS_NODE]->(b)) " +
            "RETURN r, a.nodeId AS fromNodeId, b.nodeId AS toNodeId ORDER BY r.relationshipType";
    }

    public static String deleteGraph() {
        return "MATCH (g:Graph {graphId: $graphId}) OPTIONAL MATCH (g)-[:CONTAINS_NODE]->(n:KnowledgeNode) " +
            "OPTIONAL MATCH (n)-[r:KNOWLEDGE_RELATIONSHIP]-() DELETE r, n, g";
    }

    public static String findConnectedNodes() {
        return "MATCH (n:KnowledgeNode {nodeId: $nodeId})-[:KNOWLEDGE_RELATIONSHIP]-(connected:KnowledgeNode) RETURN DISTINCT connected ORDER BY connected.label";
    }

    public static String findRelationshipsByType() {
        return "MATCH (a:KnowledgeNode)-[r:KNOWLEDGE_RELATIONSHIP {relationshipType: $relationshipType}]->(b:KnowledgeNode) " +
            "RETURN r, a.nodeId AS fromNodeId, b.nodeId AS toNodeId ORDER BY r.createdAt";
    }

    public static String findSubgraphNodes() {
        return "MATCH path = (start:KnowledgeNode {nodeId: $nodeId})-[:KNOWLEDGE_RELATIONSHIP*0..$depth]-(connected:KnowledgeNode) " +
            "UNWIND nodes(path) AS node RETURN DISTINCT node ORDER BY node.label";
    }

    public static String findSubgraphRelationships() {
        return "MATCH (a:KnowledgeNode)-[r:KNOWLEDGE_RELATIONSHIP]->(b:KnowledgeNode) " +
            "WHERE a.nodeId IN $nodeIds AND b.nodeId IN $nodeIds RETURN r, a.nodeId AS fromNodeId, b.nodeId AS toNodeId ORDER BY r.createdAt";
    }
}
