package com.buschmais.jqassistant.sonar.plugin.sensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;

import com.buschmais.jqassistant.sonar.plugin.JQAssistant;

/**
 *
 * @author rzozmann
 *
 */
public class RuleKeyResolver implements BatchExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(JQAssistantSensor.class);

    private RulesProfile rulesProfile;

    private RuleFinder ruleFinder;

    public RuleKeyResolver(RulesProfile profile, RuleFinder ruleFinder) {
        this.rulesProfile = profile;
        this.ruleFinder = ruleFinder;
    }

    public RuleKey resolve(JQAssistantRuleType type) {
        final Rule rule = ruleFinder.findByKey(JQAssistant.KEY, type.getKey());
        RuleKey ruleKey;
        if (rule != null && rulesProfile.getActiveRule(rule) != null) {
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
