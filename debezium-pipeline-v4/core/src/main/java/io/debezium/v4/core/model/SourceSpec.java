package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record SourceSpec(
    String type,
    ConnectorConfig connector,
    SchemaSelection schema,
    SnapshotConfig snapshot,
    Map<String, String> properties
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String type; private ConnectorConfig connector; private SchemaSelection schema;
        private SnapshotConfig snapshot; private Map<String, String> properties = Map.of();
        public Builder type(String t) { this.type = t; return this; }
        public Builder connector(ConnectorConfig c) { this.connector = c; return this; }
        public Builder schema(SchemaSelection s) { this.schema = s; return this; }
        public Builder snapshot(SnapshotConfig s) { this.snapshot = s; return this; }
        public Builder properties(Map<String, String> p) { this.properties = p; return this; }
        public SourceSpec build() { return new SourceSpec(type, connector, schema, snapshot, properties); }
    }
}