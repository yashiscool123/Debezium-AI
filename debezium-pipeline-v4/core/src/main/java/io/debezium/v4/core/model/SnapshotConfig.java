package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record SnapshotConfig(
        String mode,
        int snapshotFetchSize,
        List<String> snapshotSelectOverrideColumns,
        boolean snapshotIncludeCollectionList,
        Map<String, String> properties) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String mode = "initial";
        private int snapshotFetchSize = 10000;
        private List<String> snapshotSelectOverrideColumns;
        private boolean snapshotIncludeCollectionList;
        private Map<String, String> properties;

        public Builder mode(String mode) {
            this.mode = mode;
            return this;
        }

        public Builder snapshotFetchSize(int snapshotFetchSize) {
            this.snapshotFetchSize = snapshotFetchSize;
            return this;
        }

        public Builder snapshotSelectOverrideColumns(List<String> snapshotSelectOverrideColumns) {
            this.snapshotSelectOverrideColumns = snapshotSelectOverrideColumns;
            return this;
        }

        public Builder snapshotIncludeCollectionList(boolean snapshotIncludeCollectionList) {
            this.snapshotIncludeCollectionList = snapshotIncludeCollectionList;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            this.properties = properties;
            return this;
        }

        public SnapshotConfig build() {
            return new SnapshotConfig(mode, snapshotFetchSize, snapshotSelectOverrideColumns, snapshotIncludeCollectionList, properties);
        }
    }
}
