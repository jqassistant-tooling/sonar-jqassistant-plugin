package org.jqassistant.contrib.sonarqube.plugin;

import org.jqassistant.contrib.sonarqube.plugin.language.SourceFileResolver;
import org.jqassistant.contrib.sonarqube.plugin.sensor.JQAssistantSensor;
import org.jqassistant.contrib.sonarqube.plugin.sensor.RuleKeyResolver;
import org.jqassistant.contrib.sonarqube.plugin.sensor.RulesRepository;
import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.Version;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JQAssistantPluginTest {

    @Test
    public void extensions() {
        JQAssistantPlugin plugin = new JQAssistantPlugin();
        SonarRuntime sonarRuntime = mock(SonarRuntime.class);
        when(sonarRuntime.getApiVersion()).thenReturn(Version.parse("1.1.1-1"));
        Plugin.Context context = new Plugin.Context(sonarRuntime);
        plugin.define(context);
        List<?> extensions = context.getExtensions();
        assertThat(extensions.contains(JQAssistantConfiguration.class), equalTo(true));
        assertThat(extensions.contains(JQAssistantSensor.class), equalTo(true));
        assertThat(extensions.contains(SourceFileResolver.class), equalTo(true));
        assertThat(extensions.contains(RuleKeyResolver.class), equalTo(true));
        assertThat(extensions.contains(RulesRepository.class), equalTo(true));
    }
}
