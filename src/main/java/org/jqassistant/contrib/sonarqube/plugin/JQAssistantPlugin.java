package org.jqassistant.contrib.sonarqube.plugin;

import org.jqassistant.contrib.sonarqube.plugin.language.SourceFileResolver;
import org.jqassistant.contrib.sonarqube.plugin.sensor.*;
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
        context.addExtension(ReportReader.class);
    }
}
