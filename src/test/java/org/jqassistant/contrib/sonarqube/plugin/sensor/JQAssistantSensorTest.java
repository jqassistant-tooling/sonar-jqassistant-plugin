package org.jqassistant.contrib.sonarqube.plugin.sensor;

import java.io.File;
import java.net.URISyntaxException;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistantConfiguration;
import org.jqassistant.schema.report.v2.ExecutableRuleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;

import static org.mockito.Mockito.*;

/**
 * Verifies the functionality of the {@link JQAssistantSensor}.
 */
@ExtendWith(MockitoExtension.class)
public class JQAssistantSensorTest {

    @Mock
    private SensorDescriptor sensorDescriptor;

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
    void setUp() throws URISyntaxException {
        baseDir = new File(JQAssistantSensorTest.class.getResource("/").toURI().getPath());
        sensor = new JQAssistantSensor(configuration, issueHandler);
    }

    @Test
    void describe() {
        sensor.describe(sensorDescriptor);

        verify(sensorDescriptor).name("JQA");
    }

    @Test
    void sensorDisabled() {
        doReturn(true).when(configuration).isSensorDisabled();

        sensor.execute(sensorContext);

        verify(configuration, never()).getReportFile();
    }

    @ParameterizedTest
    @ValueSource(strings = { "jqassistant-report-concept-issue.xml", "jqassistant-report-1_8.xml", "jqassistant-report-constraint-issue.xml",
        "jqassistant-report-constraint-issue-1.8.xml", "jqassistant-report-constraint-issue-2.0.xml" })
    void issues(String reportFile) {
        stubFileSystem(reportFile);

        sensor.execute(sensorContext);

        verify(issueHandler).process(eq(sensorContext), any(File.class), any(ExecutableRuleType.class));
    }

    @Test
    void noExistentReport() {
        stubFileSystem("non-existent-report.xml");

        sensor.execute(sensorContext);

        verify(issueHandler, never()).process(eq(sensorContext), any(File.class), any(ExecutableRuleType.class));
    }

    @Test
    void reportWithoutIssue() {
        stubFileSystem("jqassistant-report-no-issue.xml");

        sensor.execute(sensorContext);

        verify(issueHandler, never()).process(eq(sensorContext), any(File.class), any(ExecutableRuleType.class));
    }

    private void stubFileSystem(String reportFile) {
        doReturn(reportFile).when(configuration).getReportFile();
        doReturn(baseDir).when(fileSystem).baseDir();
        doReturn(fileSystem).when(sensorContext).fileSystem();
    }
}
