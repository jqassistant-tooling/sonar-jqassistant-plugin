package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scanner.ScannerSide;

import java.util.Optional;

/**
 * @author rzozmann
 */
@ScannerSide
public class RuleKeyResolver {

    private ActiveRules ruleFinder;

    public RuleKeyResolver(ActiveRules ruleFinder) {
        this.ruleFinder = ruleFinder;
    }

    public Optional<RuleKey> resolve(RuleType type) {
        ActiveRule rule = ruleFinder.findByInternalKey(JQAssistant.KEY, type.getKey());
        if (rule != null) {
            return Optional.of(rule.ruleKey());
        }
        return Optional.empty();
    }

}
