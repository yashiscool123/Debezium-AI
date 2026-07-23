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

public record ConnectorConfig(
    String connectorClass,
    String name,
    Map<String, String> config,
    Map<String, String> secrets
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String connectorClass; private String name; private Map<String, String> config = Map.of();
        private Map<String, String> secrets = Map.of();
        public Builder connectorClass(String c) { this.connectorClass = c; return this; }
        public Builder name(String n) { this.name = n; return this; }
        public Builder config(Map<String, String> c) { this.config = c; return this; }
        public Builder secrets(Map<String, String> s) { this.secrets = s; return this; }
        public ConnectorConfig build() { return new ConnectorConfig(connectorClass, name, config, secrets); }
    }
}

public record SchemaSelection(
    List<String> includeTables,
    List<String> excludeTables,
    List<String> includeColumns,
    List<String> excludeColumns,
    String tablePattern,
    boolean includeAllTables
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private List<String> includeTables = List.of(); private List<String> excludeTables = List.of();
        private List<String> includeColumns = List.of(); private List<String> excludeColumns = List.of();
        private String tablePattern; private boolean includeAllTables = false;
        public Builder includeTables(List<String> t) { this.includeTables = t; return this; }
        public Builder excludeTables(List<String> t) { this.excludeTables = t; return this; }
        public Builder includeColumns(List<String> c) { this.includeColumns = c; return this; }
        public Builder excludeColumns(List<String> c) { this.excludeColumns = c; return this; }
        public Builder tablePattern(String p) { this.tablePattern = p; return this; }
        public Builder includeAllTables(boolean b) { this.includeAllTables = b; return this; }
        public SchemaSelection build() { return new SchemaSelection(includeTables, excludeTables, includeColumns, excludeColumns, tablePattern, includeAllTables); }
    }
}

public record SnapshotConfig(
    String mode,
    int snapshotFetchSize,
    List<String> snapshotSelectOverrideColumns,
    boolean snapshotIncludeCollectionList,
    Map<String, String> properties
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String mode = "initial"; private int snapshotFetchSize = 10000;
        private List<String> snapshotSelectOverrideColumns = List.of();
        private boolean snapshotIncludeCollectionList = false;
        private Map<String, String> properties = Map.of();
        public Builder mode(String m) { this.mode = m; return this; }
        public Builder snapshotFetchSize(int s) { this.snapshotFetchSize = s; return this; }
        public Builder snapshotSelectOverrideColumns(List<String> s) { this.snapshotSelectOverrideColumns = s; return this; }
        public Builder snapshotIncludeCollectionList(boolean s) { this.snapshotIncludeCollectionList = s; return this; }
        public Builder properties(Map<String, String> p) { this.properties = p; return this; }
        public SnapshotConfig build() { return new SnapshotConfig(mode, snapshotFetchSize, snapshotSelectOverrideColumns, snapshotIncludeCollectionList, properties); }
    }
}

public record TopicConfig(
    String prefix,
    String namingStrategy,
    String regexPattern,
    String regexReplacement,
    int partitions,
    short replicationFactor,
    Map<String, String> properties
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String prefix; private String namingStrategy = "database.schema.table";
        private String regexPattern; private String regexReplacement;
        private int partitions = 1; private short replicationFactor = 1;
        private Map<String, String> properties = Map.of();
        public Builder prefix(String p) { this.prefix = p; return this; }
        public Builder namingStrategy(String n) { this.namingStrategy = n; return this; }
        public Builder regexPattern(String p) { this.regexPattern = p; return this; }
        public Builder regexReplacement(String r) { this.regexReplacement = r; return this; }
        public Builder partitions(int p) { this.partitions = p; return this; }
        public Builder replicationFactor(short r) { this.replicationFactor = r; return this; }
        public Builder properties(Map<String, String> p) { this.properties = p; return this; }
        public TopicConfig build() { return new TopicConfig(prefix, namingStrategy, regexPattern, regexReplacement, partitions, replicationFactor, properties); }
    }
}

public record SinkConfig(
    String type,
    Map<String, String> properties
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String type = "kafka"; private Map<String, String> properties = Map.of();
        public Builder type(String t) { this.type = t; return this; }
        public Builder properties(Map<String, String> p) { this.properties = p; return this; }
        public SinkConfig build() { return new SinkConfig(type, properties); }
    }
}