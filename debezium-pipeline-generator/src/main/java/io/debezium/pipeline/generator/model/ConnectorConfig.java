package io.debezium.pipeline.generator.model;

import java.util.List;
import java.util.Map;

public record ConnectorConfig(
    String name,
    String connectorClass,
    String type,
    Map<String, String> config,
    List<TransformConfig> transforms,
    Map<String, String> predicates,
    TopicCreationConfig topicCreation
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String connectorClass;
        private String type;
        private Map<String, String> config;
        private List<TransformConfig> transforms;
        private Map<String, String> predicates;
        private TopicCreationConfig topicCreation;

        public Builder name(String name) { this.name = name; return this; }
        public Builder connectorClass(String connectorClass) { this.connectorClass = connectorClass; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder config(Map<String, String> config) { this.config = config; return this; }
        public Builder transforms(List<TransformConfig> transforms) { this.transforms = transforms; return this; }
        public Builder predicates(Map<String, String> predicates) { this.predicates = predicates; return this; }
        public Builder topicCreation(TopicCreationConfig topicCreation) { this.topicCreation = topicCreation; return this; }

        public ConnectorConfig build() {
            return new ConnectorConfig(name, connectorClass, type, config, transforms, predicates, topicCreation);
        }
    }

    public enum ConnectorType {
        MYSQL("io.debezium.connector.mysql.MySqlConnector", "mysql"),
        POSTGRES("io.debezium.connector.postgresql.PostgresConnector", "postgresql"),
        MONGODB("io.debezium.connector.mongodb.MongoDbConnector", "mongodb"),
        SQLSERVER("io.debezium.connector.sqlserver.SqlServerConnector", "sqlserver"),
        ORACLE("io.debezium.connector.oracle.OracleConnector", "oracle"),
        MARIADB("io.debezium.connector.mariadb.MariaDbConnector", "mariadb"),
        JDBC("io.debezium.connector.jdbc.JdbcSourceConnector", "jdbc"),
        CASSANDRA("io.debezium.connector.cassandra.CassandraConnector", "cassandra"),
        DB2("io.debezium.connector.db2.Db2Connector", "db2"),
        INFORMIX("io.debezium.connector.informix.InformixConnector", "informix"),
        VITESS("io.debezium.connector.vitess.VitessConnector", "vitess"),
        SPOCK("io.debezium.connector.spanner.SpannerConnector", "spanner"),
        YUGABYTE("io.debezium.connector.yugabyte.YugabyteConnector", "yugabyte"),
        COCKROACHDB("io.debezium.connector.cockroachdb.CockroachDbConnector", "cockroachdb"),
        SPANNER("io.debezium.connector.spanner.SpannerConnector", "spanner"),
        JDE2("io.debezium.connector.jde.JdeConnector", "jde"),
        SINK_JDBC("io.debezium.connector.jdbc.JdbcSinkConnector", "jdbc-sink"),
        SINK_ELASTICSEARCH("io.debezium.connector.elasticsearch.ElasticsearchSinkConnector", "elasticsearch-sink"),
        SINK_S3("io.debezium.connector.s3.S3SinkConnector", "s3-sink"),
        SINK_AZURE_BLOB("io.debezium.connector.azureblob.AzureBlobSinkConnector", "azure-blob-sink"),
        SINK_GCS("io.debezium.connector.gcs.GCSSinkConnector", "gcs-sink"),
        SINK_HDFS("io.debezium.connector.hdfs.HdfsSinkConnector", "hdfs-sink"),
        SINK_SNOWFLAKE("io.debezium.connector.snowflake.SnowflakeSinkConnector", "snowflake-sink"),
        SINK_BIGQUERY("io.debezium.connector.bigquery.BigQuerySinkConnector", "bigquery-sink"),
        SINK_REDIS("io.debezium.connector.redis.RedisSinkConnector", "redis-sink"),
        SINK_CASSANDRA("io.debezium.connector.cassandra.CassandraSinkConnector", "cassandra-sink"),
        SINK_MONGODB("io.debezium.connector.mongodb.MongoDbSinkConnector", "mongodb-sink"),
        SINK_HTTP("io.debezium.connector.http.HttpSinkConnector", "http-sink"),
        SINK_KINESIS("io.debezium.connector.kinesis.KinesisSinkConnector", "kinesis-sink"),
        SINK_PULSAR("io.debezium.connector.pulsar.PulsarSinkConnector", "pulsar-sink"),
        SINK_RABBITMQ("io.debezium.connector.rabbitmq.RabbitMqSinkConnector", "rabbitmq-sink"),
        SINK_SOLACE("io.debezium.connector.solace.SolaceSinkConnector", "solace-sink");

        private final String connectorClass;
        private final String type;
        ConnectorType(String connectorClass, String type) {
            this.connectorClass = connectorClass;
            this.type = type;
        }
        public String connectorClass() { return connectorClass; }
        public String type() { return type; }
    }
}