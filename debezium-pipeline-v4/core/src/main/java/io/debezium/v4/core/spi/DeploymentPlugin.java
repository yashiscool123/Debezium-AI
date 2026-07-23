package io.debezium.v4.core.spi;

import java.util.Map;

public interface DeploymentPlugin extends PipelinePlugin {
    String generate(Map<String, Object> pipelineConfig);
    default String type() { return "deployment"; }
}
