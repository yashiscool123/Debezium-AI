package io.debezium.v4.plugins.transformers.builtin;

import io.debezium.v4.core.spi.TransformerPlugin;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class ContentBasedRouterTransformerPlugin implements TransformerPlugin {

    @Override
    public String name() { return "content-based-router"; }
    @Override
    public String version() { return "3.7.0"; }
    @Override
    public String transformerClass() { return "org.apache.kafka.connect.transforms.ContentBasedRouter"; }

    @Override
    public Map<String, Object> defaultConfig() {
        return Map.of("transforms.router.type", "org.apache.kafka.connect.transforms.ContentBasedRouter");
    }

    @Override
    public String description() { return "Routes records to different topics based on content"; }
}
