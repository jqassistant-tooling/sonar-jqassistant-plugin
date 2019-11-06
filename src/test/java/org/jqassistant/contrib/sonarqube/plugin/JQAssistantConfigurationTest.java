package org.jqassistant.contrib.sonarqube.plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class JQAssistantConfigurationTest {

    @Mock
    private SensorContext context;

    @Mock
    private Configuration sonarConfiguration;

    private JQAssistantConfiguration configuration;

    @BeforeEach
    public void setUp() {
        configuration = new JQAssistantConfiguration(sonarConfiguration);
    }

    @Test
    public void getDefaultReportFile() {
        File projectPath = new File(".");

        File reportFile = configuration.getReportFile(projectPath);

        assertThat(reportFile).isEqualTo(new File(projectPath, JQAssistantConfiguration.DEFAULT_REPORT_PATH));
    }

    @Test
    public void getCustomReportFile() {
        File projectPath = new File(".");
        doReturn(Optional.of("customReport.xml")).when(sonarConfiguration).get(JQAssistantConfiguration.REPORT_PATH);

        File reportFile = configuration.getReportFile(projectPath);

        assertThat(reportFile).isEqualTo(new File(projectPath, "customReport.xml"));
    }

    @Test
    void getPropertyDefinitions() {
        List<PropertyDefinition> propertyDefinitions = JQAssistantConfiguration.getPropertyDefinitions();
        List<String> properties = propertyDefinitions.stream().map(propertyDefinition -> propertyDefinition.key()).collect(toList());
        assertThat(properties).containsExactly(JQAssistantConfiguration.REPORT_PATH, JQAssistantConfiguration.DISABLED);
    }
}
