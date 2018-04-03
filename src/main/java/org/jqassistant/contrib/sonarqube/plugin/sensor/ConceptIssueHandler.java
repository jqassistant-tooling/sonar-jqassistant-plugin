package org.jqassistant.contrib.sonarqube.plugin.sensor;

import java.util.Map;

import org.jqassistant.contrib.sonarqube.plugin.language.ResourceResolver;
import org.sonar.api.batch.fs.InputComponent;
import com.buschmais.jqassistant.core.report.schema.v1.ConceptType;
import com.buschmais.jqassistant.core.report.schema.v1.RowType;

class ConceptIssueHandler extends AbstractIssueHandler<ConceptType> {

    ConceptIssueHandler(InputComponent baseDir, Map<String, ResourceResolver> languageResourceResolvers) {
        super(baseDir, languageResourceResolvers);
    }

    @Override
    protected SourceLocation determineAlternativeResource(RowType rowType) {
        if (rowType == null) {
            // use project as anchor in case of not given resource
            return new SourceLocation(getBaseDir(), null);
        }
        return null;
    }

    @Override
    protected String getMessage(InputComponent resourceResolved, String ruleId, String ruleDescription, String primaryColumn, RowType rowEntry) {
        return ruleId + ": The concept could not be applied.";
    }
}
