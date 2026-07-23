package io.debezium.pipeline.generator.model;

import java.util.Map;

public record SecurityConfig(
    String serviceAccount,
    boolean runAsNonRoot,
    int runAsUser,
    int fsGroup,
    Map<String, String> capabilities,
    String seLinuxOptions,
    String appArmorProfile,
    boolean allowPrivilegeEscalation,
    Map<String, String> podSecurityContext
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String serviceAccount;
        private boolean runAsNonRoot = true;
        private int runAsUser = 1000;
        private int fsGroup = 1000;
        private Map<String, String> capabilities;
        private String seLinuxOptions;
        private String appArmorProfile;
        private boolean allowPrivilegeEscalation = false;
        private Map<String, String> podSecurityContext;

        public Builder serviceAccount(String serviceAccount) { this.serviceAccount = serviceAccount; return this; }
        public Builder runAsNonRoot(boolean runAsNonRoot) { this.runAsNonRoot = runAsNonRoot; return this; }
        public Builder runAsUser(int runAsUser) { this.runAsUser = runAsUser; return this; }
        public Builder fsGroup(int fsGroup) { this.fsGroup = fsGroup; return this; }
        public Builder capabilities(Map<String, String> capabilities) { this.capabilities = capabilities; return this; }
        public Builder seLinuxOptions(String seLinuxOptions) { this.seLinuxOptions = seLinuxOptions; return this; }
        public Builder appArmorProfile(String appArmorProfile) { this.appArmorProfile = appArmorProfile; return this; }
        public Builder allowPrivilegeEscalation(boolean allowPrivilegeEscalation) { this.allowPrivilegeEscalation = allowPrivilegeEscalation; return this; }
        public Builder podSecurityContext(Map<String, String> podSecurityContext) { this.podSecurityContext = podSecurityContext; return this; }

        public SecurityConfig build() {
            return new SecurityConfig(serviceAccount, runAsNonRoot, runAsUser, fsGroup, capabilities, seLinuxOptions, appArmorProfile, allowPrivilegeEscalation, podSecurityContext);
        }
    }
}