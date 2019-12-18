package org.jqassistant.contrib.sonarqube.plugin.sensor;

import com.buschmais.jqassistant.core.report.schema.v1.ExecutableRuleType;
import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.jqassistant.contrib.sonarqube.plugin.JQAssistantConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * Verifies the functionality of the {@link JQAssistantSensor}.
 */
@ExtendWith(MockitoExtension.class)
public class JQAssistantSensorTest {

    private JQAssistantSensor sensor;

    @Mock
    private JQAssistantConfiguration configuration;

    @Mock
    private FileSystem fileSystem;

    @Mock
    private SensorContext sensorContext;

    @Mock
    private ActiveRules activeRules;

    @Mock
    private IssueHandler issueHandler;

    private File baseDir;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        baseDir = new File(JQAssistantSensorTest.class.getResource("/").toURI().getPath());
    }

    @Test
    public void noIssue() {
        sensor = new JQAssistantSensor(configuration, new RuleKeyResolver(activeRules), issueHandler);
        stubFileSystem("jqassistant-report-no-issue.xml");

        sensor.execute(sensorContext);

        verify(issueHandler, never()).process(eq(sensorContext), any(File.class), any(RuleType.class), any(ExecutableRuleType.class), any(RuleKey.class));
    }

    @Test
    public void createConceptIssue() {
        Rule rule = Rule.create(JQAssistant.KEY, "example:TestConcept", "example:TestConcept");
        RuleKeyResolver keyResolver = stubRuleKeyResolver(rule);
        sensor = new JQAssistantSensor(configuration, keyResolver, issueHandler);
        stubFileSystem("jqassistant-report-concept-issue.xml");

        sensor.execute(sensorContext);

        verify(issueHandler).process(eq(sensorContext), any(File.class), eq(RuleType.CONCEPT), any(ExecutableRuleType.class), any(RuleKey.class));
    }

    @Test
    public void createConstraintIssue() {
        Rule rule = Rule.create(JQAssistant.KEY, "example:TestConstraint", "example:TestConstraint");
        RuleKeyResolver keyResolver = stubRuleKeyResolver(rule);
        sensor = new JQAssistantSensor(configuration, keyResolver, issueHandler);
        stubFileSystem("jqassistant-report-constraint-issue.xml");

        sensor.execute(sensorContext);

        verify(issueHandler).process(eq(sensorContext), any(File.class), eq(RuleType.CONSTRAINT), any(ExecutableRuleType.class), any(RuleKey.class));
    }

    private RuleKeyResolver stubRuleKeyResolver(Rule rule) {
        RuleKeyResolver keyResolver = mock(RuleKeyResolver.class);
        when(keyResolver.resolve(any(RuleType.class))).thenReturn(Optional.of(rule.ruleKey()));
        return keyResolver;
    }

    private void stubFileSystem(String reportFile) {
        doReturn(new File(baseDir, reportFile)).when(configuration).getReportFile(baseDir, baseDir);
        doReturn(baseDir).when(fileSystem).baseDir();
        doReturn(fileSystem).when(sensorContext).fileSystem();
    }
}
