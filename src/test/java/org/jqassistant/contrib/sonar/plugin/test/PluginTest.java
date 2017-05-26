package org.jqassistant.contrib.sonar.plugin.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.jqassistant.contrib.sonar.plugin.JQAssistantConfiguration;
import org.jqassistant.contrib.sonar.plugin.JQAssistantPlugin;
import org.jqassistant.contrib.sonar.plugin.language.JavaResourceResolver;
import org.jqassistant.contrib.sonar.plugin.sensor.JQAssistantRulesRepository;
import org.jqassistant.contrib.sonar.plugin.sensor.JQAssistantSensor;
import org.jqassistant.contrib.sonar.plugin.sensor.RuleKeyResolver;
import org.junit.Test;

public class PluginTest {

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
