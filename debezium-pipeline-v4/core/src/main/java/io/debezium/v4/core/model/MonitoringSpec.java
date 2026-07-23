package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record MonitoringSpec(
        boolean metricsEnabled,
        boolean healthEnabled,
        String metricsPort,
        List<AlertRuleSpec> alertRules,
        Map<String, String> dashboards) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean metricsEnabled = true;
        private boolean healthEnabled = true;
        private String metricsPort = "9404";
        private List<AlertRuleSpec> alertRules;
        private Map<String, String> dashboards;

        public Builder metricsEnabled(boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
            return this;
        }

        public Builder healthEnabled(boolean healthEnabled) {
            this.healthEnabled = healthEnabled;
            return this;
        }

        public Builder metricsPort(String metricsPort) {
            this.metricsPort = metricsPort;
            return this;
        }

        public Builder alertRules(List<AlertRuleSpec> alertRules) {
            this.alertRules = alertRules;
            return this;
        }

        public Builder dashboards(Map<String, String> dashboards) {
            this.dashboards = dashboards;
            return this;
        }

        public MonitoringSpec build() {
            return new MonitoringSpec(metricsEnabled, healthEnabled, metricsPort, alertRules, dashboards);
        }
    }
}
