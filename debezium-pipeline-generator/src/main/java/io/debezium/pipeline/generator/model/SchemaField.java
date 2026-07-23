package io.debezium.pipeline.generator.model;

import java.util.Map;

public record SchemaField(
    String catalog,
    String schema,
    String tableName,
    String name,
    String dataType,
    String nativeType,
    int precision,
    int scale,
    boolean nullable,
    boolean primaryKey,
    boolean autoIncrement,
    String defaultValue,
    String comment,
    int ordinalPosition,
    Map<String, String> metadata
) {
    public String fullName() {
        return catalog + "." + schema + "." + tableName + "." + name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String catalog;
        private String schema;
        private String tableName;
        private String name;
        private String dataType;
        private String nativeType;
        private int precision = 0;
        private int scale = 0;
        private boolean nullable = true;
        private boolean primaryKey = false;
        private boolean autoIncrement = false;
        private String defaultValue;
        private String comment;
        private int ordinalPosition = 0;
        private Map<String, String> metadata;

        public Builder catalog(String catalog) { this.catalog = catalog; return this; }
        public Builder schema(String schema) { this.schema = schema; return this; }
        public Builder tableName(String tableName) { this.tableName = tableName; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder dataType(String dataType) { this.dataType = dataType; return this; }
        public Builder nativeType(String nativeType) { this.nativeType = nativeType; return this; }
        public Builder precision(int precision) { this.precision = precision; return this; }
        public Builder scale(int scale) { this.scale = scale; return this; }
        public Builder nullable(boolean nullable) { this.nullable = nullable; return this; }
        public Builder primaryKey(boolean primaryKey) { this.primaryKey = primaryKey; return this; }
        public Builder autoIncrement(boolean autoIncrement) { this.autoIncrement = autoIncrement; return this; }
        public Builder defaultValue(String defaultValue) { this.defaultValue = defaultValue; return this; }
        public Builder comment(String comment) { this.comment = comment; return this; }
        public Builder ordinalPosition(int ordinalPosition) { this.ordinalPosition = ordinalPosition; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }

        public SchemaField build() {
            return new SchemaField(catalog, schema, tableName, name, dataType, nativeType, precision, scale, nullable, primaryKey, autoIncrement, defaultValue, comment, ordinalPosition, metadata);
        }
    }
}