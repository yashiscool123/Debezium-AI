package io.debezium.pipeline.generator.model;

public enum ConnectorType {
    MYSQL("mysql", "MySQL", "io.debezium.connector.mysql.MySqlConnector"),
    POSTGRESQL("postgresql", "PostgreSQL", "io.debezium.connector.postgresql.PostgresConnector"),
    MONGODB("mongodb", "MongoDB", "io.debezium.connector.mongodb.MongoDbConnector"),
    SQLSERVER("sqlserver", "SQL Server", "io.debezium.connector.sqlserver.SqlServerConnector"),
    ORACLE("oracle", "Oracle", "io.debezium.connector.oracle.OracleConnector"),
    MARIADB("mariadb", "MariaDB", "io.debezium.connector.mariadb.MariaDbConnector"),
    JDBC("jdbc", "JDBC", "io.debezium.connector.jdbc.JdbcConnector"),
    DB2("db2", "Db2", "io.debezium.connector.db2.Db2Connector"),
    CASSANDRA("cassandra", "Cassandra", "io.debezium.connector.cassandra.CassandraConnector"),
    VITESS("vitess", "Vitess", "io.debezium.connector.vitess.VitessConnector"),
    SPOKER("spanner", "Spanner", "io.debezium.connector.spanner.SpannerConnector"),
    INFORMIX("informix", "Informix", "io.debezium.connector.informix.InformixConnector"),
    YASHANDB("yashandb", "YashanDB", "io.debezium.connector.yashandb.YashanDBConnector"),
    COCKROACHDB("cockroachdb", "CockroachDB", "io.debezium.connector.cockroachdb.CockroachDbConnector");

    private final String id;
    private final String displayName;
    private final String connectorClass;

    ConnectorType(String id, String displayName, String connectorClass) {
        this.id = id;
        this.displayName = displayName;
        this.connectorClass = connectorClass;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getConnectorClass() { return connectorClass; }

    public static ConnectorType fromId(String id) {
        for (ConnectorType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown connector type: " + id);
    }
}