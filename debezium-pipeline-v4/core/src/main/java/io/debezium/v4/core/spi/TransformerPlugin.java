package io.debezium.v4.core.spi;

import java.util.Map;

public interface TransformerPlugin extends PipelinePlugin {
    String transform(String sourceColumn, String targetColumn, String sourceType, String targetType, Map<String, String> config);
    default String type() { return "transformer"; }
}
