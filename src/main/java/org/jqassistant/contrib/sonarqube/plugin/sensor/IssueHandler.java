package org.jqassistant.contrib.sonarqube.plugin.sensor;

import com.buschmais.jqassistant.core.report.schema.v1.*;
import org.jqassistant.contrib.sonarqube.plugin.language.JavaResourceResolver;
import org.jqassistant.contrib.sonarqube.plugin.language.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scanner.ScannerSide;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.jqassistant.contrib.sonarqube.plugin.sensor.RuleType.CONCEPT;
import static org.jqassistant.contrib.sonarqube.plugin.sensor.RuleType.CONSTRAINT;

/**
 * Base class to create issues.
 *
 * @author rzozmann
 */
@ScannerSide
public class IssueHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssueHandler.class);

    private static final String NEWLINE = "\n";

    private final Map<String, ResourceResolver> languageResourceResolvers;

    private final RuleKeyResolver ruleResolver;

    public IssueHandler(JavaResourceResolver resourceResolver, RuleKeyResolver ruleResolver) {
        this.ruleResolver = ruleResolver;
        this.languageResourceResolvers = new HashMap<>();
        this.languageResourceResolvers.put(resourceResolver.getLanguage().toLowerCase(Locale.ENGLISH), resourceResolver);
    }

    /**
     * Create issues, based on content.
     */
    void process(SensorContext sensorContext, File projectPath, ExecutableRuleType executableRuleType) {
        ResultType result = executableRuleType.getResult();
        if (result == null) {
            //'result' may be null for not applied (failed) concepts
            createIssue(sensorContext, projectPath, Optional.empty(), executableRuleType, null, null);
        } else {
            String primaryColumn = getPrimaryColumn(result);
            for (RowType rowType : result.getRows()
                .getRow()) {
                Optional<SourceLocation> target = resolveSourceLocation(sensorContext, rowType, primaryColumn);
                createIssue(sensorContext, projectPath, target, executableRuleType, rowType, primaryColumn);
            }
        }
    }

    private RuleType getRuleType(ExecutableRuleType executableRuleType) {
        if (executableRuleType instanceof ConceptType) {
            return CONCEPT;
        } else if (executableRuleType instanceof ConstraintType) {
            return CONSTRAINT;
        } else {
            throw new IllegalArgumentException("Rule type not supported; " + executableRuleType.getClass());
        }
    }

    private void createIssue(SensorContext sensorContext, File projectPath, Optional<SourceLocation> target, ExecutableRuleType executableRuleType, RowType rowType, String primaryColumn) {
        if (target.isPresent()) {
            SourceLocation sourceLocation = target.get();
            Optional<InputComponent> inputComponent = sourceLocation.getResource();
            if (inputComponent.isPresent()) {
                // Create an issue if a SourceLocation exists and InputComponent could be resolved (e.g. a class in a module)
                createIssue(sensorContext, executableRuleType, rowType, inputComponent.get(), sourceLocation.getLineNumber(), Optional.of(primaryColumn));
            }
        } else if (sensorContext.fileSystem()
            .baseDir()
            .equals(projectPath)) {
            // Create issue on project level for all items that cannot be mapped to a SourceLocation (e.g. packages or empty concepts)
            createIssue(sensorContext, executableRuleType, rowType, sensorContext.project(), Optional.empty(), Optional.empty());
        }
    }

    private void createIssue(SensorContext sensorContext, ExecutableRuleType executableRuleType, RowType rowType, InputComponent inputComponent,
                             Optional<Integer> lineNumber, Optional<String> matchedColumn) {
        RuleType ruleType = getRuleType(executableRuleType);
        Optional<RuleKey> ruleKey = ruleResolver.resolve(ruleType);
        if (ruleKey.isPresent()) {
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
                TextRange textRange = ((InputFile) inputComponent).selectLine(lineNumber.get());
                newIssueLocation.at(textRange);
            }
            severity.ifPresent(newIssue::overrideSeverity);
            newIssue.forRule(ruleKey.get())
                .at(newIssueLocation);
            newIssue.save();
        } else {
            LOGGER.warn("Cannot resolve rule key for id '{}', no issue will be created. Is the rule not activated?", executableRuleType.getId());
        }
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
    private Optional<SourceLocation> resolveSourceLocation(SensorContext sensorContext, RowType rowType, String primaryColumn) {
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
