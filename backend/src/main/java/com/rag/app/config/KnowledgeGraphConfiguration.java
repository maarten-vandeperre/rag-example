package com.rag.app.config;

import com.rag.app.integration.api.controllers.KnowledgeGraphController;
import com.rag.app.shared.configuration.KnowledgeProcessingConfiguration;
import com.rag.app.shared.domain.knowledge.services.KnowledgeGraphDomainService;
import com.rag.app.integration.api.dto.knowledge.KnowledgeGraphDtoMapper;
import com.rag.app.shared.infrastructure.knowledge.HeuristicDocumentQualityValidator;
import com.rag.app.shared.infrastructure.knowledge.HeuristicKnowledgeExtractionService;
import com.rag.app.shared.infrastructure.knowledge.Neo4jConfiguration;
import com.rag.app.shared.infrastructure.knowledge.Neo4jKnowledgeGraphRepository;
import com.rag.app.shared.infrastructure.knowledge.mappers.KnowledgeGraphMapper;
import com.rag.app.shared.infrastructure.knowledge.mappers.KnowledgeNodeMapper;
import com.rag.app.shared.infrastructure.knowledge.mappers.KnowledgeRelationshipMapper;
import com.rag.app.shared.interfaces.knowledge.DocumentQualityValidator;
import com.rag.app.shared.interfaces.knowledge.KnowledgeExtractionService;
import com.rag.app.shared.usecases.knowledge.BrowseKnowledgeGraph;
import com.rag.app.shared.usecases.knowledge.BuildKnowledgeGraph;
import com.rag.app.shared.usecases.knowledge.ExtractKnowledgeFromDocument;
import com.rag.app.shared.usecases.knowledge.GetKnowledgeGraphStatistics;
import com.rag.app.shared.usecases.knowledge.SearchKnowledgeGraph;
import com.rag.app.user.interfaces.UserManagementFacade;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.neo4j.driver.Driver;

import java.time.Clock;

@ApplicationScoped
public class KnowledgeGraphConfiguration {

    @ConfigProperty(name = "neo4j.uri", defaultValue = "bolt://localhost:7687")
    String neo4jUri;

    @ConfigProperty(name = "neo4j.username", defaultValue = "neo4j")
    String neo4jUsername;

    @ConfigProperty(name = "neo4j.password", defaultValue = "dev-password")
    String neo4jPassword;

    @ConfigProperty(name = "neo4j.database", defaultValue = "neo4j")
    String neo4jDatabase;

    @Produces
    @ApplicationScoped
    public Neo4jConfiguration neo4jConfiguration() {
        return new Neo4jConfiguration(
            neo4jUri,
            neo4jUsername,
            neo4jPassword,
            neo4jDatabase,
            java.time.Duration.ofMinutes(30),
            50,
            java.time.Duration.ofMinutes(2)
        );
    }

    @Produces
    @ApplicationScoped
    public Driver neo4jDriver(Neo4jConfiguration configuration) {
        return configuration.createDriver();
    }

    @Produces
    @ApplicationScoped
    public KnowledgeGraphMapper knowledgeGraphMapper() {
        return new KnowledgeGraphMapper();
    }

    @Produces
    @ApplicationScoped
    public KnowledgeNodeMapper knowledgeNodeMapper() {
        return new KnowledgeNodeMapper();
    }

    @Produces
    @ApplicationScoped
    public KnowledgeRelationshipMapper knowledgeRelationshipMapper() {
        return new KnowledgeRelationshipMapper();
    }

    @Produces
    @ApplicationScoped
    public Neo4jKnowledgeGraphRepository knowledgeGraphRepository(
            Driver driver,
            Neo4jConfiguration configuration,
            KnowledgeGraphMapper graphMapper,
            KnowledgeNodeMapper nodeMapper,
            KnowledgeRelationshipMapper relationshipMapper) {
        return new Neo4jKnowledgeGraphRepository(driver, configuration, graphMapper, nodeMapper, relationshipMapper);
    }

    @Produces
    @ApplicationScoped
    public BrowseKnowledgeGraph browseKnowledgeGraph(Neo4jKnowledgeGraphRepository repository) {
        return new BrowseKnowledgeGraph(repository);
    }

    @Produces
    @ApplicationScoped
    public SearchKnowledgeGraph searchKnowledgeGraph(Neo4jKnowledgeGraphRepository repository) {
        return new SearchKnowledgeGraph(repository);
    }

    @Produces
    @ApplicationScoped
    public GetKnowledgeGraphStatistics getKnowledgeGraphStatistics(Neo4jKnowledgeGraphRepository repository) {
        return new GetKnowledgeGraphStatistics(repository);
    }

    @Produces
    @ApplicationScoped
    public KnowledgeProcessingConfiguration knowledgeProcessingConfiguration() {
        return new KnowledgeProcessingConfiguration();
    }

    @Produces
    @ApplicationScoped
    public KnowledgeGraphDomainService knowledgeGraphDomainService() {
        return new KnowledgeGraphDomainService();
    }

    @Produces
    @ApplicationScoped
    public KnowledgeExtractionService knowledgeExtractionService() {
        return new HeuristicKnowledgeExtractionService();
    }

    @Produces
    @ApplicationScoped
    public DocumentQualityValidator documentQualityValidator() {
        return new HeuristicDocumentQualityValidator();
    }

    @Produces
    @ApplicationScoped
    public ExtractKnowledgeFromDocument extractKnowledgeFromDocument(KnowledgeExtractionService knowledgeExtractionService,
                                                                     DocumentQualityValidator documentQualityValidator) {
        return new ExtractKnowledgeFromDocument(knowledgeExtractionService, documentQualityValidator, Clock.systemUTC());
    }

    @Produces
    @ApplicationScoped
    public BuildKnowledgeGraph buildKnowledgeGraph(Neo4jKnowledgeGraphRepository repository,
                                                   KnowledgeGraphDomainService knowledgeGraphDomainService) {
        return new BuildKnowledgeGraph(repository, knowledgeGraphDomainService, Clock.systemUTC());
    }

    @Produces
    @ApplicationScoped
    public KnowledgeGraphDtoMapper knowledgeGraphDtoMapper() {
        return new KnowledgeGraphDtoMapper();
    }

    @Produces
    @ApplicationScoped
    public KnowledgeGraphController knowledgeGraphController(
            BrowseKnowledgeGraph browseKnowledgeGraph,
            SearchKnowledgeGraph searchKnowledgeGraph,
            GetKnowledgeGraphStatistics getKnowledgeGraphStatistics,
            KnowledgeGraphDtoMapper dtoMapper,
            UserManagementFacade userManagementFacade) {
        return new KnowledgeGraphController(
            browseKnowledgeGraph,
            searchKnowledgeGraph,
            getKnowledgeGraphStatistics,
            dtoMapper,
            userManagementFacade
        );
    }
}
