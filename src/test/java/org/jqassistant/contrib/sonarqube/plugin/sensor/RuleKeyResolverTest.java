package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RuleKeyResolverTest {

    @Mock
    private ActiveRules activeRules;

    @Mock
    private ActiveRule constraintRule;

    @Mock
    private ActiveRule conceptRule;

    private RuleKeyResolver ruleKeyResolver;

    @BeforeEach
    public void setUp() {
        ruleKeyResolver = new RuleKeyResolver(activeRules);
        doReturn(mock(RuleKey.class)).when(constraintRule).ruleKey();
        doReturn(mock(RuleKey.class)).when(conceptRule).ruleKey();
    }

    @Test
    void resolve() {
        doReturn(constraintRule).when(activeRules).findByInternalKey(JQAssistant.KEY, RuleType.CONSTRAINT.getKey());
        doReturn(conceptRule).when(activeRules).findByInternalKey(JQAssistant.KEY, RuleType.CONCEPT.getKey());

        Optional<RuleKey> constraintKey = ruleKeyResolver.resolve(RuleType.CONSTRAINT);
        Optional<RuleKey> conceptKey = ruleKeyResolver.resolve(RuleType.CONCEPT);

        assertThat(constraintKey.isPresent()).isEqualTo(true);
        assertThat(conceptKey.isPresent()).isEqualTo(true);
    }
}
