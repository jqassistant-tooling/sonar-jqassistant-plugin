package org.jqassistant.contrib.sonarqube.plugin.sensor;

import com.buschmais.jqassistant.core.report.schema.v1.*;
import org.jqassistant.contrib.sonarqube.plugin.language.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(JQAssistantSensor.class);

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
            createIssue(Optional.empty(), ruleType, ruleKey, null, null);
        } else {
            String primaryColumn = getPrimaryColumn(result);
            for (RowType rowType : result.getRows().getRow()) {
                Optional<SourceLocation> target = resolveRelatedResource(rowType, primaryColumn);
                createIssue(target, ruleType, ruleKey, primaryColumn, rowType);
            }
        }
    }

    private void createIssue(Optional<SourceLocation> target, T ruleType, RuleKey ruleKey, String primaryColumn, RowType rowType) {
        if (target.isPresent() || sensorContext.fileSystem().baseDir().equals(projectPath)) {
            NewIssue newIssue = sensorContext.newIssue();
            String message = "[" + ruleType.getId() + "] " + getMessage(ruleType.getDescription(), primaryColumn, rowType);
            LOGGER.info("Creating issue '{}'.", message);
            NewIssueLocation newIssueLocation = newIssue.newLocation().message(message);
            if (target.isPresent()) {
                SourceLocation sourceLocation = target.get();
                newIssueLocation.on(sourceLocation.resource);
                Integer lineNumber = sourceLocation.lineNumber;
                if (lineNumber != null) {
                    TextRange textRange = toTextRange((InputFile) sourceLocation.resource, lineNumber);
                    newIssueLocation.at(textRange);
                }
            } else {
                newIssueLocation.on(sensorContext.project());
            }
            if (ruleType.getSeverity() != null) {
                newIssue.overrideSeverity(mapSeverity2SonarQ(ruleType.getSeverity()));
            }
            newIssue.forRule(ruleKey).at(newIssueLocation);
            newIssue.save();
        }
    }

    private TextRange toTextRange(InputFile resourceResolved, Integer lineNumber) {

        return resourceResolved.newRange(lineNumber, 0, lineNumber, 0);
    }

    private Severity mapSeverity2SonarQ(SeverityType severity) {
        if (severity == null) {
            return null;
        }
        switch (severity.getLevel()) {
            case 0:
                return Severity.BLOCKER;
            case 1:
                return Severity.CRITICAL;
            case 2:
                return Severity.MAJOR;
            case 3:
                return Severity.MINOR;
            case 4:
                return Severity.INFO;
            default:
                return null;
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
            if (!name.equals(primaryColumn)) {
                continue;
            }
            ElementType languageElement = column.getElement();
            if (languageElement == null) {
                return Optional.empty();
            }
            SourceType source = column.getSource();
            final ResourceResolver resourceResolver = languageResourceResolvers.get(languageElement.getLanguage().toLowerCase(Locale.ENGLISH));
            if (resourceResolver == null) {
                return Optional.empty();
            }
            String element = languageElement.getValue();
            InputPath resource = resourceResolver.resolve(sensorContext.fileSystem(), element, source.getName(), column.getValue());
            if (resource == null) {
                return Optional.empty();
            }
            return Optional.of(new SourceLocation(resource, source.getLine()));
        }
        return Optional.empty();
    }

    /**
     * Resource, line number and rule key are already set.
     *
     * @param ruleDescription The jQAssistant rule description.
     * @param primaryColumn   The name of the primary colum, maybe <code>null</code>.
     * @param rowEntry        Maybe <code>null</code> for not applied concepts.
     * @return Message String.
     */
    protected abstract String getMessage(String ruleDescription, String primaryColumn, RowType rowEntry);

}
