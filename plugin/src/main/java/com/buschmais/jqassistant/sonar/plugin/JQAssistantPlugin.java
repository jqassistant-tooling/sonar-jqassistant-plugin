package com.buschmais.jqassistant.sonar.plugin;

import java.util.List;

import org.sonar.api.SonarPlugin;

import com.buschmais.jqassistant.sonar.plugin.language.JavaResourceResolver;
import com.buschmais.jqassistant.sonar.plugin.sensor.JQAssistantRulesRepository;
import com.buschmais.jqassistant.sonar.plugin.sensor.JQAssistantSensor;
import com.buschmais.jqassistant.sonar.plugin.sensor.RuleKeyResolver;
import com.google.common.collect.ImmutableList;

/**
 * Defines the jQAssistant plugin.
 */
public class JQAssistantPlugin extends SonarPlugin {

    /**
     * Return the plugin extensions.
     *
     * @return The plugin extensions.
     */
    @SuppressWarnings("rawtypes")
    public List getExtensions() {
        ImmutableList.Builder<Object> builder = ImmutableList.builder();
        builder.add(JQAssistantSensor.class);
        builder.add(JavaResourceResolver.class);
        builder.add(JQAssistantConfiguration.class);
        builder.addAll(JQAssistantConfiguration.getPropertyDefinitions());
        builder.add(RuleKeyResolver.class);
        builder.add(JQAssistantRulesRepository.class);
        return builder.build();
    }
}
