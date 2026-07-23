package io.debezium.v4.core.spi;

import io.debezium.v4.core.model.PipelineDefinition;
import java.util.Map;

public interface PipelinePlugin {
    String name();
    String version();
    String type();
    default void onBeforeCreate(PipelineDefinition pipeline) {}
    default void onAfterCreate(PipelineDefinition pipeline) {}
    default void onBeforeDeploy(PipelineDefinition pipeline) {}
    default void onAfterDeploy(PipelineDefinition pipeline) {}
    default Map<String, String> getDefaultConfig() { return Map.of(); }
    default Map<String, String> getConfigSchema() { return Map.of(); }
}
