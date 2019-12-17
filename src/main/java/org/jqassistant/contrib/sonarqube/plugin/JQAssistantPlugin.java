package org.jqassistant.contrib.sonarqube.plugin;

import org.jqassistant.contrib.sonarqube.plugin.language.JavaResourceResolver;
import org.jqassistant.contrib.sonarqube.plugin.sensor.RulesRepository;
import org.jqassistant.contrib.sonarqube.plugin.sensor.JQAssistantSensor;
import org.jqassistant.contrib.sonarqube.plugin.sensor.RuleKeyResolver;
import org.sonar.api.Plugin;

/**
 * Defines the jQAssistant plugin.
 */
public class JQAssistantPlugin implements Plugin {

    public void define(Context context) {
        context.addExtension(JQAssistantSensor.class);
        context.addExtension(JavaResourceResolver.class);
        context.addExtension(JQAssistantConfiguration.class);
        context.addExtensions(JQAssistantConfiguration.getPropertyDefinitions());
        context.addExtension(RuleKeyResolver.class);
        context.addExtension(RulesRepository.class);
    }
}
