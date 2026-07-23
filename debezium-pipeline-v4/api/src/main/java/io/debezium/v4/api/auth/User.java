package io.debezium.v4.api.auth;

import java.time.Instant;
import java.util.*;

public enum UserType {
    HUMAN, SERVICE
}

public record User(
    String id,
    String username,
    String email,
    String passwordHash,
    UserType type,
    Set<Role> roles,
    Set<Permission> permissions,
    String tenantId,
    boolean enabled,
    boolean mfaEnabled,
    Instant lastLogin,
    Instant createdAt,
    Instant updatedAt,
    Map<String, Object> attributes
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id; private String username; private String email;
        private String passwordHash; private UserType type = UserType.HUMAN;
        private Set<Role> roles = new HashSet<>();
        private Set<Permission> permissions = new HashSet<>(); private String tenantId = "default";
        private boolean enabled = true; private boolean mfaEnabled = false;
        private Instant lastLogin; private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();
        private Map<String, Object> attributes = new HashMap<>();

        public Builder id(String v) { this.id = v; return this; }
        public Builder username(String v) { this.username = v; return this; }
        public Builder email(String v) { this.email = v; return this; }
        public Builder passwordHash(String v) { this.passwordHash = v; return this; }
        public Builder type(UserType v) { this.type = v; return this; }
        public Builder roles(Set<Role> v) { this.roles = v; return this; }
        public Builder addRole(Role r) { this.roles.add(r); return this; }
        public Builder permissions(Set<Permission> v) { this.permissions = v; return this; }
        public Builder addPermission(Permission p) { this.permissions.add(p); return this; }
        public Builder tenantId(String v) { this.tenantId = v; return this; }
        public Builder enabled(boolean v) { this.enabled = v; return this; }
        public Builder mfaEnabled(boolean v) { this.mfaEnabled = v; return this; }
        public Builder attributes(Map<String, Object> v) { this.attributes = v; return this; }
        public User build() { return new User(id, username, email, passwordHash, type, Collections.unmodifiableSet(roles), Collections.unmodifiableSet(permissions), tenantId, enabled, mfaEnabled, lastLogin, createdAt, updatedAt, Collections.unmodifiableMap(attributes)); }
    }
}
