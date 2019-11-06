package org.jqassistant.contrib.sonarqube.plugin.sensor;

import com.buschmais.jqassistant.core.report.schema.v1.ConstraintType;
import org.jqassistant.contrib.sonarqube.plugin.language.ResourceResolver;
import org.sonar.api.batch.sensor.SensorContext;

import java.io.File;
import java.util.Map;

class ConstraintIssueHandler extends AbstractIssueHandler<ConstraintType> {

    ConstraintIssueHandler(SensorContext sensorContext, Map<String, ResourceResolver> languageResourceResolvers, File projectPath) {
        super(sensorContext, languageResourceResolvers, projectPath);
    }

    @Override
    protected String getMessage(ConstraintType ruleDescription) {
        return ruleDescription.getDescription();
    }

}
