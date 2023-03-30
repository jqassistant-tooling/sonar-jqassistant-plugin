package org.jqassistant.contrib.sonarqube.plugin.sensor;

import java.io.File;
import java.util.List;
import java.util.Optional;

import com.buschmais.jqassistant.core.report.api.ReportReader;

import lombok.RequiredArgsConstructor;
import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.jqassistant.contrib.sonarqube.plugin.JQAssistantConfiguration;
import org.jqassistant.schema.report.v2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.scanner.fs.InputProject;

import static org.jqassistant.schema.report.v2.StatusEnumType.FAILURE;
import static org.jqassistant.schema.report.v2.StatusEnumType.WARNING;

/**
 * {@link Sensor} implementation scanning for jqassistant-report.xml files.
 */
@ScannerSide
@RequiredArgsConstructor
public class JQAssistantSensor implements Sensor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JQAssistantSensor.class);

    private final JQAssistantConfiguration configuration;
    private final IssueHandler issueHandler;

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name("JQA");
    }

    @Override
    public void execute(SensorContext context) {
        if (!configuration.isSensorDisabled()) {
            startScan(context);
        } else {
            LOGGER.info("{} is disabled", JQAssistant.NAME);
        }
    }

    private void startScan(SensorContext context) {
        String reportPath = configuration.getReportFile();
        File projectDir = getProjectDirectory(context);
        File baseDir = context.fileSystem()
            .baseDir();
        Optional<ReportLocator.ReportLocation> optionalReportLocation = ReportLocator.resolveReportFile(projectDir, baseDir, reportPath);
        if (optionalReportLocation.isPresent()) {
            ReportLocator.ReportLocation reportLocation = optionalReportLocation.get();
            File moduleDirectory = reportLocation.getModuleDirectory();
            File reportFile = reportLocation.getReportFile();
            LOGGER.info("Using jQAssistant report at '{}' for module '{}' .", reportFile.getPath(), moduleDirectory.getPath());
            ReportReader reportReader = new ReportReader(this.getClass()
                .getClassLoader());
            JqassistantReport report = reportReader.read(reportFile);
            if (report != null) {
                evaluate(context, moduleDirectory, report.getGroupOrConceptOrConstraint());
            }
        } else {
            LOGGER.info("No jQAssistant report found, skipping.");
        }
    }

    private File getProjectDirectory(SensorContext context) {
        InputProject project = context.project();
        if (project instanceof DefaultInputProject) {
            return ((DefaultInputProject) project).getBaseDir()
                .toFile();
        }
        return context.fileSystem()
            .baseDir();
    }

    private void evaluate(SensorContext context, File reportModulePath, List<ReferencableRuleType> rules) {
        for (ReferencableRuleType rule : rules) {
            if (rule instanceof GroupType) {
                GroupType groupType = (GroupType) rule;
                LOGGER.info("Processing group '{}'", groupType.getId());
                evaluate(context, reportModulePath, groupType.getGroupOrConceptOrConstraint());
            }
            if (rule instanceof ExecutableRuleType) {
                ExecutableRuleType executableRuleType = (ExecutableRuleType) rule;
                StatusEnumType ruleStatus = executableRuleType.getStatus();
                if (FAILURE.equals(ruleStatus) || WARNING.equals(ruleStatus)) {
                    issueHandler.process(context, reportModulePath, executableRuleType);
                }
            }
        }
    }
}
