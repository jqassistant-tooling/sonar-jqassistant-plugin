package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;

/**
 *
 * @author rzozmann
 *
 */
@BatchSide
public class RuleKeyResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(JQAssistantSensor.class);

    private ActiveRules ruleFinder;

    public RuleKeyResolver(ActiveRules ruleFinder) {
        this.ruleFinder = ruleFinder;
    }

    public RuleKey resolve(JQAssistantRuleType type) {
        final ActiveRule rule = ruleFinder.findByInternalKey(JQAssistant.KEY, type.getKey());
        RuleKey ruleKey;
        if (rule != null) {
            ruleKey = rule.ruleKey();
        } else {
            ruleKey = null;
        }
        // Remember: Activating of rule in a profile does not have a effect in
        // database :-(
        if (ruleKey == null) {
            LOGGER.error("The rule '{}' is not active/present, no violation for {} can be reported in SonarQ", type.getKey(), JQAssistant.NAME);
        }
        return ruleKey;
    }

}
