package org.jqassistant.contrib.sonarqube.plugin.sensor;

import com.buschmais.jqassistant.core.report.schema.v1.*;
import org.jqassistant.contrib.sonarqube.plugin.language.ResourceResolver;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Base class to produce a number of violations defined by instance of {@link T}.
 *
 * @author rzozmann
 */
abstract class AbstractIssueHandler<T extends ExecutableRuleType> {

    private static final String NEWLINE = "\n";
    private final SensorContext sensorContext;

    private final Map<String, ResourceResolver> languageResourceResolvers;

    private final File projectPath;

    protected AbstractIssueHandler(SensorContext sensorContext, Map<String, ResourceResolver> languageResourceResolvers, File projectPath) {
        this.sensorContext = sensorContext;
        this.languageResourceResolvers = languageResourceResolvers;
        this.projectPath = projectPath;
    }

    /**
     * Create 0..n violations, based on content and type of <i>ruleType</i>.
     */
    public final void process(T ruleType, RuleKey ruleKey) {
        ResultType result = ruleType.getResult();
        if (result == null) {
            //'result' may be null for not applied (failed) concepts
            createIssue(Optional.empty(), ruleType, ruleKey, null);
        } else {
            String primaryColumn = getPrimaryColumn(result);
            for (RowType rowType : result.getRows()
                .getRow()) {
                Optional<SourceLocation> target = resolveRelatedResource(rowType, primaryColumn);
                createIssue(target, ruleType, ruleKey, rowType);
            }
        }
    }

    private void createIssue(Optional<SourceLocation> target, T ruleType, RuleKey ruleKey, RowType rowType) {
        StringBuilder message = new StringBuilder().append('[')
            .append(ruleType.getId())
            .append("]")
            .append(" ")
            .append(getMessage(ruleType));
        appendResult(rowType, message);
        Optional<Severity> severity = convertSeverity(ruleType.getSeverity());
        if (target.isPresent()) {
            SourceLocation sourceLocation = target.get();
            Optional<InputComponent> inputComponent = sourceLocation.getResource();
            if (inputComponent.isPresent()) {
                // Create an issue if a SourceLocation exists and InputComponent could be resolved (e.g. a class in a module)
                createIssue(ruleKey, message.toString(), severity, inputComponent.get(), sourceLocation.getLineNumber());
            }
        } else if (sensorContext.fileSystem()
            .baseDir()
            .equals(projectPath)) {
            // Create issue on project level for all items that cannot be mapped to a SourceLocation (e.g. packages or empty concepts)
            createIssue(ruleKey, message.toString(), severity, sensorContext.project(), Optional.empty());
        }
    }

    private void createIssue(RuleKey ruleKey, String message, Optional<Severity> severity, InputComponent inputComponent,
        Optional<Integer> lineNumber) {
        NewIssue newIssue = sensorContext.newIssue();
        NewIssueLocation newIssueLocation = newIssue.newLocation()
            .message(message);
        newIssueLocation.on(inputComponent);
        if (lineNumber.isPresent()) {
            TextRange textRange = toTextRange((InputFile) inputComponent, lineNumber.get());
            newIssueLocation.at(textRange);
        }
        severity.ifPresent(s -> newIssue.overrideSeverity(s));
        newIssue.forRule(ruleKey)
            .at(newIssueLocation);
        newIssue.save();
    }

    private TextRange toTextRange(InputFile inputFile, Integer lineNumber) {
        return inputFile.newRange(lineNumber, 0, lineNumber, 0);
    }

    private Optional<Severity> convertSeverity(SeverityType severity) {
        if (severity == null) {
            return Optional.empty();
        }
        switch (severity.getLevel()) {
            case 0:
                return Optional.of(Severity.BLOCKER);
            case 1:
                return Optional.of(Severity.CRITICAL);
            case 2:
                return Optional.of(Severity.MAJOR);
            case 3:
                return Optional.of(Severity.MINOR);
            case 4:
                return Optional.of(Severity.INFO);
            default:
                return Optional.empty();
        }
    }

    /**
     * Determine the primary column from the result, i.e. the column which contains the resource to create an issue for.
     *
     * @param result The result.
     * @return The name of the primary column or <code>null</code>.
     */
    private String getPrimaryColumn(ResultType result) {
        if (result == null) {
            return null;
        }
        ColumnsHeaderType columns = result.getColumns();
        for (ColumnHeaderType columnHeaderType : columns.getColumn()) {
            if (!columnHeaderType.isPrimary()) {
                continue;
            }
            return columnHeaderType.getValue();
        }
        return null;
    }

    /**
     * Helper method to lookup affected resource for given row of report entry.
     *
     * @return The resource or <code>null</code> if not found.
     */
    private Optional<SourceLocation> resolveRelatedResource(RowType rowType, String primaryColumn) {
        if (rowType == null || primaryColumn == null) {
            return Optional.empty();
        }
        for (ColumnType column : rowType.getColumn()) {
            String name = column.getName();
            if (name.equals(primaryColumn)) {
                ElementType languageElement = column.getElement();
                if (languageElement == null) {
                    return Optional.empty();
                }
                SourceType source = column.getSource();
                ResourceResolver resourceResolver = languageResourceResolvers.get(languageElement.getLanguage()
                    .toLowerCase(Locale.ENGLISH));
                if (resourceResolver == null) {
                    return Optional.empty();
                }
                String element = languageElement.getValue();
                InputPath resource = resourceResolver.resolve(sensorContext.fileSystem(), element, source.getName(), column.getValue());
                SourceLocation sourceLocation = SourceLocation.builder()
                    .resource(Optional.ofNullable(resource))
                    .lineNumber(Optional.ofNullable(source.getLine()))
                    .build();
                return Optional.of(sourceLocation);
            }
        }
        return Optional.empty();
    }

    private StringBuilder appendResult(RowType rowType, StringBuilder message) {
        if (rowType != null) {
            message.append(NEWLINE);
            for (ColumnType column : rowType.getColumn()) {
                String name = column.getName();
                String value = column.getValue();
                if (message.length() > 0) {
                    message.append(", ");
                }
                message.append(name);
                message.append('=');
                message.append(value);
                message.append(NEWLINE);
            }
        }
        return message;
    }

    /**
     * Resource, line number and rule key are already set.
     *
     * @param ruleDescription The jQAssistant rule description.
     * @return Message String.
     */
    protected abstract String getMessage(T ruleDescription);

}
