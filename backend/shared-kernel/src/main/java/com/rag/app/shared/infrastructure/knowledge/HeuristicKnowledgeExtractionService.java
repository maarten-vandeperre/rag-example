package com.rag.app.shared.infrastructure.knowledge;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;
import com.rag.app.shared.domain.knowledge.valueobjects.ConfidenceScore;
import com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference;
import com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge;
import com.rag.app.shared.domain.knowledge.valueobjects.ExtractionMetadata;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeType;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipType;
import com.rag.app.shared.interfaces.knowledge.KnowledgeExtractionException;
import com.rag.app.shared.interfaces.knowledge.KnowledgeExtractionService;
import com.rag.app.shared.interfaces.knowledge.UnsupportedDocumentFormatException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HeuristicKnowledgeExtractionService implements KnowledgeExtractionService {
    private static final Set<String> SUPPORTED_DOCUMENT_TYPES = Set.of("PDF", "MARKDOWN", "PLAIN_TEXT", "TEXT", "TXT");
    private static final Set<String> TECHNICAL_KEYWORDS = Set.of(
        "knowledge", "graph", "pipeline", "search", "entity", "relationship", "document", "upload",
        "semantic", "retrieval", "processing", "integration", "extraction", "indexing", "classification"
    );
    private static final Pattern PERSON_PATTERN = Pattern.compile("\\b([A-Z][a-z]+\\s+[A-Z][a-z]+)\\b");
    private static final Pattern ORGANIZATION_PATTERN = Pattern.compile(
        "\\b([A-Z][A-Za-z]+(?:\\s+[A-Z][A-Za-z]+){0,3}\\s+(?:Inc|Corp|Corporation|Company|Ltd|University|Institute|Systems|Labs|Foundation|Team))\\b");
    private static final Pattern LOCATION_PATTERN = Pattern.compile(
        "\\b(?:in|at|from|near|inside|across)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){0,2})\\b");
    private static final Pattern WORKS_FOR_PATTERN = Pattern.compile(
        "\\b([A-Z][a-z]+\\s+[A-Z][a-z]+)\\s+(?:works?|worked|serves?|served|joined|leads?)\\s+(?:at|for)\\s+([A-Z][A-Za-z]+(?:\\s+[A-Z][A-Za-z]+){0,3}\\s+(?:Inc|Corp|Corporation|Company|Ltd|University|Institute|Systems|Labs|Foundation|Team))\\b");
    private static final Pattern LOCATED_IN_PATTERN = Pattern.compile(
        "\\b([A-Z][A-Za-z]+(?:\\s+[A-Z][A-Za-z]+){0,3})\\s+(?:is\\s+)?(?:located|based|operates?)\\s+(?:in|at)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){0,2})\\b");

    private final Map<String, Object> defaultExtractionOptions;

    public HeuristicKnowledgeExtractionService() {
        this(Map.of(
            "extract_entities", true,
            "extract_relationships", true,
            "extract_sections", true,
            "strategy", "balanced",
            "min_confidence", 0.55d,
            "chunk_size", 1200,
            "max_entities", 120
        ));
    }

    public HeuristicKnowledgeExtractionService(Map<String, Object> defaultExtractionOptions) {
        this.defaultExtractionOptions = Map.copyOf(Objects.requireNonNull(defaultExtractionOptions, "defaultExtractionOptions cannot be null"));
    }

    @Override
    public ExtractedKnowledge extractKnowledge(String documentContent,
                                               String documentTitle,
                                               String documentType,
                                               Map<String, Object> extractionOptions) throws KnowledgeExtractionException {
        if (documentContent == null || documentContent.isBlank()) {
            throw new KnowledgeExtractionException("documentContent cannot be null or blank");
        }
        if (documentTitle == null || documentTitle.isBlank()) {
            throw new KnowledgeExtractionException("documentTitle cannot be null or blank");
        }
        if (!supportsDocumentType(documentType)) {
            throw new UnsupportedDocumentFormatException("Unsupported document type: " + documentType);
        }

        Instant startedAt = Instant.now();
        Map<String, Object> mergedOptions = new LinkedHashMap<>(defaultExtractionOptions);
        mergedOptions.putAll(Objects.requireNonNull(extractionOptions, "extractionOptions cannot be null"));

        try {
            double minConfidence = readDouble(mergedOptions, "min_confidence", 0.55d);
            int chunkSize = readInteger(mergedOptions, "chunk_size", 1200);
            int maxEntities = readInteger(mergedOptions, "max_entities", 120);
            boolean extractEntities = readBoolean(mergedOptions, "extract_entities", true);
            boolean extractRelationships = readBoolean(mergedOptions, "extract_relationships", true);
            boolean extractSections = readBoolean(mergedOptions, "extract_sections", true);
            String strategy = String.valueOf(mergedOptions.getOrDefault("strategy", "balanced")).toLowerCase(Locale.ROOT);

            DocumentReference sourceDocument = new DocumentReference(
                UUID.nameUUIDFromBytes((documentTitle + ":" + documentType).getBytes(StandardCharsets.UTF_8)),
                documentTitle,
                null,
                0.85d
            );
            List<String> warnings = new ArrayList<>();
            List<Section> sections = segmentDocument(documentContent, documentType, chunkSize);
            if (sections.size() > 1) {
                warnings.add("Document processed in " + sections.size() + " chunks for extraction");
            }

            Map<String, KnowledgeNode> nodesByKey = new LinkedHashMap<>();
            Map<String, String> nodeKeyByLabel = new LinkedHashMap<>();
            List<KnowledgeRelationship> relationships = new ArrayList<>();
            Set<String> relationshipKeys = new LinkedHashSet<>();

            int processedSections = 0;
            for (Section section : sections) {
                if ("fast".equals(strategy) && processedSections >= 5) {
                    warnings.add("Fast extraction strategy limited section analysis to the first 5 sections");
                    break;
                }
                processedSections++;

                KnowledgeNode sectionNode = null;
                if (extractSections) {
                    sectionNode = upsertNode(nodesByKey, nodeKeyByLabel, section.title(), NodeType.DOCUMENT_SECTION,
                        Map.of("sectionIndex", section.index(), "strategy", strategy), section.documentReference(sourceDocument),
                        new ConfidenceScore(0.95d), minConfidence, startedAt);
                }

                if (!extractEntities) {
                    continue;
                }

                List<KnowledgeNode> sectionEntities = new ArrayList<>();
                sectionEntities.addAll(extractPatternEntities(section.text(), NodeType.PERSON, PERSON_PATTERN, 0.84d, section, sourceDocument,
                    Map.of("category", "person"), nodesByKey, nodeKeyByLabel, minConfidence, startedAt));
                sectionEntities.addAll(extractPatternEntities(section.text(), NodeType.ORGANIZATION, ORGANIZATION_PATTERN, 0.82d, section, sourceDocument,
                    Map.of("category", "organization"), nodesByKey, nodeKeyByLabel, minConfidence, startedAt));
                sectionEntities.addAll(extractPatternEntities(section.text(), NodeType.LOCATION, LOCATION_PATTERN, 0.72d, section, sourceDocument,
                    Map.of("category", "location"), nodesByKey, nodeKeyByLabel, minConfidence, startedAt));
                sectionEntities.addAll(extractTechnicalConcepts(section, sourceDocument, nodesByKey, nodeKeyByLabel, minConfidence, startedAt));

                if (nodesByKey.size() > maxEntities) {
                    warnings.add("Entity extraction capped at " + maxEntities + " nodes");
                    break;
                }

                if (sectionNode != null && extractRelationships) {
                    for (KnowledgeNode entityNode : sectionEntities) {
                        addRelationship(relationships, relationshipKeys, sectionNode.nodeId(), entityNode.nodeId(), RelationshipType.MENTIONS,
                            Map.of("predicate", "mentions", "sectionTitle", section.title()), section.documentReference(sourceDocument),
                            0.68d, minConfidence, startedAt);
                    }
                }

                if (extractRelationships) {
                    extractExplicitRelationships(section, sourceDocument, nodesByKey, nodeKeyByLabel, relationships, relationshipKeys, minConfidence, startedAt);
                    if (!"fast".equals(strategy)) {
                        extractCoOccurrenceRelationships(section, nodeKeyByLabel, nodesByKey, relationships, relationshipKeys, sourceDocument, minConfidence, startedAt);
                    }
                }
            }

            if (nodesByKey.isEmpty()) {
                warnings.add("No entities satisfied the configured extraction thresholds");
            }
            if (relationships.isEmpty()) {
                warnings.add("No relationships were extracted from the document");
            }

            Instant completedAt = Instant.now();
            return new ExtractedKnowledge(
                List.copyOf(nodesByKey.values()),
                List.copyOf(relationships),
                sourceDocument,
                new ExtractionMetadata(
                    "heuristic-pattern-extraction",
                    completedAt,
                    Duration.between(startedAt, completedAt),
                    Map.copyOf(mergedOptions),
                    List.copyOf(warnings)
                )
            );
        } catch (KnowledgeExtractionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new KnowledgeExtractionException("Heuristic extraction failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public boolean supportsDocumentType(String documentType) {
        return documentType != null && SUPPORTED_DOCUMENT_TYPES.contains(documentType.trim().toUpperCase(Locale.ROOT));
    }

    @Override
    public List<String> getSupportedExtractionTypes() {
        return List.of("entities", "relationships", "sections", "co_occurrence");
    }

    @Override
    public Map<String, Object> getDefaultExtractionOptions() {
        return defaultExtractionOptions;
    }

    private List<Section> segmentDocument(String content, String documentType, int chunkSize) {
        String normalizedType = documentType.trim().toUpperCase(Locale.ROOT);
        List<Section> sections = new ArrayList<>();

        if ("MARKDOWN".equals(normalizedType)) {
            String[] lines = content.split("\\R");
            String currentTitle = "Document Overview";
            StringBuilder currentText = new StringBuilder();
            int sectionIndex = 1;
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.startsWith("#")) {
                    if (!currentText.isEmpty()) {
                        sections.add(new Section(sectionIndex++, currentTitle, cleanText(currentText.toString()), normalizedType));
                        currentText = new StringBuilder();
                    }
                    currentTitle = line.replaceFirst("^#+\\s*", "").trim();
                } else {
                    currentText.append(line).append(' ');
                }
            }
            if (!currentText.isEmpty()) {
                sections.add(new Section(sectionIndex, currentTitle, cleanText(currentText.toString()), normalizedType));
            }
        }

        if (sections.isEmpty()) {
            String cleaned = cleanText(content);
            int chunkIndex = 0;
            for (int start = 0; start < cleaned.length(); start += chunkSize) {
                int end = Math.min(cleaned.length(), start + chunkSize);
                sections.add(new Section(chunkIndex + 1, "Section " + (chunkIndex + 1), cleaned.substring(start, end), normalizedType));
                chunkIndex++;
            }
        }

        return sections.stream().filter(section -> !section.text().isBlank()).toList();
    }

    private List<KnowledgeNode> extractPatternEntities(String text,
                                                       NodeType nodeType,
                                                       Pattern pattern,
                                                       double baseConfidence,
                                                       Section section,
                                                       DocumentReference sourceDocument,
                                                       Map<String, Object> defaultProperties,
                                                       Map<String, KnowledgeNode> nodesByKey,
                                                       Map<String, String> nodeKeyByLabel,
                                                       double minConfidence,
                                                       Instant createdAt) {
        List<KnowledgeNode> nodes = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String label = matcher.group(1).trim();
            if (label.length() < 3 || label.split("\\s+").length > 5) {
                continue;
            }
            double confidence = Math.min(0.98d, baseConfidence + occurrenceBoost(text, label));
            KnowledgeNode node = upsertNode(nodesByKey, nodeKeyByLabel, label, nodeType,
                mergeProperties(defaultProperties, Map.of("sectionTitle", section.title(), "occurrences", countOccurrences(text, label))),
                section.documentReference(sourceDocument), new ConfidenceScore(confidence), minConfidence, createdAt);
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private List<KnowledgeNode> extractTechnicalConcepts(Section section,
                                                         DocumentReference sourceDocument,
                                                         Map<String, KnowledgeNode> nodesByKey,
                                                         Map<String, String> nodeKeyByLabel,
                                                         double minConfidence,
                                                         Instant createdAt) {
        Map<String, Integer> phraseCounts = new LinkedHashMap<>();
        String normalized = section.text().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s-]", " ");
        String[] tokens = normalized.split("\\s+");
        for (int index = 0; index < tokens.length - 1; index++) {
            String first = tokens[index];
            String second = tokens[index + 1];
            if (TECHNICAL_KEYWORDS.contains(first) || TECHNICAL_KEYWORDS.contains(second)) {
                String phrase = toTitleCase(first + " " + second);
                phraseCounts.merge(phrase, 1, Integer::sum);
            }
        }

        List<KnowledgeNode> nodes = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : phraseCounts.entrySet()) {
            double confidence = Math.min(0.9d, 0.58d + (entry.getValue() * 0.06d));
            KnowledgeNode node = upsertNode(nodesByKey, nodeKeyByLabel, entry.getKey(), NodeType.CONCEPT,
                Map.of("sectionTitle", section.title(), "technicalTerm", true, "occurrences", entry.getValue()),
                section.documentReference(sourceDocument), new ConfidenceScore(confidence), minConfidence, createdAt);
            if (node != null) {
                nodes.add(node);
            }
        }

        if (!section.title().equalsIgnoreCase("Document Overview")) {
            KnowledgeNode topicNode = upsertNode(nodesByKey, nodeKeyByLabel, section.title(), NodeType.TOPIC,
                Map.of("origin", "heading", "sectionIndex", section.index()), section.documentReference(sourceDocument),
                ConfidenceScore.high(), minConfidence, createdAt);
            if (topicNode != null) {
                nodes.add(topicNode);
            }
        }
        return nodes;
    }

    private void extractExplicitRelationships(Section section,
                                              DocumentReference sourceDocument,
                                              Map<String, KnowledgeNode> nodesByKey,
                                              Map<String, String> nodeKeyByLabel,
                                              List<KnowledgeRelationship> relationships,
                                              Set<String> relationshipKeys,
                                              double minConfidence,
                                              Instant createdAt) {
        Matcher worksForMatcher = WORKS_FOR_PATTERN.matcher(section.text());
        while (worksForMatcher.find()) {
            addPredicateRelationship(worksForMatcher.group(1), NodeType.PERSON, worksForMatcher.group(2), NodeType.ORGANIZATION,
                "works_for", section, sourceDocument, nodesByKey, nodeKeyByLabel, relationships, relationshipKeys, 0.86d, minConfidence, createdAt);
        }

        Matcher locatedInMatcher = LOCATED_IN_PATTERN.matcher(section.text());
        while (locatedInMatcher.find()) {
            addPredicateRelationship(locatedInMatcher.group(1), NodeType.ORGANIZATION, locatedInMatcher.group(2), NodeType.LOCATION,
                "located_in", section, sourceDocument, nodesByKey, nodeKeyByLabel, relationships, relationshipKeys, 0.74d, minConfidence, createdAt);
        }
    }

    private void extractCoOccurrenceRelationships(Section section,
                                                  Map<String, String> nodeKeyByLabel,
                                                  Map<String, KnowledgeNode> nodesByKey,
                                                  List<KnowledgeRelationship> relationships,
                                                  Set<String> relationshipKeys,
                                                  DocumentReference sourceDocument,
                                                  double minConfidence,
                                                  Instant createdAt) {
        String[] sentences = section.text().split("(?<=[.!?])\\s+");
        for (String sentence : sentences) {
            List<KnowledgeNode> sentenceNodes = nodesByKey.values().stream()
                .filter(node -> node.nodeType() != NodeType.DOCUMENT_SECTION)
                .filter(node -> sentence.toLowerCase(Locale.ROOT).contains(node.label().toLowerCase(Locale.ROOT)))
                .limit(4)
                .toList();

            for (int index = 0; index < sentenceNodes.size(); index++) {
                for (int targetIndex = index + 1; targetIndex < sentenceNodes.size(); targetIndex++) {
                    KnowledgeNode fromNode = sentenceNodes.get(index);
                    KnowledgeNode toNode = sentenceNodes.get(targetIndex);
                    addRelationship(relationships, relationshipKeys, fromNode.nodeId(), toNode.nodeId(), RelationshipType.RELATED_TO,
                        Map.of("predicate", "co_occurs", "sentence", sentence.trim()), section.documentReference(sourceDocument),
                        0.61d, minConfidence, createdAt);
                }
            }
        }
    }

    private void addPredicateRelationship(String fromLabel,
                                          NodeType fromType,
                                          String toLabel,
                                          NodeType toType,
                                          String predicate,
                                          Section section,
                                          DocumentReference sourceDocument,
                                          Map<String, KnowledgeNode> nodesByKey,
                                          Map<String, String> nodeKeyByLabel,
                                          List<KnowledgeRelationship> relationships,
                                          Set<String> relationshipKeys,
                                          double confidence,
                                          double minConfidence,
                                          Instant createdAt) {
        KnowledgeNode fromNode = findOrCreateNode(nodesByKey, nodeKeyByLabel, fromLabel, fromType, section, sourceDocument, Math.min(confidence, 0.92d), minConfidence, createdAt);
        KnowledgeNode toNode = findOrCreateNode(nodesByKey, nodeKeyByLabel, toLabel, toType, section, sourceDocument, Math.min(confidence - 0.04d, 0.88d), minConfidence, createdAt);
        if (fromNode != null && toNode != null) {
            addRelationship(relationships, relationshipKeys, fromNode.nodeId(), toNode.nodeId(), RelationshipType.RELATED_TO,
                Map.of("predicate", predicate, "sectionTitle", section.title()), section.documentReference(sourceDocument), confidence, minConfidence, createdAt);
        }
    }

    private KnowledgeNode findOrCreateNode(Map<String, KnowledgeNode> nodesByKey,
                                           Map<String, String> nodeKeyByLabel,
                                           String label,
                                           NodeType type,
                                           Section section,
                                           DocumentReference sourceDocument,
                                           double confidence,
                                           double minConfidence,
                                           Instant createdAt) {
        String key = nodeKey(type, label);
        if (nodesByKey.containsKey(key)) {
            return nodesByKey.get(key);
        }
        return upsertNode(nodesByKey, nodeKeyByLabel, label, type,
            Map.of("sectionTitle", section.title(), "generated", true), section.documentReference(sourceDocument),
            new ConfidenceScore(Math.max(0.0d, Math.min(1.0d, confidence))), minConfidence, createdAt);
    }

    private KnowledgeNode upsertNode(Map<String, KnowledgeNode> nodesByKey,
                                     Map<String, String> nodeKeyByLabel,
                                     String label,
                                     NodeType nodeType,
                                     Map<String, Object> properties,
                                     DocumentReference sourceDocument,
                                     ConfidenceScore confidence,
                                     double minConfidence,
                                     Instant createdAt) {
        if (confidence.value() < minConfidence || label == null || label.isBlank()) {
            return null;
        }

        String key = nodeKey(nodeType, label);
        KnowledgeNode existing = nodesByKey.get(key);
        if (existing == null) {
            KnowledgeNode node = KnowledgeNode.create(label, nodeType, properties, sourceDocument, confidence, createdAt);
            nodesByKey.put(key, node);
            nodeKeyByLabel.put(label.toLowerCase(Locale.ROOT), key);
            return node;
        }

        KnowledgeNode merged = existing.withProperties(properties, createdAt).withConfidence(existing.confidence().mergeWith(confidence), createdAt);
        nodesByKey.put(key, merged);
        return merged;
    }

    private void addRelationship(List<KnowledgeRelationship> relationships,
                                 Set<String> relationshipKeys,
                                 NodeId fromNodeId,
                                 NodeId toNodeId,
                                 RelationshipType relationshipType,
                                 Map<String, Object> properties,
                                 DocumentReference sourceDocument,
                                 double confidence,
                                 double minConfidence,
                                 Instant createdAt) {
        if (confidence < minConfidence || fromNodeId.equals(toNodeId)) {
            return;
        }

        String key = relationshipType.name() + ':' + fromNodeId.value() + ':' + toNodeId.value() + ':' + properties.getOrDefault("predicate", "");
        if (!relationshipKeys.add(key)) {
            return;
        }

        relationships.add(KnowledgeRelationship.create(
            fromNodeId,
            toNodeId,
            relationshipType,
            properties,
            sourceDocument,
            new ConfidenceScore(Math.min(1.0d, confidence)),
            createdAt
        ));
    }

    private Map<String, Object> mergeProperties(Map<String, Object> primary, Map<String, Object> secondary) {
        Map<String, Object> merged = new LinkedHashMap<>(primary);
        merged.putAll(secondary);
        return Map.copyOf(merged);
    }

    private String cleanText(String content) {
        return content
            .replaceAll("`{1,3}[^`]*`{1,3}", " ")
            .replaceAll("[*_>#-]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private int countOccurrences(String text, String label) {
        return Math.max(1, text.split(Pattern.quote(label), -1).length - 1);
    }

    private double occurrenceBoost(String text, String label) {
        return Math.min(0.12d, countOccurrences(text, label) * 0.02d);
    }

    private String nodeKey(NodeType type, String label) {
        return type.name() + ':' + label.trim().toLowerCase(Locale.ROOT);
    }

    private double readDouble(Map<String, Object> options, String key, double defaultValue) {
        Object value = options.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private int readInteger(Map<String, Object> options, String key, int defaultValue) {
        Object value = options.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private boolean readBoolean(Map<String, Object> options, String key, boolean defaultValue) {
        Object value = options.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String toTitleCase(String value) {
        String[] words = value.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < words.length; index++) {
            String word = words[index];
            if (word.isBlank()) {
                continue;
            }
            if (index > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    private record Section(int index, String title, String text, String documentType) {
        private DocumentReference documentReference(DocumentReference sourceDocument) {
            return new DocumentReference(sourceDocument.documentId(), sourceDocument.documentName(), title, 0.82d);
        }
    }
}
