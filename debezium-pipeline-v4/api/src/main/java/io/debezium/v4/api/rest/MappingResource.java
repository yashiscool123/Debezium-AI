package io.debezium.v4.api.rest;

import io.debezium.v4.ai.mapping.MappingEngine;
import io.debezium.v4.ai.embeddings.EmbeddingService;
import io.debezium.v4.core.model.ColumnMappingSpec;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/v4/mappings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Mappings", description = "AI-Powered Source to Target Mapping")
public class MappingResource {

    @Inject MappingEngine mappingEngine;
    @Inject EmbeddingService embeddingService;

    @POST
    @Path("/suggest")
    @Operation(summary = "AI-suggested table and column mappings")
    public Response suggestMappings(SuggestRequest request) {
        MappingEngine.MappingResult result = mappingEngine.generateMappings(
            request.sourceTables(), request.targetTables(), request.options());
        return Response.ok(result).build();
    }

    @POST
    @Path("/embed")
    @Operation(summary = "Generate embedding for field/text")
    public Response embed(EmbedRequest request) {
        float[] embedding = request.fieldName() != null
            ? embeddingService.embedField(request.fieldName(), request.dataType(), request.description())
            : embeddingService.embed(request.text());
        return Response.ok(Map.of("embedding", embedding, "dimensions", embedding.length)).build();
    }

    @POST
    @Path("/similarity")
    @Operation(summary = "Find similar fields")
    public Response findSimilar(SimilarityRequest request) {
        double sim = embeddingService.similarity(
            embeddingService.embed(request.textA()),
            embeddingService.embed(request.textB()));
        return Response.ok(Map.of("similarity", sim)).build();
    }

    public record SuggestRequest(List<MappingEngine.SchemaTable> sourceTables, List<MappingEngine.SchemaTable> targetTables, MappingEngine.MappingOptions options) {}
    public record EmbedRequest(String fieldName, String dataType, String description, String text) {}
    public record SimilarityRequest(String textA, String textB) {}
}
