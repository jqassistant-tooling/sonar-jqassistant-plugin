package org.jqassistant.contrib.sonarqube.plugin.sensor;

import java.util.Locale;
import java.util.Map;

import org.jqassistant.contrib.sonarqube.plugin.language.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import com.buschmais.jqassistant.core.report.schema.v1.ColumnHeaderType;
import com.buschmais.jqassistant.core.report.schema.v1.ColumnType;
import com.buschmais.jqassistant.core.report.schema.v1.ColumnsHeaderType;
import com.buschmais.jqassistant.core.report.schema.v1.ElementType;
import com.buschmais.jqassistant.core.report.schema.v1.ExecutableRuleType;
import com.buschmais.jqassistant.core.report.schema.v1.ResultType;
import com.buschmais.jqassistant.core.report.schema.v1.RowType;
import com.buschmais.jqassistant.core.report.schema.v1.SeverityType;
import com.buschmais.jqassistant.core.report.schema.v1.SourceType;

/**
 * Base class to produce a number of violations defined by instance of {@link T}.
 * @author rzozmann
 *
 */
abstract class AbstractIssueHandler<T extends ExecutableRuleType> {

	protected static final Logger LOGGER = LoggerFactory.getLogger(JQAssistantSensor.class);

	private final Map<String, ResourceResolver> languageResourceResolvers;
	private SensorContext context = null;
	private final InputDir baseDir;

	protected AbstractIssueHandler(InputDir baseDir, Map<String, ResourceResolver> languageResourceResolvers) {
		this.languageResourceResolvers = languageResourceResolvers;
		this.baseDir = baseDir;
	}

	/**
	 * Create 0..n violations, based on content and type of <i>ruleType</i>.
	 */
	public final void process(SensorContext sensorContext, T ruleType, RuleKey ruleKey)
	{
		this.context = sensorContext;
		ResultType result = ruleType.getResult();
		//'result' may be null for a) not applied (failed) concepts and b) successful constraints
		if(result == null)
		{
			final SourceLocation target = resolveRelatedResource(null, null);
			if(target == null || target.resource == null) {
				return;
			}
			//allow issue creation on project level
			handleIssueBuilding(target.resource, target.lineNumber, ruleType, ruleKey, null, null);
			return;
		}

		String primaryColumn = getPrimaryColumn(result);
		for (RowType rowType : result.getRows().getRow()) {
			final SourceLocation target = resolveRelatedResource(rowType, primaryColumn);
			//report only violations for matching resources to avoid duplicated violations from other sub modules in same report
			if(target == null || target.resource == null) {
				continue;
			}
			handleIssueBuilding(target.resource, target.lineNumber, ruleType, ruleKey, primaryColumn, rowType);
		}
	}

	private void handleIssueBuilding(InputPath resourceResolved, Integer lineNumber, T ruleType, RuleKey ruleKey, String primaryColumn, RowType rowType)
	{
		NewIssue newIssue = context.newIssue();

		String message = getMessage(ruleType.getId(), ruleType.getDescription(), primaryColumn, rowType);
		if(message == null) {
			LOGGER.trace("Issue creation suppressed for {} on row {}", ruleType.getId(), rowType);
			return;
		}
		NewIssueLocation newIssueLocation = newIssue.newLocation().message(message).on(resourceResolved);
		if(lineNumber != null){
			TextRange textRange = toTextRange((InputFile) resourceResolved, lineNumber);
			newIssueLocation.at(textRange);
		}
		if (ruleType.getSeverity() != null) {
			newIssue.overrideSeverity(mapSeverity2SonarQ(ruleType.getSeverity()));
		}

		newIssue.forRule(ruleKey).at(newIssueLocation);

		newIssue.save();
	}

	private TextRange toTextRange(InputFile resourceResolved, Integer lineNumber) {

		return resourceResolved.newRange(lineNumber, 0, lineNumber, 0);
	}

	private Severity mapSeverity2SonarQ(SeverityType severity)
	{
		if(severity == null) {
			return null;
		}
		switch(severity.getLevel())
		{
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
	 * @param result
	 *            The result.
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
	 * @return The resource or <code>null</code> if not found.
	 */
	private final SourceLocation resolveRelatedResource(RowType rowType, String primaryColumn) {
		if(rowType == null || primaryColumn == null) {
			return determineAlternativeResource(rowType);
		}
		for (ColumnType column : rowType.getColumn()) {
			String name = column.getName();
			if (!name.equals(primaryColumn)) {
				continue;
			}
			ElementType languageElement = column.getElement();
			if (languageElement == null) {
				return determineAlternativeResource(rowType);
			}
			SourceType source = column.getSource();
			final ResourceResolver resourceResolver = languageResourceResolvers.get(languageElement.getLanguage().toLowerCase(Locale.ENGLISH));
			if (resourceResolver == null) {
				return determineAlternativeResource(rowType);
			}
			String element = languageElement.getValue();
			InputPath resource = resourceResolver.resolve(element, source.getName(), column.getValue());
			if(resource == null) {
				return determineAlternativeResource(rowType);
			}
			return new SourceLocation(resource, source.getLine());
		}
		return determineAlternativeResource(rowType);
	}

	@SuppressWarnings("javadoc")
	public InputDir getBaseDir() {

		return baseDir;
	}

	/**
	 * Client hook method to determine a resource for issue if default strategy fails.
	 * @param rowType The jQAssistant entry for row.<br/>
	 * The default implementation of this method does <code>return null</code>.
	 *
	 * @return The alternative location or <code>null</code>.
	 */
	protected SourceLocation determineAlternativeResource(RowType rowType)
	{
		return null;
	}

	/**
	 * Resource, line number and rule key are already set.
	 * @param ruleId The jQAssistant rule id.
	 * @param ruleDescription The jQAssistant rule description.
	 * @param primaryColumn The name of the primary colum, maybe <code>null</code>.
	 * @param rowEntry Maybe <code>null</code> for not applied concepts.
	 * @return Message String.
	 */
	protected abstract String getMessage(String ruleId, String ruleDescription, String primaryColumn, RowType rowEntry);

}
