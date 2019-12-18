package org.jqassistant.contrib.sonarqube.plugin.sensor;

import com.buschmais.jqassistant.core.report.schema.v1.ExecutableRuleType;
import com.buschmais.jqassistant.core.report.schema.v1.GroupType;
import com.buschmais.jqassistant.core.report.schema.v1.JqassistantReport;
import com.buschmais.jqassistant.core.report.schema.v1.ReferencableRuleType;
import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.jqassistant.contrib.sonarqube.plugin.JQAssistantConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.scanner.fs.InputProject;

import java.io.File;
import java.util.List;

import static com.buschmais.jqassistant.core.report.schema.v1.StatusEnumType.FAILURE;

/**
 * {@link Sensor} implementation scanning for jqassistant-report.xml files.
 */
public class JQAssistantSensor implements Sensor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JQAssistantSensor.class);

    private final JQAssistantConfiguration configuration;
    private final IssueHandler issueHandler;

    public JQAssistantSensor(JQAssistantConfiguration configuration, IssueHandler issueHandler) {
        this.configuration = configuration;
        this.issueHandler = issueHandler;

    }

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor
            .name("JQA");
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
        File projectDir = getProjectDirectory(context);
        File baseDir = context.fileSystem().baseDir();
        File reportFile = configuration.getReportFile(projectDir, baseDir);
        if (reportFile.exists()) {
            LOGGER.info("Found jQAssistant report at '{}'.", reportFile.getAbsolutePath());
            JqassistantReport report = ReportReader.getInstance().read(reportFile);
            if (report != null) {
                evaluate(context, projectDir, report.getGroupOrConceptOrConstraint());
            }
        } else {
            LOGGER.info("No report found at '{}', skipping.", reportFile.getPath());
        }
    }

    private File getProjectDirectory(SensorContext context) {
        InputProject project = context.project();
        if (project instanceof DefaultInputProject) {
            return ((DefaultInputProject) project).getBaseDir().toFile();
        }
        return context.fileSystem().baseDir();
    }

    private void evaluate(SensorContext context, File projectPath, List<ReferencableRuleType> rules) {
        for (ReferencableRuleType rule : rules) {
            if (rule instanceof GroupType) {
                GroupType groupType = (GroupType) rule;
                LOGGER.info("Processing group '{}'", groupType.getId());
                evaluate(context, projectPath, groupType.getGroupOrConceptOrConstraint());
            }
            if (rule instanceof ExecutableRuleType) {
                ExecutableRuleType executableRuleType = (ExecutableRuleType) rule;
                if (FAILURE.equals(executableRuleType.getStatus())) {
                    issueHandler.process(context, projectPath, executableRuleType);
                }
            }
        }
    }
}
