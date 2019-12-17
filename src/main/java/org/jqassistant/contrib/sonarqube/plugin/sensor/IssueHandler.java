package org.jqassistant.contrib.sonarqube.plugin.sensor;

import com.buschmais.jqassistant.core.report.schema.v1.*;
import lombok.extern.slf4j.Slf4j;
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
 * Base class to create issues.
 *
 * @author rzozmann
 */
@Slf4j
class IssueHandler {

    private static final String NEWLINE = "\n";
    private final SensorContext sensorContext;

    private final Map<String, ResourceResolver> languageResourceResolvers;

    private final File projectPath;

    IssueHandler(SensorContext sensorContext, Map<String, ResourceResolver> languageResourceResolvers, File projectPath) {
        this.sensorContext = sensorContext;
        this.languageResourceResolvers = languageResourceResolvers;
        this.projectPath = projectPath;
    }

    /**
     * Create 0..n violations, based on content and type of <i>ruleType</i>.
     */
    void process(RuleType ruleType, ExecutableRuleType executableRuleType, RuleKey ruleKey) {
        ResultType result = executableRuleType.getResult();
        if (result == null) {
            //'result' may be null for not applied (failed) concepts
            createIssue(Optional.empty(), ruleType, executableRuleType, ruleKey, null, null);
        } else {
            String primaryColumn = getPrimaryColumn(result);
            for (RowType rowType : result.getRows()
                .getRow()) {
                Optional<SourceLocation> target = resolveSourceLocation(rowType, primaryColumn);
                createIssue(target, ruleType, executableRuleType, ruleKey, rowType, primaryColumn);
            }
        }
    }

    private void createIssue(Optional<SourceLocation> target, RuleType ruleType, ExecutableRuleType executableRuleType, RuleKey ruleKey, RowType rowType, String primaryColumn) {
        if (target.isPresent()) {
            SourceLocation sourceLocation = target.get();
            Optional<InputComponent> inputComponent = sourceLocation.getResource();
            if (inputComponent.isPresent()) {
                // Create an issue if a SourceLocation exists and InputComponent could be resolved (e.g. a class in a module)
                createIssue(ruleType, executableRuleType, ruleKey, rowType, inputComponent.get(), sourceLocation.getLineNumber(), Optional.of(primaryColumn));
            }
        } else if (sensorContext.fileSystem()
            .baseDir()
            .equals(projectPath)) {
            // Create issue on project level for all items that cannot be mapped to a SourceLocation (e.g. packages or empty concepts)
            createIssue(ruleType, executableRuleType, ruleKey, rowType, sensorContext.project(), Optional.empty(), Optional.empty());
        }
    }

    private void createIssue(RuleType ruleType, ExecutableRuleType executableRuleType, RuleKey ruleKey, RowType rowType, InputComponent inputComponent,
                             Optional<Integer> lineNumber, Optional<String> matchedColumn) {
        StringBuilder message = new StringBuilder().append('[')
            .append(executableRuleType.getId())
            .append("]")
            .append(" ")
            .append(createMessage(ruleType, executableRuleType));
        appendResult(rowType, matchedColumn, message);
        Optional<Severity> severity = convertSeverity(executableRuleType.getSeverity());
        NewIssue newIssue = sensorContext.newIssue();
        NewIssueLocation newIssueLocation = newIssue.newLocation()
            .message(message.toString());
        newIssueLocation.on(inputComponent);
        if (lineNumber.isPresent()) {
            TextRange textRange = toTextRange((InputFile) inputComponent, lineNumber.get());
            newIssueLocation.at(textRange);
        }
        severity.ifPresent(newIssue::overrideSeverity);
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
    private Optional<SourceLocation> resolveSourceLocation(RowType rowType, String primaryColumn) {
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

    /**
     * Appends the result to a message.
     *
     * @param rowType       The {@link RowType} containing values.
     * @param matchedColumn The name of the column that could be matched to a source location.
     * @param message       The message builder.
     */
    private void appendResult(RowType rowType, Optional<String> matchedColumn, StringBuilder message) {
        if (rowType != null) {
            message.append(NEWLINE);
            int count = 0;
            for (ColumnType column : rowType.getColumn()) {
                if (count > 0) {
                    message.append(", ").append(NEWLINE);
                }
                String name = column.getName();
                if (!(matchedColumn.isPresent() && name.equals(matchedColumn.get()))) {
                    String value = column.getValue();
                    message.append(name);
                    message.append(':');
                    message.append(value);
                    message.append(NEWLINE);
                    count++;
                }
            }
        }
    }

    private String createMessage(RuleType ruleType, ExecutableRuleType executableRuleType) {
        switch (ruleType) {
            case CONCEPT:
                return "The concept could not be applied: " + executableRuleType.getDescription();
            case CONSTRAINT:
                return executableRuleType.getDescription();
            default:
                throw new IllegalArgumentException("Rule type not supported; " + executableRuleType.getClass());
        }
    }

}
