package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.jqassistant.contrib.sonarqube.plugin.JQAssistantConfiguration;
import org.jqassistant.contrib.sonarqube.plugin.language.SourceFileResolver;
import org.jqassistant.schema.report.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scanner.ScannerSide;

import java.io.File;
import java.util.Locale;
import java.util.Optional;

import static java.util.Optional.*;
import static java.util.stream.Collectors.joining;
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

    private final SourceFileResolver sourceFileResolver;

    private final JQAssistantConfiguration configuration;

    private final RuleKeyResolver ruleResolver;

    public IssueHandler(JQAssistantConfiguration configuration, SourceFileResolver sourceFileResolver, RuleKeyResolver ruleResolver) {
        this.configuration = configuration;
        this.ruleResolver = ruleResolver;
        this.sourceFileResolver = sourceFileResolver;
    }

    /**
     * Create issues, based on content.
     */
    void process(SensorContext sensorContext, File reportModulePath, ExecutableRuleType executableRuleType) {
        ResultType result = executableRuleType.getResult();
        if (result == null) {
            // 'result' may be null for not applied (failed) concepts
            newIssue(sensorContext, reportModulePath, empty(), executableRuleType, null, null);
        } else {
            String primaryColumn = getPrimaryColumn(result);
            for (RowType rowType : result.getRows().getRow()) {
                Optional<SourceLocation> target = resolveSourceLocation(sensorContext, rowType, primaryColumn);
                newIssue(sensorContext, reportModulePath, target, executableRuleType, rowType, primaryColumn);
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

    private void newIssue(SensorContext sensorContext, File reportModulePath, Optional<SourceLocation> target, ExecutableRuleType executableRuleType,
        RowType rowType, String primaryColumn) {
        if (target.isPresent()) {
            SourceLocation sourceLocation = target.get();
            Optional<InputFile> inputFile = sourceLocation.getInputFile();
            if (inputFile.isPresent()) {
                // Create an external issue if a SourceLocation exists and InputComponent could
                // be resolved (e.g. a class in a module)
                newExternalIssue(sensorContext, executableRuleType, rowType, inputFile.get(), sourceLocation.getStartLine(), sourceLocation.getEndLine(),
                    of(primaryColumn));
            }
        } else if (sensorContext.fileSystem().baseDir().equals(reportModulePath)) {
            // Create issue on project level for all items that cannot be mapped to a
            // SourceLocation (e.g. packages or empty concepts)
            newIssue(sensorContext, executableRuleType, rowType, sensorContext.project());
        }
    }

    private void newExternalIssue(SensorContext sensorContext, ExecutableRuleType executableRuleType, RowType rowType, InputFile inputFile,
        Optional<Integer> startLine, Optional<Integer> endLine, Optional<String> matchedColumn) {
        RuleType ruleType = getRuleType(executableRuleType);
        org.sonar.api.rules.RuleType issueType = configuration.getIssueType();

        sensorContext.newAdHocRule().engineId(JQAssistant.NAME).ruleId(executableRuleType.getId()).name(executableRuleType.getId())
            .description(executableRuleType.getDescription()).type(issueType).severity(ruleType.getDefaultSeverity()).save();

        NewExternalIssue newExternalIssue = sensorContext.newExternalIssue().type(issueType);
        StringBuilder message = new StringBuilder(executableRuleType.getDescription()).append(NEWLINE);
        message.append(convertRow(rowType, matchedColumn));
        NewIssueLocation newIssueLocation = newExternalIssue.newLocation().message(message.toString()).on(inputFile);
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
        convertSeverity(executableRuleType.getSeverity()).ifPresent(newExternalIssue::severity);
        newExternalIssue.engineId(JQAssistant.NAME).ruleId(executableRuleType.getId()).at(newIssueLocation).save();
    }

    private void newIssue(SensorContext sensorContext, ExecutableRuleType executableRuleType, RowType rowType, InputComponent inputComponent) {
        RuleType ruleType = getRuleType(executableRuleType);
        Optional<RuleKey> ruleKey = ruleResolver.resolve(ruleType);
        if (!ruleKey.isPresent()) {
            LOGGER.warn("Rule with id '{}' is not active, no issue will be created.", executableRuleType.getId());
        } else {
            StringBuilder message = new StringBuilder().append('[').append(executableRuleType.getId()).append("]").append(" ")
                .append(createMessage(ruleType, executableRuleType)).append(NEWLINE);
            message.append(convertRow(rowType, empty()));
            NewIssue newIssue = sensorContext.newIssue();
            NewIssueLocation newIssueLocation = newIssue.newLocation().message(message.toString()).on(inputComponent);
            convertSeverity(executableRuleType.getSeverity()).ifPresent(newIssue::overrideSeverity);
            newIssue.forRule(ruleKey.get()).at(newIssueLocation).save();
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
            return empty();
        }
        for (ColumnType column : rowType.getColumn()) {
            String name = column.getName();
            SourceLocationType source = column.getSource();
            if (source != null && name.equals(primaryColumn)) {
                String sourceFileName = source.getFileName();
                if (sourceFileName != null) {
                    InputFile inputFile = sourceFileResolver.resolve(sensorContext.fileSystem(), sourceFileName);
                    SourceLocation sourceLocation = SourceLocation.builder().inputFile(ofNullable(inputFile)).startLine(ofNullable(source.getStartLine()))
                        .endLine(ofNullable(source.getEndLine())).build();
                    return of(sourceLocation);
                } else {
                    // Fallback for pre 1.11 reports without legacy source location.
                    String javaSourceFileName = getJavaSourceFileName(source.getName());
                    InputFile inputFile = sourceFileResolver.resolve(sensorContext.fileSystem(), javaSourceFileName);
                    SourceLocation sourceLocation = SourceLocation.builder().inputFile(ofNullable(inputFile)).startLine(ofNullable(source.getLine()))
                        .endLine(ofNullable(source.getLine())).build();
                    return of(sourceLocation);
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
     * @param matchedColumn
     *     The name of the column that could be matched to a source location.
     * @return The result as String representation.
     */
    private String convertRow(RowType rowType, Optional<String> matchedColumn) {
        if (rowType != null) {
            return rowType.getColumn().stream().filter(columnType -> !(matchedColumn.isPresent() && columnType.getName().equals(matchedColumn.get())))
                .map(columnType -> columnType.getName() + ":" + columnType.getValue()).collect(joining("," + NEWLINE));
        }
        return "";
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

    /**
     * Convert a given entry like
     * <code>com/buschmais/jqassistant/examples/sonar/project/Bar.class</code> into
     * a source file name like
     * <code>com/buschmais/jqassistant/examples/sonar/project/Bar.java</code>.
     *
     * @deprecated To be replaced by {@link SourceLocationType#getFileName()}.
     */
    @Deprecated
    private String getJavaSourceFileName(String classFileName) {
        if (classFileName == null || classFileName.isEmpty()) {
            return null;
        }
        String result = classFileName;
        if (result.toLowerCase(Locale.ENGLISH).endsWith(".class")) {
            result = result.substring(0, result.length() - ".class".length());
        }
        // remove nested class fragments
        int index = result.indexOf('$');
        if (index > -1) {
            result = result.substring(0, index);
        }
        return result.concat(".java");
    }

}
