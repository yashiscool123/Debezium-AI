package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record SchemaSelection(
        List<String> includeTables,
        List<String> excludeTables,
        List<String> includeColumns,
        List<String> excludeColumns,
        String tablePattern,
        boolean includeAllTables) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> includeTables;
        private List<String> excludeTables;
        private List<String> includeColumns;
        private List<String> excludeColumns;
        private String tablePattern;
        private boolean includeAllTables;

        public Builder includeTables(List<String> includeTables) {
            this.includeTables = includeTables;
            return this;
        }

        public Builder excludeTables(List<String> excludeTables) {
            this.excludeTables = excludeTables;
            return this;
        }

        public Builder includeColumns(List<String> includeColumns) {
            this.includeColumns = includeColumns;
            return this;
        }

        public Builder excludeColumns(List<String> excludeColumns) {
            this.excludeColumns = excludeColumns;
            return this;
        }

        public Builder tablePattern(String tablePattern) {
            this.tablePattern = tablePattern;
            return this;
        }

        public Builder includeAllTables(boolean includeAllTables) {
            this.includeAllTables = includeAllTables;
            return this;
        }

        public SchemaSelection build() {
            return new SchemaSelection(includeTables, excludeTables, includeColumns, excludeColumns, tablePattern, includeAllTables);
        }
    }
}
