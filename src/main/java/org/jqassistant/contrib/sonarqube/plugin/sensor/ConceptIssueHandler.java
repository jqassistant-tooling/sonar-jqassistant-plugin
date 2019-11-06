package org.jqassistant.contrib.sonarqube.plugin.sensor;

import com.buschmais.jqassistant.core.report.schema.v1.ConceptType;
import org.jqassistant.contrib.sonarqube.plugin.language.ResourceResolver;
import org.sonar.api.batch.sensor.SensorContext;

import java.io.File;
import java.util.Map;

class ConceptIssueHandler extends AbstractIssueHandler<ConceptType> {

    ConceptIssueHandler(SensorContext sensorContext, Map<String, ResourceResolver> languageResourceResolvers, File projectPath) {
        super(sensorContext, languageResourceResolvers, projectPath);
    }

    @Override
    protected String getMessage(ConceptType ruleDescription) {
        return "The concept could not be applied: " + ruleDescription.getDescription();
    }

}
