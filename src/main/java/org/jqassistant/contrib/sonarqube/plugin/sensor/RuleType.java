package org.jqassistant.contrib.sonarqube.plugin.sensor;

/**
 * The rule types supported by jQAssistant.
 */
public enum RuleType {

    CONCEPT {
        @Override
        public String getKey() {
            return RulesRepository.INVALID_CONCEPT_KEY;
        }
    },
    CONSTRAINT {
        @Override
        public String getKey() {
            return RulesRepository.CONSTRAINT_VIOLATION_KEY;
        }
    };

    public abstract String getKey();
}
