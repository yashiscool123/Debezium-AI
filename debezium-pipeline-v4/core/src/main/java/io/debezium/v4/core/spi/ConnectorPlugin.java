package io.debezium.v4.core.spi;

import java.util.Map;

public interface ConnectorPlugin extends PipelinePlugin {
    String getConnectorClass();
    Map<String, String> getRequiredConfig();
    Map<String, String> getOptionalConfig();
    default String type() { return "connector"; }
}
