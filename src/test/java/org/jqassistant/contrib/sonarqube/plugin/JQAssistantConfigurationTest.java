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
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class JQAssistantConfigurationTest {

    private static final File PROJECT_DIR = new File("/");
    private static final File MODULE_DIR = new File("/module");

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
    public void disabledNotSet() {
        assertThat(configuration.isSensorDisabled()).isEqualTo(false);
    }

    @Test
    public void disabledSet() {
        doReturn(Optional.of(Boolean.TRUE)).when(sonarConfiguration).getBoolean(JQAssistantConfiguration.DISABLED);

        assertThat(configuration.isSensorDisabled()).isEqualTo(true);
    }

    @Test
    public void getDefaultReportFile() throws IOException {
        File reportFile = configuration.getReportFile(PROJECT_DIR, MODULE_DIR);

        assertThat(reportFile).isEqualTo(new File(PROJECT_DIR, JQAssistantConfiguration.DEFAULT_REPORT_PATH).getCanonicalFile());
    }

    @Test
    public void getRelativeReportFile() {
        doReturn(Optional.of("customReport.xml")).when(sonarConfiguration).get(JQAssistantConfiguration.REPORT_PATH);

        File reportFile = configuration.getReportFile(PROJECT_DIR, MODULE_DIR);

        assertThat(reportFile).isEqualTo(new File(MODULE_DIR, "customReport.xml"));
    }

    @Test
    public void getAbsoluteReportFile() {
        String absoluteReportPath = new File("/customReport.xml").getAbsolutePath();
        doReturn(Optional.of(absoluteReportPath)).when(sonarConfiguration).get(JQAssistantConfiguration.REPORT_PATH);

        File reportFile = configuration.getReportFile(PROJECT_DIR, MODULE_DIR);

        assertThat(reportFile).isEqualTo(new File("/customReport.xml").getAbsoluteFile());
    }


    @Test
    void getPropertyDefinitions() {
        List<PropertyDefinition> propertyDefinitions = JQAssistantConfiguration.getPropertyDefinitions();
        List<String> properties = propertyDefinitions.stream().map(propertyDefinition -> propertyDefinition.key()).collect(toList());
        assertThat(properties).containsExactly(JQAssistantConfiguration.REPORT_PATH, JQAssistantConfiguration.DISABLED);
    }
}
