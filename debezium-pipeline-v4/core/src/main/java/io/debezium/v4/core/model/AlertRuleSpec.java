package io.debezium.v4.core.model;

import java.util.List;
import java.util.Map;

public record AlertRuleSpec(
        String name,
        String metric,
        String condition,
        String threshold,
        String severity,
        String duration,
        List<String> notificationChannels) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String metric;
        private String condition;
        private String threshold;
        private String severity;
        private String duration;
        private List<String> notificationChannels;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder metric(String metric) {
            this.metric = metric;
            return this;
        }

        public Builder condition(String condition) {
            this.condition = condition;
            return this;
        }

        public Builder threshold(String threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder severity(String severity) {
            this.severity = severity;
            return this;
        }

        public Builder duration(String duration) {
            this.duration = duration;
            return this;
        }

        public Builder notificationChannels(List<String> notificationChannels) {
            this.notificationChannels = notificationChannels;
            return this;
        }

        public AlertRuleSpec build() {
            return new AlertRuleSpec(name, metric, condition, threshold, severity, duration, notificationChannels);
        }
    }
}
