package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.jqassistant.contrib.sonarqube.plugin.JQAssistantConfiguration;
import org.jqassistant.contrib.sonarqube.plugin.language.JavaResourceResolver;
import org.jqassistant.contrib.sonarqube.plugin.language.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.fs.internal.DefaultTextRange;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collections;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.*;

/**
 * Verifies the functionality of the
 * {@link JQAssistantSensor}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JQAssistantSensorTest {

    private JQAssistantSensor sensor;

    @Mock
    private RulesProfile rulesProfile;
    @Mock
    private ComponentContainer componentContainer;
    @Mock
    private JQAssistantConfiguration configuration;
    @Mock
    private FileSystem fileSystem;
    @Mock
    private SensorContext sensorContext;
    @Mock
    private ActiveRules activeRules;
    @Mock
    NewIssue newIssue;
    @Mock
    NewIssueLocation newIssueLocation;

    private File baseDir;

    @Before
    public void setUp() throws URISyntaxException {
        baseDir = new File(JQAssistantSensorTest.class.getResource("/").toURI().getPath());
    }

    @Test
    public void noIssue() {
        String conceptId = "example:TestConcept";
        String constraintId = "example:TestConstraint";
        Rule concept = Rule.create(JQAssistant.KEY, conceptId, conceptId);
        Rule constraint = Rule.create(JQAssistant.KEY, constraintId, constraintId);
        ActiveRule activeConceptRule = mock(ActiveRule.class);
        ActiveRule activeConstraintRule = mock(ActiveRule.class);
        when(activeConceptRule.getRule()).thenReturn(concept);
        when(activeConstraintRule.getRule()).thenReturn(constraint);
        when(rulesProfile.getActiveRulesByRepository(JQAssistant.KEY)).thenReturn(
            asList(activeConceptRule, activeConstraintRule));
        when(componentContainer.getComponentsByType(ResourceResolver.class)).thenReturn(Collections.emptyList());
        RuleKeyResolver keyResolver = mock(RuleKeyResolver.class);
        when(keyResolver.resolve(any(JQAssistantRuleType.class))).thenReturn(constraint.ruleKey());
        when(componentContainer.getComponentsByType(RuleKeyResolver.class)).thenReturn(asList(keyResolver));
        sensor = new JQAssistantSensor(configuration, new JavaResourceResolver(), new RuleKeyResolver(activeRules));
        String reportFile = "jqassistant-report-no-issue.xml";
        when(configuration.getReportPath()).thenReturn(reportFile);
        when(fileSystem.baseDir()).thenReturn(baseDir);
        Issuable issuable = mock(Issuable.class);
        when(sensorContext.fileSystem()).thenReturn(fileSystem);

        sensor.execute(sensorContext);

        verify(issuable, never()).addIssue(any(Issue.class));
    }


    @Test
    public void createConceptIssue() {
        Rule rule = stubRule("example:TestConcept");
        stubNewIssue(rule);
        when(componentContainer.getComponentsByType(ResourceResolver.class)).thenReturn(Collections.emptyList());
        RuleKeyResolver keyResolver = stubRuleKeyResolver(rule);
        sensor = new JQAssistantSensor(configuration, new JavaResourceResolver(), keyResolver);
        stubFileSystem("jqassistant-report-concept-issue.xml");

        sensor.execute(sensorContext);

        verify(sensorContext, times(1)).newIssue();
        verify(newIssueLocation).message(contains("The concept"));
        verify(newIssue, times(1)).forRule(rule.ruleKey());
    }

    @Test
    public void createConstraintIssue() {
        Rule rule = stubRule("example:TestConstraint");
        stubNewIssue(rule);
        ResourceResolver resourceResolver = mock(JavaResourceResolver.class);
        when(resourceResolver.getLanguage()).thenReturn("Java");
        InputPath javaResource = mock(InputPath.class, withSettings().extraInterfaces(InputFile.class));
        when(resourceResolver.resolve(any(FileSystem.class), any(String.class), any(String.class), any(String.class))).thenReturn(javaResource);
        when(((InputFile) javaResource).newRange(16, 0, 16, 0))
            .thenReturn(new DefaultTextRange(new DefaultTextPointer(16, 0), new DefaultTextPointer(16, 0)));
        when(componentContainer.getComponentsByType(ResourceResolver.class)).thenReturn(asList(resourceResolver));
        RuleKeyResolver keyResolver = stubRuleKeyResolver(rule);
        sensor = new JQAssistantSensor(configuration, (JavaResourceResolver) resourceResolver, keyResolver);
        stubFileSystem("jqassistant-report-constraint-issue.xml");

        sensor.execute(sensorContext);

        verify(sensorContext, times(2)).newIssue();
        verify(newIssueLocation, times(2))
            .at(new DefaultTextRange(new DefaultTextPointer(16, 0), new DefaultTextPointer(16, 0)));
        verify(newIssueLocation, times(2)).message(contains("A test constraint."));
        verify(newIssue, times(2)).forRule(rule.ruleKey());
    }

    private RuleKeyResolver stubRuleKeyResolver(Rule rule) {
        RuleKeyResolver keyResolver = mock(RuleKeyResolver.class);
        when(keyResolver.resolve(any(JQAssistantRuleType.class))).thenReturn(rule.ruleKey());
        when(componentContainer.getComponentByType(RuleKeyResolver.class)).thenReturn(keyResolver);
        return keyResolver;
    }

    private void stubFileSystem(String reportFile) {
        when(configuration.getReportPath()).thenReturn(reportFile);
        when(fileSystem.baseDir()).thenReturn(baseDir);
        when(sensorContext.fileSystem()).thenReturn(fileSystem);
    }

    private Rule stubRule(String ruleId) {
        Rule rule = Rule.create(JQAssistant.KEY, ruleId, ruleId);
        ActiveRule activeRule = mock(ActiveRule.class);
        when(activeRule.getRule()).thenReturn(rule);
        when(rulesProfile.getActiveRulesByRepository(JQAssistant.KEY)).thenReturn(asList(activeRule));
        return rule;
    }

    private void stubNewIssue(Rule rule) {
        when(sensorContext.newIssue()).thenReturn(newIssue);
        when(newIssue.newLocation()).thenReturn(newIssueLocation);
        when(newIssue.forRule(rule.ruleKey())).thenReturn(newIssue);
        when(newIssueLocation.message(any(String.class))).thenReturn(newIssueLocation);
    }

}
