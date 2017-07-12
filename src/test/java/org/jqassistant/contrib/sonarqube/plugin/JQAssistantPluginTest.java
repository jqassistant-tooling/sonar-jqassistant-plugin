package org.jqassistant.contrib.sonarqube.plugin;

import java.util.List;

import org.jqassistant.contrib.sonarqube.plugin.language.JavaResourceResolver;
import org.jqassistant.contrib.sonarqube.plugin.sensor.JQAssistantRulesRepository;
import org.jqassistant.contrib.sonarqube.plugin.sensor.JQAssistantSensor;
import org.jqassistant.contrib.sonarqube.plugin.sensor.RuleKeyResolver;
import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.utils.Version;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class JQAssistantPluginTest {

    @Test
    public void extensions() {
        JQAssistantPlugin plugin = new JQAssistantPlugin();
        Plugin.Context context = new Plugin.Context(Version.parse("1.1.1-1"));
        plugin.define(context);
        List<?> extensions = context.getExtensions();
        assertThat(extensions.contains(JQAssistantConfiguration.class), equalTo(true));
        assertThat(extensions.contains(JQAssistantSensor.class), equalTo(true));
        assertThat(extensions.contains(JavaResourceResolver.class), equalTo(true));
        assertThat(extensions.contains(RuleKeyResolver.class), equalTo(true));
        assertThat(extensions.contains(JQAssistantRulesRepository.class), equalTo(true));
    }
}
