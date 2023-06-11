package org.jqassistant.tooling.sonarqube.plugin;

import org.jqassistant.tooling.sonarqube.plugin.language.SourceFileResolver;
import org.jqassistant.tooling.sonarqube.plugin.sensor.IssueHandler;
import org.jqassistant.tooling.sonarqube.plugin.sensor.IssueKeyProvider;
import org.jqassistant.tooling.sonarqube.plugin.sensor.JQAssistantSensor;
import org.jqassistant.tooling.sonarqube.plugin.sensor.RulesRepository;
import org.sonar.api.Plugin;

/**
 * Defines the jQAssistant plugin.
 */
public class JQAssistantPlugin implements Plugin {

    public void define(Context context) {
        context.addExtension(JQAssistantSensor.class);
        context.addExtension(SourceFileResolver.class);
        context.addExtension(JQAssistantConfiguration.class);
        context.addExtensions(JQAssistantConfiguration.getPropertyDefinitions());
        context.addExtension(RulesRepository.class);
        context.addExtension(IssueHandler.class);
        context.addExtension(IssueKeyProvider.class);
    }
}
