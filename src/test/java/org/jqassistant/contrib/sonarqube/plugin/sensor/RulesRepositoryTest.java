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

import static org.jqassistant.contrib.sonarqube.plugin.sensor.RulesRepository.*;
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

    private RulesRepository rulesRepository = new RulesRepository();

    @BeforeEach
    public void setUp() {
        doReturn(newRepository).when(context).createRepository(JQAssistant.KEY, Java.KEY);
        doReturn(conceptRule).when(newRepository).createRule(INVALID_CONCEPT_KEY);
        doReturn(constraintRule).when(newRepository).createRule(CONSTRAINT_VIOLATION_KEY);
    }

    @Test
    public void defineRules() {
        rulesRepository.define(context);

        verify(newRepository).createRule(INVALID_CONCEPT_KEY);
        verify(conceptRule).setName(INVALID_CONCEPT_RULE_NAME);
        verify(conceptRule).setInternalKey(INVALID_CONCEPT_KEY);
        verify(conceptRule).setSeverity(MINOR);

        verify(newRepository).createRule(CONSTRAINT_VIOLATION_KEY);
        verify(constraintRule).setName(CONSTRAINT_VIOLATION_RULE_NAME);
        verify(constraintRule).setInternalKey(CONSTRAINT_VIOLATION_KEY);
        verify(constraintRule).setSeverity(MAJOR);

        verify(newRepository).done();
    }

}
