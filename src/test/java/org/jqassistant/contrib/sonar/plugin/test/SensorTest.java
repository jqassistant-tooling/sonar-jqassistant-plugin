package org.jqassistant.contrib.sonar.plugin.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import javax.xml.bind.JAXBException;

import org.jqassistant.contrib.sonar.plugin.JQAssistant;
import org.jqassistant.contrib.sonar.plugin.JQAssistantConfiguration;
import org.jqassistant.contrib.sonar.plugin.sensor.JQAssistantRuleType;
import org.jqassistant.contrib.sonar.plugin.sensor.JQAssistantSensor;
import org.jqassistant.contrib.sonar.plugin.sensor.LanguageResourceResolver;
import org.jqassistant.contrib.sonar.plugin.sensor.RuleKeyResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;

/**
 * Verifies the functionality of the
 * {@link JQAssistantSensor}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SensorTest {

	private JQAssistantSensor sensor;

	@Mock
	private RulesProfile rulesProfile;
	@Mock
	private ResourcePerspectives resourcePerspectives;
	@Mock
	private ComponentContainer componentContainer;
	@Mock
	private JQAssistantConfiguration configuration;
	@Mock
	private FileSystem moduleFileSystem;
	@Mock
	private Project project;
	@Mock
	private SensorContext sensorContext;

	@Test
	public void noIssue() throws JAXBException {
		String conceptId = "example:TestConcept";
		String constraintId = "example:TestConstraint";
		Rule concept = Rule.create(JQAssistant.KEY, conceptId, conceptId);
		Rule constraint = Rule.create(JQAssistant.KEY, constraintId, constraintId);
		ActiveRule activeConceptRule = mock(ActiveRule.class);
		ActiveRule activeConstraintRule = mock(ActiveRule.class);
		when(activeConceptRule.getRule()).thenReturn(concept);
		when(activeConstraintRule.getRule()).thenReturn(constraint);
		when(rulesProfile.getActiveRulesByRepository(JQAssistant.KEY)).thenReturn(Arrays.asList(activeConceptRule, activeConstraintRule));
		when(componentContainer.getComponentsByType(LanguageResourceResolver.class)).thenReturn(Collections.<LanguageResourceResolver> emptyList());
		RuleKeyResolver keyResolver = mock(RuleKeyResolver.class);
		when(keyResolver.resolve(any(JQAssistantRuleType.class))).thenReturn(constraint.ruleKey());
		when(componentContainer.getComponentsByType(RuleKeyResolver.class)).thenReturn(Arrays.asList(keyResolver));
		sensor = new JQAssistantSensor(configuration, resourcePerspectives, componentContainer, moduleFileSystem);
		String reportFile = "jqassistant-report-no-issue.xml";
		when(configuration.getReportPath()).thenReturn(reportFile);
        when(moduleFileSystem.baseDir()).thenReturn(new File(SensorTest.class.getResource("/").getFile()));
		Issuable issuable = mock(Issuable.class);

		sensor.analyse(project, sensorContext);

		verify(issuable, never()).addIssue(Mockito.any(Issue.class));
	}

	@Test
	public void createConceptIssue() throws JAXBException {
		String ruleId = "example:TestConcept";
		Rule rule = Rule.create(JQAssistant.KEY, ruleId, ruleId);
		ActiveRule activeRule = mock(ActiveRule.class);
		when(activeRule.getRule()).thenReturn(rule);
		when(rulesProfile.getActiveRulesByRepository(JQAssistant.KEY)).thenReturn(Arrays.asList(activeRule));
		when(componentContainer.getComponentsByType(LanguageResourceResolver.class)).thenReturn(Collections.<LanguageResourceResolver> emptyList());
		RuleKeyResolver keyResolver = mock(RuleKeyResolver.class);
		when(keyResolver.resolve(any(JQAssistantRuleType.class))).thenReturn(rule.ruleKey());
		when(componentContainer.getComponentByType(RuleKeyResolver.class)).thenReturn(keyResolver);
		sensor = new JQAssistantSensor(configuration, resourcePerspectives, componentContainer, moduleFileSystem);
		String reportFile ="jqassistant-report-concept-issue.xml";
		when(configuration.getReportPath()).thenReturn(reportFile);
        when(moduleFileSystem.baseDir()).thenReturn(new File(SensorTest.class.getResource("/").getFile()));
		when(sensorContext.getResource(project)).thenReturn(project);
		Issuable issuable = mock(Issuable.class);
		Issuable.IssueBuilder issueBuilder = mock(Issuable.IssueBuilder.class);
		when(issuable.newIssueBuilder()).thenReturn(issueBuilder);
		when(issueBuilder.ruleKey(any(RuleKey.class))).thenReturn(issueBuilder);
		when(issueBuilder.message(anyString())).thenReturn(issueBuilder);
		when(issueBuilder.line(anyInt())).thenReturn(issueBuilder);
		Issue issue = mock(Issue.class);
		when(issueBuilder.build()).thenReturn(issue);
		when(resourcePerspectives.as(Issuable.class, (Resource) project)).thenReturn(issuable);

		sensor.analyse(project, sensorContext);

		verify(issuable).addIssue(issue);
		verify(issueBuilder).message(contains("The concept"));
		verify(issueBuilder).ruleKey(rule.ruleKey());
	}

	@Test
	public void createConstraintIssue() throws JAXBException {
		String ruleId = "example:TestConstraint";
		Rule rule = Rule.create(JQAssistant.KEY, ruleId, ruleId);
		ActiveRule activeRule = mock(ActiveRule.class);
		when(activeRule.getRule()).thenReturn(rule);
		when(rulesProfile.getActiveRulesByRepository(JQAssistant.KEY)).thenReturn(Arrays.asList(activeRule));
		LanguageResourceResolver resourceResolver = mock(LanguageResourceResolver.class);
		when(resourceResolver.getLanguage()).thenReturn("Java");
		Resource javaResource = mock(Resource.class);
		when(resourceResolver.resolve(project, "WriteField", "com/buschmais/jqassistant/examples/sonar/project/Bar.class", "com.buschmais.jqassistant.examples.sonar.project.Bar#void setValue(java.lang.String)")).thenReturn(
				javaResource);
		when(componentContainer.getComponentsByType(LanguageResourceResolver.class)).thenReturn(Arrays.asList(resourceResolver));
		RuleKeyResolver keyResolver = mock(RuleKeyResolver.class);
		when(keyResolver.resolve(any(JQAssistantRuleType.class))).thenReturn(rule.ruleKey());
		when(componentContainer.getComponentByType(RuleKeyResolver.class)).thenReturn(keyResolver);
		sensor = new JQAssistantSensor(configuration, resourcePerspectives, componentContainer, moduleFileSystem);
		String reportFile = "jqassistant-report-constraint-issue.xml";
		when(configuration.getReportPath()).thenReturn(reportFile);
        when(moduleFileSystem.baseDir()).thenReturn(new File(SensorTest.class.getResource("/").getFile()));
		when(sensorContext.getResource(javaResource)).thenReturn(mock(Resource.class));
		Issuable issuable = mock(Issuable.class);
		Issuable.IssueBuilder issueBuilder = mock(Issuable.IssueBuilder.class);
		when(issuable.newIssueBuilder()).thenReturn(issueBuilder);
		when(issueBuilder.ruleKey(any(RuleKey.class))).thenReturn(issueBuilder);
		when(issueBuilder.message(anyString())).thenReturn(issueBuilder);
		when(issueBuilder.line(anyInt())).thenReturn(issueBuilder);
		Issue issue = mock(Issue.class);
		when(issueBuilder.build()).thenReturn(issue);
		when(resourcePerspectives.as(Matchers.eq(Issuable.class), any(Resource.class))).thenReturn(issuable);

		sensor.analyse(project, sensorContext);

		verify(issuable).addIssue(issue);
		verify(issueBuilder).line(16);
		verify(issueBuilder).message(contains("A test constraint."));
		verify(issueBuilder).ruleKey(rule.ruleKey());
	}
}
