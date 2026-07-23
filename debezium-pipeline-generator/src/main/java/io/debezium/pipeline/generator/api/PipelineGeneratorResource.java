package io.debezium.pipeline.generator.api;

import io.debezium.pipeline.generator.model.*;
import io.debezium.pipeline.generator.service.*;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.*;
import java.util.stream.Collectors;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PipelineGeneratorResource {

    @Inject
    ConnectorConfigBuilder configBuilder;

    @Inject
    TopicNamingStrategyBuilder namingBuilder;

    @Inject
    TransformChainBuilder transformBuilder;

    @Inject
    DeploymentGenerator deploymentGenerator;

    @Inject
    SchemaIntrospectionService schemaIntrospection;

    @Inject
    MappingSuggestionEngine mappingEngine;

    @Inject
    EmbeddingService embeddingService;

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of(
            "status", "UP",
            "service", "debezium-pipeline-generator",
            "version", "3.7.0-SNAPSHOT"
        )).build();
    }

    @GET
    @Path("/connectors")
    public Response listConnectors() {
        List<Map<String, String>> connectors = Arrays.stream(ConnectorType.values())
            .map(t -> Map.of(
                "id", t.getId(),
                "name", t.getDisplayName(),
                "class", t.getConnectorClass()
            ))
            .collect(Collectors.toList());
        return Response.ok(connectors).build();
    }

    @GET
    @Path("/connectors/{type}/schema")
    public Response getConnectorSchema(@PathParam("type") String type) {
        try {
            ConnectorType connectorType = ConnectorType.fromId(type);
            ConnectorConfigBuilder.ConfigSchema schema = configBuilder.getConfigSchema(connectorType);
            return Response.ok(schema).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Unknown connector type: " + type))
                .build();
        }
    }

    @POST
    @Path("/connectors/{type}/validate")
    public Response validateConnectorConfig(@PathParam("type") String type, Map<String, String> config) {
        try {
            ConnectorType connectorType = ConnectorType.fromId(type);
            ConnectorConfigBuilder.ValidationResult result = configBuilder.validateConfig(connectorType, config);
            return Response.ok(result).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Unknown connector type: " + type))
                .build();
        }
    }

    @POST
    @Path("/pipelines/generate")
    public Response generatePipeline(PipelineGenerationRequest request) {
        try {
            Log.infof("Generating pipeline: %s", request.name());

            // Build source connector
            ConnectorConfig sourceConnector = configBuilder.build(
                request.source().type(),
                request.source().config(),
                request.kafka(),
                request.schemaRegistry()
            );

            // Build sink connector if target provided
            ConnectorConfig sinkConnector = null;
            if (request.target() != null) {
                sinkConnector = configBuilder.build(
                    request.target().type(),
                    request.target().config(),
                    request.kafka(),
                    request.schemaRegistry()
                );
            }

            // Build naming strategy
            TopicNamingStrategyBuilder.NamingStrategy namingStrategy = namingBuilder.build(
                request.topicNamingStrategy(),
                request.kafka().topicPrefix(),
                request.topicNamingPlaceholders()
            );

            // Build transformation chain
            TransformChainBuilder.TransformChain transformChain = transformBuilder.build(
                request.transformation()
            );

            // Build pipeline spec
            PipelineSpec spec = PipelineSpec.builder()
                .id(UUID.randomUUID().toString())
                .name(request.name())
                .description(request.description())
                .sourceConnector(sourceConnector)
                .sinkConnector(sinkConnector)
                .mappings(request.mappings() != null ? request.mappings() : List.of())
                .globalTransforms(transformChain.transforms())
                .deployment(request.deployment() != null ? request.deployment() : 
                    DeploymentConfig.builder().type("strimzi").namespace("debezium").build())
                .metadata(Map.of("generatedAt", String.valueOf(System.currentTimeMillis())))
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .status(PipelineSpec.Status.DRAFT)
                .build();

            // Generate deployment artifacts
            DeploymentGenerator.DeploymentArtifacts artifacts = deploymentGenerator.generate(spec);

            PipelineGenerationResponse response = new PipelineGenerationResponse(
                spec,
                artifacts,
                namingStrategy,
                transformChain
            );

            return Response.ok(response).build();

        } catch (Exception e) {
            Log.errorf("Pipeline generation failed: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/pipelines/validate")
    public Response validatePipeline(PipelineSpec spec) {
        try {
            // Validate source connector
            ConnectorConfigBuilder.ValidationResult sourceValidation = configBuilder.validateConfig(
                ConnectorType.fromId(spec.sourceConnector().type()),
                spec.sourceConnector().config()
            );

            // Validate sink connector if present
            ConnectorConfigBuilder.ValidationResult sinkValidation = null;
            if (spec.sinkConnector() != null) {
                sinkValidation = configBuilder.validateConfig(
                    ConnectorType.fromId(spec.sinkConnector().type()),
                    spec.sinkConnector().config()
                );
            }

            boolean valid = sourceValidation.valid() && (sinkValidation == null || sinkValidation.valid());

            return Response.ok(Map.of(
                "valid", valid,
                "sourceValidation", sourceValidation,
                "sinkValidation", sinkValidation
            )).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/schema/introspect")
    public Response introspectSchema(SourceDatabaseConfig config) {
        try {
            SchemaIntrospectionService.SchemaInfo schema = schemaIntrospection.introspect(config);
            return Response.ok(schema).build();
        } catch (Exception e) {
            Log.errorf("Schema introspection failed: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/schema/tables")
    public Response listTables(
            @QueryParam("type") String type,
            @QueryParam("host") String host,
            @QueryParam("port") int port,
            @QueryParam("username") String username,
            @QueryParam("password") String password,
            @QueryParam("database") String database) {

        SourceDatabaseConfig config = SourceDatabaseConfig.builder()
            .type(type)
            .host(host)
            .port(port)
            .username(username)
            .password(password)
            .databaseName(database)
            .build();

        try {
            List<String> tables = schemaIntrospection.listTables(config);
            return Response.ok(tables).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/mappings/suggest")
    public Response suggestMappings(MappingSuggestionRequest request) {
        try {
            MappingSuggestionEngine.MappingSuggestion suggestion = mappingEngine.suggestMappings(
                request.source(),
                request.target(),
                request.mappings()
            );
            return Response.ok(suggestion).build();
        } catch (Exception e) {
            Log.errorf("Mapping suggestion failed: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/embeddings/field")
    public Response embedField(SchemaField field) {
        try {
            float[] embedding = embeddingService.embedField(field);
            return Response.ok(Map.of(
                "embedding", embedding,
                "dimensions", embedding.length
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/embeddings/similar")
    public Response findSimilar(EmbeddingSimilarityRequest request) {
        try {
            // This would need actual source/target fields - simplified for demo
            return Response.ok(Map.of(
                "message", "Use /mappings/suggest for full mapping suggestions"
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/transforms/smts")
    public Response listSMTs() {
        List<Map<String, Object>> smts = Arrays.stream(TransformConfig.Type.values())
            .map(t -> Map.of(
                "name", t.name(),
                "class", t.className(),
                "description", getSMTDescription(t)
            ))
            .collect(Collectors.toList());
        return Response.ok(smts).build();
    }

    private String getSMTDescription(TransformConfig.Type type) {
        return switch (type) {
            case EXTRACT_NEW_RECORD_STATE -> "Extracts the after state from Debezium change events";
            case FLATTEN -> "Flattens nested structures";
            case MASK_FIELD -> "Masks sensitive field values";
            case REPLACE_FIELD -> "Renames, includes, or excludes fields";
            case SET_SCHEMA -> "Sets schema metadata on records";
            case CAST -> "Casts fields to different types";
            case TIMESTAMP_CONVERTER -> "Converts timestamp formats";
            case HOIST_FIELD -> "Moves a field to the top level";
            case REGEX_ROUTER -> "Routes topics using regex";
            case TIMESTAMP_ROUTER -> "Routes topics based on timestamp";
            case OUTBOX -> "Routes outbox events";
            case CONTENT_BASED_ROUTER -> "Routes based on record content";
            default -> "Custom transformation";
        };
    }

    @GET
    @Path("/deployment/types")
    public Response listDeploymentTypes() {
        List<Map<String, String>> types = Arrays.stream(DeploymentConfig.DeploymentType.values())
            .map(t -> Map.of("id", t.value(), "name", t.name()))
            .collect(Collectors.toList());
        return Response.ok(types).build();
    }

    @GET
    @Path("/topic-naming/strategies")
    public Response listNamingStrategies() {
        List<Map<String, String>> strategies = Arrays.stream(TopicNamingStrategyBuilder.StrategyType.values())
            .map(s -> Map.of("id", s.id(), "description", s.description()))
            .collect(Collectors.toList());
        return Response.ok(strategies).build();
    }

    // Request/Response Records
    public record PipelineGenerationRequest(
        String name,
        String description,
        SourceDatabaseConfig source,
        TargetDatabaseConfig target,
        KafkaConfig kafka,
        SchemaRegistryConfig schemaRegistry,
        TopicNamingStrategyBuilder.StrategyType topicNamingStrategy,
        Map<String, String> topicNamingPlaceholders,
        TransformationConfig transformation,
        DeploymentConfig deployment,
        List<MappingRule> mappings
    ) {}

    public record PipelineGenerationResponse(
        PipelineSpec spec,
        DeploymentGenerator.DeploymentArtifacts artifacts,
        TopicNamingStrategyBuilder.NamingStrategy namingStrategy,
        TransformChainBuilder.TransformChain transformChain
    ) {}

    public record MappingSuggestionRequest(
        SourceDatabaseConfig source,
        TargetDatabaseConfig target,
        List<MappingSuggestionEngine.TableMappingRequest> mappings
    ) {}

    public record EmbeddingSimilarityRequest(
        SchemaField sourceField,
        List<SchemaField> targetFields,
        int topK
    ) {}
}