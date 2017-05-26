package org.jqassistant.contrib.sonarqube.plugin;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistantConfiguration;
import org.jqassistant.contrib.sonarqube.plugin.JQAssistantPlugin;
import org.jqassistant.contrib.sonarqube.plugin.language.JavaResourceResolver;
import org.jqassistant.contrib.sonarqube.plugin.sensor.JQAssistantRulesRepository;
import org.jqassistant.contrib.sonarqube.plugin.sensor.JQAssistantSensor;
import org.jqassistant.contrib.sonarqube.plugin.sensor.RuleKeyResolver;
import org.junit.Test;

public class JQAssistantPluginTest {

    @Test
    public void extensions() {
        JQAssistantPlugin plugin = new JQAssistantPlugin();
        List<?> extensions = plugin.getExtensions();
        assertThat(extensions.contains(JQAssistantConfiguration.class), equalTo(true));
        assertThat(extensions.contains(JQAssistantSensor.class), equalTo(true));
        assertThat(extensions.contains(JavaResourceResolver.class), equalTo(true));
        assertThat(extensions.contains(RuleKeyResolver.class), equalTo(true));
        assertThat(extensions.contains(JQAssistantRulesRepository.class), equalTo(true));
    }
}
