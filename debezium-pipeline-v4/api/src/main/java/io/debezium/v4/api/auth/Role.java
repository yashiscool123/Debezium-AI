package io.debezium.v4.api.auth;

import java.util.*;

public enum Role {
    SUPER_ADMIN("Super Administrator", "Full system access, including tenant management"),
    ADMIN("Administrator", "Full access within their tenant"),
    PIPELINE_MANAGER("Pipeline Manager", "Create, edit, deploy, and delete pipelines"),
    PIPELINE_OPERATOR("Pipeline Operator", "Start, stop, and monitor pipelines"),
    PIPELINE_VIEWER("Pipeline Viewer", "View-only access to pipelines and metrics"),
    CONNECTOR_ADMIN("Connector Admin", "Manage connector configurations"),
    DATA_ENGINEER("Data Engineer", "AI mapping, schema introspection, and transforms"),
    AUDITOR("Auditor", "Read-only audit log and monitoring access"),
    DEVELOPER("Developer", "API access for integration development");

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }

    public static Set<Permission> defaultPermissions(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> Set.of(Permission.values());
            case ADMIN -> Set.of(
                Permission.PIPELINE_CREATE, Permission.PIPELINE_READ, Permission.PIPELINE_UPDATE,
                Permission.PIPELINE_DELETE, Permission.PIPELINE_DEPLOY, Permission.PIPELINE_START,
                Permission.PIPELINE_STOP, Permission.PIPELINE_RUN_AS, Permission.CONNECTOR_READ,
                Permission.CONNECTOR_CREATE, Permission.CONNECTOR_UPDATE, Permission.CONNECTOR_DELETE,
                Permission.MAPPING_READ, Permission.MAPPING_CREATE, Permission.MAPPING_UPDATE,
                Permission.MAPPING_DELETE, Permission.METRICS_READ, Permission.METRICS_WRITE,
                Permission.AUDIT_READ, Permission.USER_READ, Permission.USER_CREATE,
                Permission.USER_UPDATE, Permission.EXPORT, Permission.IMPORT
            );
            case PIPELINE_MANAGER -> Set.of(
                Permission.PIPELINE_CREATE, Permission.PIPELINE_READ, Permission.PIPELINE_UPDATE,
                Permission.PIPELINE_DELETE, Permission.PIPELINE_DEPLOY, Permission.PIPELINE_START,
                Permission.PIPELINE_STOP, Permission.PIPELINE_RUN_AS, Permission.CONNECTOR_READ,
                Permission.MAPPING_READ, Permission.MAPPING_CREATE, Permission.METRICS_READ,
                Permission.EXPORT, Permission.IMPORT
            );
            case PIPELINE_OPERATOR -> Set.of(
                Permission.PIPELINE_READ, Permission.PIPELINE_DEPLOY, Permission.PIPELINE_START,
                Permission.PIPELINE_STOP, Permission.METRICS_READ
            );
            case PIPELINE_VIEWER -> Set.of(
                Permission.PIPELINE_READ, Permission.METRICS_READ
            );
            case CONNECTOR_ADMIN -> Set.of(
                Permission.CONNECTOR_READ, Permission.CONNECTOR_CREATE,
                Permission.CONNECTOR_UPDATE, Permission.CONNECTOR_DELETE
            );
            case DATA_ENGINEER -> Set.of(
                Permission.MAPPING_READ, Permission.MAPPING_CREATE, Permission.MAPPING_UPDATE,
                Permission.MAPPING_DELETE, Permission.CONNECTOR_READ, Permission.PIPELINE_READ
            );
            case AUDITOR -> Set.of(
                Permission.AUDIT_READ, Permission.METRICS_READ, Permission.PIPELINE_READ
            );
            case DEVELOPER -> Set.of(
                Permission.PIPELINE_CREATE, Permission.PIPELINE_READ, Permission.PIPELINE_UPDATE,
                Permission.CONNECTOR_READ, Permission.MAPPING_READ, Permission.MAPPING_CREATE,
                Permission.EXPORT, Permission.IMPORT
            );
        };
    }

    public static Role fromString(String s) {
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(s) || r.displayName().equalsIgnoreCase(s)) return r;
        }
        throw new IllegalArgumentException("Unknown role: " + s);
    }
}
