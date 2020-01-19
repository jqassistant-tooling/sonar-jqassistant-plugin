package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistantConfiguration;
import org.jqassistant.schema.report.v1.ExecutableRuleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.SensorContext;

import java.io.File;
import java.net.URISyntaxException;

import static org.mockito.Mockito.*;

/**
 * Verifies the functionality of the {@link JQAssistantSensor}.
 */
@ExtendWith(MockitoExtension.class)
public class JQAssistantSensorTest {

    @Mock
    private JQAssistantConfiguration configuration;

    @Mock
    private FileSystem fileSystem;

    @Mock
    private SensorContext sensorContext;

    @Mock
    private IssueHandler issueHandler;

    private File baseDir;

    private JQAssistantSensor sensor;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        baseDir = new File(JQAssistantSensorTest.class.getResource("/").toURI().getPath());
    }

    @Test
    public void noIssue() {
        sensor = new JQAssistantSensor(configuration, issueHandler);
        stubFileSystem("jqassistant-report-no-issue.xml");

        sensor.execute(sensorContext);

        verify(issueHandler, never()).process(eq(sensorContext), any(File.class), any(ExecutableRuleType.class));
    }

    @Test
    public void createConceptIssue() {
        sensor = new JQAssistantSensor(configuration, issueHandler);
        stubFileSystem("jqassistant-report-concept-issue.xml");

        sensor.execute(sensorContext);

        verify(issueHandler).process(eq(sensorContext), any(File.class), any(ExecutableRuleType.class));
    }

    @Test
    public void createConceptIssue_1_8() {
        sensor = new JQAssistantSensor(configuration, issueHandler);
        stubFileSystem("jqassistant-report-1_8.xml");

        sensor.execute(sensorContext);

        verify(issueHandler).process(eq(sensorContext), any(File.class), any(ExecutableRuleType.class));
    }

    @Test
    public void createConstraintIssue() {
        sensor = new JQAssistantSensor(configuration, issueHandler);
        stubFileSystem("jqassistant-report-constraint-issue.xml");

        sensor.execute(sensorContext);

        verify(issueHandler).process(eq(sensorContext), any(File.class), any(ExecutableRuleType.class));
    }

    private void stubFileSystem(String reportFile) {
        doReturn(new File(baseDir, reportFile)).when(configuration).getReportFile(baseDir, baseDir);
        doReturn(baseDir).when(fileSystem).baseDir();
        doReturn(fileSystem).when(sensorContext).fileSystem();
    }
}
