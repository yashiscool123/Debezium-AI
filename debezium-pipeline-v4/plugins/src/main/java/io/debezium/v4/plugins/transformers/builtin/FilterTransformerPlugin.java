package io.debezium.v4.plugins.transformers.builtin;

import io.debezium.v4.core.spi.TransformerPlugin;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class FilterTransformerPlugin implements TransformerPlugin {

    @Override
    public String name() { return "filter"; }
    @Override
    public String version() { return "3.7.0"; }
    @Override
    public String transformerClass() { return "io.debezium.transforms.Filter"; }

    @Override
    public Map<String, Object> defaultConfig() {
        return Map.of("transforms.filter.type", "io.debezium.transforms.Filter");
    }

    @Override
    public String description() { return "Filters records based on a condition"; }
}
