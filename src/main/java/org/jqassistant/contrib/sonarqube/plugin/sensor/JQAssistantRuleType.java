package org.jqassistant.contrib.sonarqube.plugin.sensor;

/**
 * The rule types supported by jQAssistant.
 */
public enum JQAssistantRuleType {

    Concept {
        @Override
        public String getKey() {
            return JQAssistantRulesRepository.INVALID_CONCEPT_KEY;
        }
    },
    Constraint {
        @Override
        public String getKey() {
            return JQAssistantRulesRepository.CONSTRAINT_VIOLATION_KEY;
        }
    };

    public abstract String getKey();
}
