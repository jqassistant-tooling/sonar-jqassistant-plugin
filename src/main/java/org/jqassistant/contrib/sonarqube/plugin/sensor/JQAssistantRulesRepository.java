package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.plugins.java.Java;

public final class JQAssistantRulesRepository implements RulesDefinition {

    public static final String INVALID_CONCEPT_KEY = "invalid-concept";
    public static final String INVALID_CONCEPT_RULE_NAME = JQAssistant.NAME + " Invalid Concept";

    public static final String CONSTRAINT_VIOLATION_KEY = "constraint-violation";
    public static final String CONSTRAINT_VIOLATION_RULE_NAME = JQAssistant.NAME + " Constraint Violation";

    @Override
    public void define(Context context) {
        final NewRepository newRepository = context.createRepository(JQAssistant.KEY, Java.KEY);
        newRepository.setName(JQAssistant.NAME);

        final NewRule constraintRule = newRepository.createRule(CONSTRAINT_VIOLATION_KEY);
        constraintRule.setName(CONSTRAINT_VIOLATION_RULE_NAME);
        constraintRule.setInternalKey(CONSTRAINT_VIOLATION_KEY);
        constraintRule.setSeverity(Severity.MAJOR);
        constraintRule.setMarkdownDescription("A jQAssistant constraint has been violated.");

        final NewRule conceptRule = newRepository.createRule(INVALID_CONCEPT_KEY);
        conceptRule.setName(INVALID_CONCEPT_RULE_NAME);
        conceptRule.setInternalKey(INVALID_CONCEPT_KEY);
        conceptRule.setSeverity(Severity.MINOR);
        conceptRule.setMarkdownDescription("A jQAssistant concept is invalid, i.e. could not be applied.");

        newRepository.done();
    }

}
