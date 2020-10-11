package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.sonar.api.batch.rule.Severity;

import static org.sonar.api.batch.rule.Severity.MAJOR;
import static org.sonar.api.batch.rule.Severity.MINOR;

/**
 * The rule types supported by jQAssistant.
 */
public enum RuleType {

    CONCEPT {
        @Override
        public String getKey() {
            return "invalid-concept";
        }

        @Override
        public String getName() {
            return JQAssistant.NAME + " Invalid Concept";
        }

        @Override
        public String getDescription() {
            return "A jQAssistant concept is invalid, i.e. could not be applied.";
        }

        @Override
        public Severity getDefaultSeverity() {
            return MINOR;
        }
    },
    CONSTRAINT {
        @Override
        public String getKey() {
            return "constraint-violation";
        }

        @Override
        public String getName() {
            return JQAssistant.NAME + " Constraint Violation";
        }

        @Override
        public String getDescription() {
            return "A jQAssistant constraint has been violated.";
        }

        @Override
        public Severity getDefaultSeverity() {
            return MAJOR;
        }
    };

    public abstract String getKey();

    public abstract String getName();

    public abstract String getDescription();

    public abstract Severity getDefaultSeverity();
}
