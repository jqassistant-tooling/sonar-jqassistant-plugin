package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.Context;
import org.sonar.api.server.rule.RulesDefinition.NewRepository;
import org.sonar.plugins.java.Java;

import static org.jqassistant.contrib.sonarqube.plugin.sensor.RuleType.CONCEPT;
import static org.jqassistant.contrib.sonarqube.plugin.sensor.RuleType.CONSTRAINT;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;

@ExtendWith(MockitoExtension.class)
class RulesRepositoryTest {

    @Mock
    private Context context;

    @Mock
    private NewRepository newRepository;

    @Mock
    private RulesDefinition.NewRule conceptRule;

    @Mock
    private RulesDefinition.NewRule constraintRule;

    private final RulesRepository rulesRepository = new RulesRepository();

    @BeforeEach
    void setUp() {
        doReturn(newRepository).when(context).createRepository(JQAssistant.KEY, Java.KEY);
        doReturn(conceptRule).when(newRepository).createRule(CONCEPT.getKey());
        doReturn(constraintRule).when(newRepository).createRule(CONSTRAINT.getKey());
    }

    @Test
    void defineRules() {
        rulesRepository.define(context);

        verify(newRepository).createRule(CONCEPT.getKey());
        verify(conceptRule).setName(CONCEPT.getName());
        verify(conceptRule).setInternalKey(CONCEPT.getKey());
        verify(conceptRule).setSeverity(MINOR);
        verify(conceptRule).setHtmlDescription(anyString());

        verify(newRepository).createRule(CONSTRAINT.getKey());
        verify(constraintRule).setName(CONSTRAINT.getName());
        verify(constraintRule).setInternalKey(CONSTRAINT.getKey());
        verify(constraintRule).setSeverity(MAJOR);
        verify(constraintRule).setHtmlDescription(anyString());

        verify(newRepository).done();
    }

}
