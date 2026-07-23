package io.debezium.v4.core.model;

import java.util.Map;

public record TargetSpec(
    String type,
    ConnectorConfig connector,
    TopicConfig topics,
    SinkConfig sink,
    Map<String, String> properties
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String type; private ConnectorConfig connector; private TopicConfig topics;
        private SinkConfig sink; private Map<String, String> properties = Map.of();
        public Builder type(String t) { this.type = t; return this; }
        public Builder connector(ConnectorConfig c) { this.connector = c; return this; }
        public Builder topics(TopicConfig t) { this.topics = t; return this; }
        public Builder sink(SinkConfig s) { this.sink = s; return this; }
        public Builder properties(Map<String, String> p) { this.properties = p; return this; }
        public TargetSpec build() { return new TargetSpec(type, connector, topics, sink, properties); }
    }
}