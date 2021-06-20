package org.jqassistant.contrib.sonarqube.plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jqassistant.contrib.sonarqube.plugin.JQAssistantConfiguration.*;
import static org.mockito.Mockito.doReturn;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;

@ExtendWith(MockitoExtension.class)
class JQAssistantConfigurationTest {

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
        doReturn(Optional.of(Boolean.TRUE)).when(sonarConfiguration).getBoolean(DISABLED);

        assertThat(configuration.isSensorDisabled()).isEqualTo(true);
    }

    @Test
    public void getDefaultReportFile() {
        assertThat(configuration.getReportFile()).isEqualTo(JQAssistantConfiguration.DEFAULT_REPORT_PATH);
    }

    @Test
    public void getRelativeReportFile() {
        doReturn(Optional.of("customReport.xml")).when(sonarConfiguration).get(REPORT_PATH);

        assertThat(configuration.getReportFile()).isEqualTo("customReport.xml");
    }

    @Test
    void getPropertyDefinitions() {
        List<PropertyDefinition> propertyDefinitions = JQAssistantConfiguration.getPropertyDefinitions();
        List<String> properties = propertyDefinitions.stream().map(PropertyDefinition::key).collect(toList());
        assertThat(properties).containsExactly(REPORT_PATH, DISABLED, ISSUE_TYPE);
    }

    @Test
    void getDefaultIssueType() {
        assertThat(configuration.getIssueType()).isEqualTo(CODE_SMELL);
    }

    @Test
    void getUpperCaseIssueType() {
        doReturn(Optional.of("BUG")).when(sonarConfiguration).get(ISSUE_TYPE);
        assertThat(configuration.getIssueType()).isEqualTo(BUG);
    }

    @Test
    void getLowerCaseIssueType() {
        doReturn(Optional.of("bug")).when(sonarConfiguration).get(ISSUE_TYPE);
        assertThat(configuration.getIssueType()).isEqualTo(BUG);
    }

}
