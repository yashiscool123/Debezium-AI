package io.debezium.v4.plugins.transformers.builtin;

import io.debezium.v4.core.spi.TransformerPlugin;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class RenameFieldTransformerPlugin implements TransformerPlugin {

    @Override
    public String name() { return "rename-field"; }
    @Override
    public String version() { return "3.7.0"; }
    @Override
    public String transformerClass() { return "org.apache.kafka.connect.transforms.RegexRouter"; }

    @Override
    public Map<String, Object> defaultConfig() {
        return Map.of("transforms.rename.type", "org.apache.kafka.connect.transforms.RegexRouter");
    }

    @Override
    public String description() { return "Renames fields in Connect records using regex patterns"; }
}
