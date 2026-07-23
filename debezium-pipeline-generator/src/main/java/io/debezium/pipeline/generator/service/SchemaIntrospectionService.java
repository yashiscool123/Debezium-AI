package io.debezium.pipeline.generator.service;

import io.debezium.pipeline.generator.model.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class SchemaIntrospectionService {

    public record SchemaInfo(
        String catalog,
        String schema,
        List<TableInfo> tables
    ) {}

    public record TableInfo(
        String catalog,
        String schema,
        String name,
        String type,
        String comment,
        List<SchemaField> columns,
        List<IndexInfo> indexes,
        List<ForeignKeyInfo> foreignKeys
    ) {}

    public record IndexInfo(
        String name,
        List<String> columns,
        boolean unique,
        boolean primary
    ) {}

    public record ForeignKeyInfo(
        String name,
        String columnName,
        String referencedTableCatalog,
        String referencedTableSchema,
        String referencedTableName,
        String referencedColumnName
    ) {}

    public SchemaInfo introspect(SourceDatabaseConfig config) {
        String url = buildJdbcUrl(config);
        
        try (Connection conn = DriverManager.getConnection(url, config.username(), config.password())) {
            DatabaseMetaData meta = conn.getMetaData();
            
            String catalog = config.databaseName();
            String schemaPattern = config.tables() != null && !config.tables().isEmpty() ? null : "%";
            
            List<TableInfo> tables = new ArrayList<>();
            
            try (ResultSet rs = meta.getTables(catalog, schemaPattern, "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String tableSchema = rs.getString("TABLE_SCHEM");
                    String tableCatalog = rs.getString("TABLE_CAT");
                    String tableType = rs.getString("TABLE_TYPE");
                    String tableComment = rs.getString("REMARKS");
                    
                    // Filter by table list if provided
                    if (config.tables() != null && !config.tables().isEmpty() && 
                        !config.tables().contains(tableName)) {
                        continue;
                    }
                    
                    List<SchemaField> columns = getColumns(meta, tableCatalog, tableSchema, tableName);
                    List<IndexInfo> indexes = getIndexes(meta, tableCatalog, tableSchema, tableName);
                    List<ForeignKeyInfo> foreignKeys = getForeignKeys(meta, tableCatalog, tableSchema, tableName);
                    
                    tables.add(new TableInfo(
                        tableCatalog, tableSchema, tableName, tableType, tableComment,
                        columns, indexes, foreignKeys
                    ));
                }
            }
            
            return new SchemaInfo(catalog, schemaPattern, tables);
            
        } catch (SQLException e) {
            Log.errorf("Failed to introspect schema: %s", e.getMessage());
            throw new RuntimeException("Schema introspection failed", e);
        }
    }

    private List<SchemaField> getColumns(DatabaseMetaData meta, String catalog, String schema, String table) throws SQLException {
        List<SchemaField> columns = new ArrayList<>();
        
        try (ResultSet rs = meta.getColumns(catalog, schema, table, "%")) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("TYPE_NAME");
                int precision = rs.getInt("COLUMN_SIZE");
                int scale = rs.getInt("DECIMAL_DIGITS");
                int nullable = rs.getInt("NULLABLE");
                String defaultValue = rs.getString("COLUMN_DEF");
                String comment = rs.getString("REMARKS");
                int ordinalPosition = rs.getInt("ORDINAL_POSITION");
                String isAutoIncrement = rs.getString("IS_AUTOINCREMENT");
                
                columns.add(SchemaField.builder()
                    .catalog(catalog)
                    .schema(schema)
                    .tableName(table)
                    .name(columnName)
                    .dataType(normalizeDataType(dataType))
                    .nativeType(dataType)
                    .precision(precision)
                    .scale(scale)
                    .nullable(nullable == DatabaseMetaData.columnNullable)
                    .primaryKey(false) // Will be updated from primary keys
                    .autoIncrement("YES".equalsIgnoreCase(isAutoIncrement))
                    .defaultValue(defaultValue)
                    .comment(comment)
                    .ordinalPosition(ordinalPosition)
                    .metadata(Map.of())
                    .build());
            }
        }
        
        // Get primary keys
        try (ResultSet rs = meta.getPrimaryKeys(catalog, schema, table)) {
            Set<String> pkColumns = new HashSet<>();
            while (rs.next()) {
                pkColumns.add(rs.getString("COLUMN_NAME"));
            }
            for (SchemaField col : columns) {
                if (pkColumns.contains(col.name())) {
                    // Rebuild with primaryKey = true
                    int index = columns.indexOf(col);
                    columns.set(index, new SchemaField(
                        col.catalog(), col.schema(), col.tableName(), col.name(),
                        col.dataType(), col.nativeType(), col.precision(), col.scale(),
                        col.nullable(), true, col.autoIncrement(), col.defaultValue(),
                        col.comment(), col.ordinalPosition(), col.metadata()
                    ));
                }
            }
        }
        
        return columns;
    }

    private List<IndexInfo> getIndexes(DatabaseMetaData meta, String catalog, String schema, String table) throws SQLException {
        List<IndexInfo> indexes = new ArrayList<>();
        Map<String, IndexInfo.Builder> indexMap = new LinkedHashMap<>();
        
        try (ResultSet rs = meta.getIndexInfo(catalog, schema, table, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName == null) continue;
                
                String columnName = rs.getString("COLUMN_NAME");
                boolean nonUnique = rs.getBoolean("NON_UNIQUE");
                String type = rs.getString("TYPE");
                
                IndexInfo.Builder builder = indexMap.computeIfAbsent(indexName, k -> 
                    IndexInfo.builder().name(indexName).unique(!nonUnique).primary("PRIMARY".equalsIgnoreCase(indexName))
                );
                builder.columns(new ArrayList<>(builder.columns()));
                builder.columns().add(columnName);
            }
        }
        
        return indexMap.values().stream().map(IndexInfo.Builder::build).collect(Collectors.toList());
    }

    private List<ForeignKeyInfo> getForeignKeys(DatabaseMetaData meta, String catalog, String schema, String table) throws SQLException {
        List<ForeignKeyInfo> fks = new ArrayList<>();
        
        try (ResultSet rs = meta.getImportedKeys(catalog, schema, table)) {
            while (rs.next()) {
                fks.add(new ForeignKeyInfo(
                    rs.getString("FK_NAME"),
                    rs.getString("FKCOLUMN_NAME"),
                    rs.getString("PKTABLE_CAT"),
                    rs.getString("PKTABLE_SCHEM"),
                    rs.getString("PKTABLE_NAME"),
                    rs.getString("PKCOLUMN_NAME")
                ));
            }
        }
        
        return fks;
    }

    private String buildJdbcUrl(SourceDatabaseConfig config) {
        return switch (config.type().toLowerCase()) {
            case "mysql", "mariadb" -> String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true",
                config.host(), config.port(), config.databaseName()
            );
            case "postgresql" -> String.format(
                "jdbc:postgresql://%s:%d/%s",
                config.host(), config.port(), config.databaseName()
            );
            case "sqlserver" -> String.format(
                "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=true;trustServerCertificate=true",
                config.host(), config.port(), config.databaseName()
            );
            case "oracle" -> String.format(
                "jdbc:oracle:thin:@//%s:%d/%s",
                config.host(), config.port(), config.databaseName()
            );
            case "mongodb" -> throw new UnsupportedOperationException("MongoDB uses different connection mechanism");
            case "db2" -> String.format(
                "jdbc:db2://%s:%d/%s",
                config.host(), config.port(), config.databaseName()
            );
            default -> throw new IllegalArgumentException("Unsupported database type: " + config.type());
        };
    }

    private String normalizeDataType(String nativeType) {
        String upper = nativeType.toUpperCase();
        
        // Numeric
        if (upper.contains("INT") || upper.contains("BIGINT") || upper.contains("SMALLINT") || 
            upper.contains("TINYINT") || upper.contains("SERIAL") || upper.contains("IDENTITY")) {
            return "INTEGER";
        }
        if (upper.contains("DECIMAL") || upper.contains("NUMERIC") || upper.contains("NUMBER")) {
            return "DECIMAL";
        }
        if (upper.contains("FLOAT") || upper.contains("DOUBLE") || upper.contains("REAL")) {
            return "DOUBLE";
        }
        
        // String
        if (upper.contains("CHAR") || upper.contains("TEXT") || upper.contains("VARCHAR") || 
            upper.contains("CLOB") || upper.contains("STRING")) {
            return "STRING";
        }
        
        // Binary
        if (upper.contains("BLOB") || upper.contains("BINARY") || upper.contains("VARBINARY") || 
            upper.contains("BYTEA")) {
            return "BYTES";
        }
        
        // Boolean
        if (upper.contains("BOOL")) {
            return "BOOLEAN";
        }
        
        // Temporal
        if (upper.contains("DATE") && !upper.contains("TIME")) {
            return "DATE";
        }
        if (upper.contains("TIME") && !upper.contains("DATE")) {
            return "TIME";
        }
        if (upper.contains("TIMESTAMP") || upper.contains("DATETIME")) {
            return "TIMESTAMP";
        }
        
        // UUID
        if (upper.contains("UUID")) {
            return "UUID";
        }
        
        // JSON
        if (upper.contains("JSON")) {
            return "JSON";
        }
        
        return nativeType;
    }

    public List<String> listTables(SourceDatabaseConfig config) {
        SchemaInfo info = introspect(config);
        return info.tables().stream()
            .map(TableInfo::name)
            .collect(Collectors.toList());
    }

    public Optional<TableInfo> getTable(SourceDatabaseConfig config, String tableName) {
        SchemaInfo info = introspect(config);
        return info.tables().stream()
            .filter(t -> t.name().equalsIgnoreCase(tableName))
            .findFirst();
    }
}