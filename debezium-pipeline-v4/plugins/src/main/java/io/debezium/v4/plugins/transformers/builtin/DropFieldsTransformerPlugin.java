package io.debezium.v4.plugins.transformers.builtin;

import io.debezium.v4.core.spi.TransformerPlugin;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class DropFieldsTransformerPlugin implements TransformerPlugin {

    @Override
    public String name() { return "drop-fields"; }
    @Override
    public String version() { return "3.7.0"; }
    @Override
    public String transformerClass() { return "org.apache.kafka.connect.transforms.Drop"; }

    @Override
    public Map<String, Object> defaultConfig() {
        return Map.of("transforms.drop.type", "org.apache.kafka.connect.transforms.Drop");
    }

    @Override
    public String description() { return "Drops specified fields from Connect records"; }
}
