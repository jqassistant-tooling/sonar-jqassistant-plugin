package org.jqassistant.contrib.sonar.plugin.sensor;

import org.jqassistant.contrib.sonar.plugin.JQAssistant;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.plugins.java.Java;

/**
 * Dummy repository to define one placeholder rule, used to assign all
 * violations coming from a
 * {@link JQAssistant#SETTINGS_VALUE_DEFAULT_REPORT_FILE_PATH
 * jQAssistant report} in a local project.
 */
public final class JQAssistantRulesRepository implements RulesDefinition {

    public final static String INVALID_CONCEPT_KEY = "invalid-concept";
    static final String INVALID_CONCEPT_RULE_NAME = JQAssistant.NAME + " Invalid Concept";

    public final static String CONSTRAINT_VIOLATION_KEY = "constraint-violation";
    static final String CONSTRAINT_VIOLATION_RULE_NAME = JQAssistant.NAME + " Constraint Violation";

    @Override
    public void define(Context context) {
        final NewRepository newRepository = context.createRepository(JQAssistant.KEY, Java.KEY);
        newRepository.setName(JQAssistant.NAME);

        final NewRule constraintRule = newRepository.createRule(CONSTRAINT_VIOLATION_KEY);
        constraintRule.setName(CONSTRAINT_VIOLATION_RULE_NAME);
        constraintRule.setInternalKey(CONSTRAINT_VIOLATION_KEY);
        constraintRule.setSeverity(Severity.MAJOR);
        constraintRule.setMarkdownDescription("*This rule must be activated for every project receiving violations from jQAssistant.");

        final NewRule conceptRule = newRepository.createRule(INVALID_CONCEPT_KEY);
        conceptRule.setName(INVALID_CONCEPT_RULE_NAME);
        conceptRule.setInternalKey(INVALID_CONCEPT_KEY);
        conceptRule.setSeverity(Severity.MINOR);
        conceptRule.setMarkdownDescription("*This rule must be activated for every project receiving violations from jQAssistant.");

        newRepository.done();
    }

}
