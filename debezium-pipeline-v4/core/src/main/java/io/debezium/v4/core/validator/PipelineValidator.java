package io.debezium.v4.core.validator;

import io.debezium.v4.core.model.*;
import java.util.*;
import java.util.regex.Pattern;

public class PipelineValidator {
    private static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9._-]{1,128}$");
    private static final Pattern VALID_NAMESPACE = Pattern.compile("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$");

    public ValidationResult validate(PipelineDefinition pipeline) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (pipeline == null) return error("Pipeline definition is null");
        if (isBlank(pipeline.name())) issues.add(error("name", "Pipeline name is required"));
        else if (!VALID_NAME.matcher(pipeline.name()).matches()) issues.add(error("name", "Invalid pipeline name"));
        if (pipeline.source() == null) issues.add(error("source", "Source configuration is required"));
        else validateSource(pipeline.source(), issues);
        if (pipeline.tableMappings() != null && !pipeline.tableMappings().isEmpty()) validateMappings(pipeline.tableMappings(), issues);
        if (pipeline.deployment() != null) validateDeployment(pipeline.deployment(), issues);
        if (pipeline.monitoring() != null) validateMonitoring(pipeline.monitoring(), issues);
        if (pipeline.dataQuality() != null && pipeline.dataQuality().enabled()) validateDataQuality(pipeline.dataQuality(), issues);
        return new ValidationResult(issues.stream().noneMatch(i -> i.severity() == Severity.ERROR), issues, summary(issues));
    }

    private void validateSource(SourceSpec source, List<ValidationIssue> issues) {
        if (isBlank(source.type())) issues.add(error("source.type", "Source type is required"));
        if (source.connector() == null) issues.add(error("source.connector", "Source connector config is required"));
        if (source.schema() == null) issues.add(error("source.schema", "Schema selection is required"));
    }

    private void validateMappings(List<TableMappingSpec> mappings, List<ValidationIssue> issues) {
        Set<String> sourceTables = new HashSet<>();
        for (TableMappingSpec m : mappings) {
            if (isBlank(m.sourceTable())) issues.add(error("mapping.sourceTable", "Source table name required"));
            if (isBlank(m.targetTable())) issues.add(warning("mapping.targetTable", "Target table name missing"));
            if (!sourceTables.add(m.sourceTable()))
                issues.add(warning("mapping." + m.sourceTable(), "Duplicate source table mapping"));
        }
    }

    private void validateDeployment(DeploymentSpec dep, List<ValidationIssue> issues) {
        if (dep.namespace() != null && !VALID_NAMESPACE.matcher(dep.namespace()).matches())
            issues.add(error("deployment.namespace", "Invalid Kubernetes namespace"));
        if (dep.replicas() < 1) issues.add(error("deployment.replicas", "Replicas must be >= 1"));
        if (dep.resources() != null) {
            ResourceSpec r = dep.resources();
            String cpuPattern = "^\\d+(\\.\\d+)?[m]?$";
            String memPattern = "^\\d+[KMGTV]i?$";
            if (r.cpuRequest() != null && !r.cpuRequest().matches(cpuPattern))
                issues.add(warning("resources.cpuRequest", "Invalid CPU format: " + r.cpuRequest()));
            if (r.memoryRequest() != null && !r.memoryRequest().matches(memPattern))
                issues.add(warning("resources.memoryRequest", "Invalid memory format: " + r.memoryRequest()));
        }
    }

    private void validateMonitoring(MonitoringSpec mon, List<ValidationIssue> issues) {
        if (mon.alertRules() != null) {
            for (AlertRuleSpec rule : mon.alertRules()) {
                if (isBlank(rule.name())) issues.add(warning("alertRule.name", "Alert rule name missing"));
                if (isBlank(rule.metric())) issues.add(warning("alertRule." + rule.name(), "Metric name missing"));
                if (isBlank(rule.threshold())) issues.add(warning("alertRule." + rule.name(), "Threshold missing"));
            }
        }
    }

    private void validateDataQuality(DataQualityConfig dq, List<ValidationIssue> issues) {
        if (dq.rules() == null || dq.rules().isEmpty()) {
            issues.add(warning("dataQuality.rules", "Data quality enabled but no rules defined"));
            return;
        }
        Set<String> ruleNames = new HashSet<>();
        for (DataQualityRuleSpec rule : dq.rules()) {
            if (isBlank(rule.name())) issues.add(error("dataQuality.rule.name", "Data quality rule name is required"));
            else if (!ruleNames.add(rule.name())) issues.add(error("dataQuality.rule." + rule.name(), "Duplicate rule name: " + rule.name()));
            if (isBlank(rule.ruleType())) issues.add(error("dataQuality.rule." + (rule.name() != null ? rule.name() : "?") + ".type", "Rule type is required"));
            if (!List.of("NOT_NULL", "NOT_EMPTY", "REGEX", "RANGE", "EQUALS", "UNIQUE", "TYPE_CHECK", "MIN_LENGTH", "MAX_LENGTH", "COMPLETENESS", "ACCURACY", "CONSISTENCY", "TIMELINESS", "CUSTOM_SQL").contains(rule.ruleType()))
                issues.add(warning("dataQuality.rule." + rule.name() + ".type", "Unknown rule type: " + rule.ruleType()));
            if (!List.of("ROW", "COLUMN", "TABLE", "PIPELINE").contains(rule.scope()))
                issues.add(warning("dataQuality.rule." + rule.name() + ".scope", "Unknown scope: " + rule.scope()));
            if (!List.of("WARN", "ERROR", "FATAL").contains(rule.severity()))
                issues.add(warning("dataQuality.rule." + rule.name() + ".severity", "Unknown severity: " + rule.severity() + ", expected WARN/ERROR/FATAL"));
            if (!"ROW".equals(rule.scope()) && isBlank(rule.column()))
                issues.add(warning("dataQuality.rule." + rule.name(), "Column scope rule has no column specified"));
            if ("REGEX".equals(rule.ruleType()) && (rule.configuration() == null || isBlank(rule.configuration().get("pattern"))))
                issues.add(warning("dataQuality.rule." + rule.name(), "REGEX rule missing 'pattern' in configuration"));
            if ("RANGE".equals(rule.ruleType())) {
                var cfg = rule.configuration();
                if (cfg == null || (cfg.get("min") == null && cfg.get("max") == null))
                    issues.add(warning("dataQuality.rule." + rule.name(), "RANGE rule missing 'min' or 'max' in configuration"));
            }
        }
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private ValidationIssue error(String field, String msg) { return new ValidationIssue(Severity.ERROR, field, msg, ""); }
    private ValidationIssue warning(String field, String msg) { return new ValidationIssue(Severity.WARNING, field, msg, ""); }
    private String summary(List<ValidationIssue> issues) {
        long errors = issues.stream().filter(i -> i.severity() == Severity.ERROR).count();
        long warnings = issues.stream().filter(i -> i.severity() == Severity.WARNING).count();
        return errors > 0 ? errors + " error(s), " + warnings + " warning(s)" : warnings + " warning(s)";
    }

    public record ValidationResult(boolean valid, List<ValidationIssue> issues, String summary) {}
    public record ValidationIssue(Severity severity, String field, String message, String suggestion) {}
    public enum Severity { ERROR, WARNING, INFO }
}
