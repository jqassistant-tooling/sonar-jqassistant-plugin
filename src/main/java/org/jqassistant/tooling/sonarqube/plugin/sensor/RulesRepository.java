package org.jqassistant.tooling.sonarqube.plugin.sensor;

import org.jqassistant.tooling.sonarqube.plugin.JQAssistant;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.plugins.java.Java;

@ScannerSide
public final class RulesRepository implements RulesDefinition {

    @Override
    public void define(Context context) {
        final NewRepository newRepository = context.createRepository(JQAssistant.KEY, Java.KEY);
        newRepository.setName(JQAssistant.NAME);

        for (RuleType value : RuleType.values()) {
            NewRule newRule = newRepository.createRule(value.getKey());
            newRule.setName(value.getName());
            newRule.setInternalKey(value.getKey());
            newRule.setSeverity(value.getDefaultSeverity().name());
            newRule.setHtmlDescription(value.getDescription());
        }

        newRepository.done();
    }

}
