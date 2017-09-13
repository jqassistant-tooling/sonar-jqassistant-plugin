package org.jqassistant.contrib.sonarqube.plugin.sensor;

import java.util.Map;

import org.jqassistant.contrib.sonarqube.plugin.language.ResourceResolver;
import org.sonar.api.batch.fs.InputComponent;
import com.buschmais.jqassistant.core.report.schema.v1.ColumnType;
import com.buschmais.jqassistant.core.report.schema.v1.ConstraintType;
import com.buschmais.jqassistant.core.report.schema.v1.RowType;

class ConstraintIssueHandler extends AbstractIssueHandler<ConstraintType> {

    ConstraintIssueHandler(InputComponent baseDir, Map<String, ResourceResolver> languageResourceResolvers) {
        super(baseDir, languageResourceResolvers);
    }

    @Override
    protected String getMessage(String ruleId, String ruleDescription, String primaryColumn, RowType rowEntry) {
        if (rowEntry == null) {
            return null;
        }
        String message = ruleId + ": " + ruleDescription;
        String addMessage = buildMessage(rowEntry, primaryColumn);
        if (addMessage.length() > 1) {
            message = message.concat(" [" + addMessage + "]");
        }
        return message;
    }

    private String buildMessage(RowType rowType, String primaryColumn) {
        StringBuilder message = new StringBuilder();
        for (ColumnType column : rowType.getColumn()) {
            String name = column.getName();
            String value = column.getValue();
            if (name.equals(primaryColumn)) {
                continue;
            }
            // log only additional columns, because the resource is already
            // assigned with primary column
            if (message.length() > 0) {
                message.append(", ");
            }
            message.append(name);
            message.append('=');
            message.append(value);
        }
        return message.toString();
    }
}
