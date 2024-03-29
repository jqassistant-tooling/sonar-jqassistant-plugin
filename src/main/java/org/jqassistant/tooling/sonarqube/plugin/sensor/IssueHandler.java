package org.jqassistant.tooling.sonarqube.plugin.sensor;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import org.jqassistant.tooling.sonarqube.plugin.JQAssistant;
import org.jqassistant.tooling.sonarqube.plugin.JQAssistantConfiguration;
import org.jqassistant.tooling.sonarqube.plugin.language.SourceFileResolver;
import org.jqassistant.schema.report.v2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.scanner.ScannerSide;

import static java.util.Optional.*;
import static java.util.stream.Collectors.joining;

/**
 * Base class to create issues.
 *
 * @author rzozmann
 */
@ScannerSide
@RequiredArgsConstructor
public class IssueHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssueHandler.class);

    private static final String NEWLINE = "\n";

    private final JQAssistantConfiguration configuration;

    private final SourceFileResolver sourceFileResolver;

    private final IssueKeyProvider issueKeyProvider;

    /**
     * Keeps the already processed rows from the jQAssistant report (to avoid duplicates).
     */
    private final Set<String> processedIssueKeys = new HashSet<>();

    /**
     * Create issues, based on content.
     */
    void process(SensorContext sensorContext, File reportModulePath, ExecutableRuleType executableRuleType) {
        ResultType result = executableRuleType.getResult();
        RuleType ruleType = getRuleType(executableRuleType);
        if (result == null) {
            // 'result' may be null for not applied (failed) concepts
            String issueKey = issueKeyProvider.getIssueKey(executableRuleType, ruleType);
            newIssue(sensorContext, executableRuleType, getRuleType(executableRuleType), issueKey, reportModulePath, empty(), "");
        } else {
            String primaryColumn = getPrimaryColumn(result);
            for (RowType rowType : result.getRows()
                .getRow()) {
                String rowKey = issueKeyProvider.getIssueKey(executableRuleType, rowType, ruleType);
                Optional<SourceLocation> target = resolveSourceLocation(sensorContext, rowType, primaryColumn);
                String message = convertRow(rowType);
                newIssue(sensorContext, executableRuleType, getRuleType(executableRuleType), rowKey, reportModulePath, target, message);
            }
        }
    }

    private RuleType getRuleType(ExecutableRuleType executableRuleType) {
        if (executableRuleType instanceof ConceptType) {
            return RuleType.CONCEPT;
        } else if (executableRuleType instanceof ConstraintType) {
            return RuleType.CONSTRAINT;
        } else {
            throw new IllegalArgumentException("Rule type not supported; " + executableRuleType.getClass());
        }
    }

    private void newIssue(SensorContext sensorContext, ExecutableRuleType executableRuleType, RuleType ruleType, String issueKey, File reportModulePath,
        Optional<SourceLocation> target, String message) {
        if (target.isPresent()) {
            SourceLocation sourceLocation = target.get();
            InputFile inputFile = sourceLocation.getInputFile();
            // Create an external issue if a SourceLocation exists and InputComponent could
            // be resolved (e.g. a class in a module)
            Optional<Integer> startLine = sourceLocation.getStartLine();
            Optional<Integer> endLine = sourceLocation.getEndLine();
            newIssue(sensorContext, executableRuleType, ruleType, issueKey, message, inputFile,
                newIssueLocation -> selectText(newIssueLocation, inputFile, startLine, endLine));
        } else if (sensorContext.fileSystem()
            .baseDir()
            .equals(reportModulePath)) {
            // Create issue on project level for all items that cannot be mapped to a
            // SourceLocation (e.g. packages or empty concepts)
            newIssue(sensorContext, executableRuleType, ruleType, issueKey, message, sensorContext.project(), newIssueLocation -> {
            });
        }
    }

    private void newIssue(SensorContext sensorContext, ExecutableRuleType executableRuleType, RuleType ruleType, String issueKey, String issueMessage,
        InputComponent inputComponent, Consumer<NewIssueLocation> locationConsumer) {
        if (processedIssueKeys.add(issueKey)) {
            org.sonar.api.rules.RuleType issueType = configuration.getIssueType();
            sensorContext.newAdHocRule()
                .engineId(JQAssistant.NAME)
                .ruleId(executableRuleType.getId())
                .name(executableRuleType.getId())
                .description(executableRuleType.getDescription())
                .type(issueType)
                .severity(ruleType.getDefaultSeverity())
                .save();
            NewExternalIssue newExternalIssue = sensorContext.newExternalIssue()
                .type(issueType);
            NewIssueLocation newIssueLocation = newExternalIssue.newLocation()
                .message(new StringBuilder(executableRuleType.getDescription()).append(NEWLINE)
                    .append(issueMessage)
                    .toString())
                .on(inputComponent);
            locationConsumer.accept(newIssueLocation);
            convertSeverity(executableRuleType.getSeverity()).ifPresent(newExternalIssue::severity);
            newExternalIssue.engineId(JQAssistant.NAME)
                .ruleId(executableRuleType.getId())
                .at(newIssueLocation)
                .save();
        }
    }

    private void selectText(NewIssueLocation newIssueLocation, InputFile inputFile, Optional<Integer> startLine, Optional<Integer> endLine) {
        if (startLine.isPresent()) {
            TextRange textRange;
            if (endLine.isPresent() && inputFile instanceof DefaultInputFile) {
                DefaultInputFile defaultInputFile = (DefaultInputFile) inputFile;
                textRange = defaultInputFile.newRange(startLine.get(), 0, endLine.get(), defaultInputFile.lineLength(endLine.get()));
            } else {
                textRange = inputFile.selectLine(startLine.get());
            }
            newIssueLocation.at(textRange);
        }
    }

    private Optional<Severity> convertSeverity(SeverityType severity) {
        if (severity == null) {
            return empty();
        }
        switch (severity.getLevel()) {
        case 0:
            return of(Severity.BLOCKER);
        case 1:
            return of(Severity.CRITICAL);
        case 2:
            return of(Severity.MAJOR);
        case 3:
            return of(Severity.MINOR);
        case 4:
            return of(Severity.INFO);
        default:
            return empty();
        }
    }

    /**
     * Determine the primary column from the result, i.e. the column which contains
     * the resource to create an issue for.
     *
     * @param result
     *     The result.
     * @return The name of the primary column or <code>null</code>.
     */
    private String getPrimaryColumn(ResultType result) {
        if (result == null) {
            return null;
        }
        // use primary attribute from columns element
        return result.getColumns()
            .getPrimary();
    }

    /**
     * Helper method to lookup affected resource for given row of report entry.
     *
     * @return The resource or <code>null</code> if not found.
     */
    private Optional<SourceLocation> resolveSourceLocation(SensorContext sensorContext, RowType rowType, String primaryColumn) {
        if (rowType == null || primaryColumn == null) {
            return empty();
        }
        for (ColumnType column : rowType.getColumn()) {
            String name = column.getName();
            SourceLocationType source = column.getSource();
            if (source != null && name.equals(primaryColumn)) {
                String sourceFileName = source.getFileName();
                if (sourceFileName == null) {
                    LOGGER.warn("Cannot determine source location, please upgrade jQAssistant to 1.11 or newer.");
                } else {
                    boolean isRelative = source.getParent() != null;
                    Optional<InputFile> inputFile = sourceFileResolver.resolve(sensorContext.fileSystem(), sourceFileName, isRelative);
                    return inputFile.map(file -> SourceLocation.builder()
                        .inputFile(file)
                        .startLine(ofNullable(source.getStartLine()))
                        .endLine(ofNullable(source.getEndLine()))
                        .build());
                }
            }
        }
        return empty();
    }

    /**
     * Convert a result row to a comma separated string.
     *
     * @param rowType
     *     The {@link RowType} containing values.
     * @return The result as String representation.
     */
    private String convertRow(RowType rowType) {
        return rowType.getColumn()
            .stream()
            .map(columnType -> columnType.getName() + ":" + columnType.getValue())
            .collect(joining("," + NEWLINE));
    }
}
