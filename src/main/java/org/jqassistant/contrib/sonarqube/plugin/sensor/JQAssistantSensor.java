package org.jqassistant.contrib.sonarqube.plugin.sensor;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.jqassistant.contrib.sonarqube.plugin.JQAssistantConfiguration;
import org.jqassistant.contrib.sonarqube.plugin.language.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import com.buschmais.jqassistant.core.report.schema.v1.ConceptType;
import com.buschmais.jqassistant.core.report.schema.v1.ConstraintType;
import com.buschmais.jqassistant.core.report.schema.v1.ExecutableRuleType;
import com.buschmais.jqassistant.core.report.schema.v1.GroupType;
import com.buschmais.jqassistant.core.report.schema.v1.JqassistantReport;
import com.buschmais.jqassistant.core.report.schema.v1.ReferencableRuleType;
import com.buschmais.jqassistant.core.report.schema.v1.StatusEnumType;
import com.buschmais.jqassistant.core.shared.xml.JAXBUnmarshaller;

/**
 * {@link Sensor} implementation scanning for jqassistant-report.xml files.
 */
@Phase(name = Phase.Name.DEFAULT)
public class JQAssistantSensor implements Sensor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JQAssistantSensor.class);
    private static final String REPORT_NAMESPACE = "http://www.buschmais.com/jqassistant/core/report/schema/v1.3";

    // avoid multiple loading of report file (maybe a huge file!) while creation
    // of new instance of sensor for a project
    // TODO: This works only if we have a single report, also in multi project
    // environment
    private static String reportFilePath = null;

    private final FileSystem fileSystem;
    private final JAXBUnmarshaller<JqassistantReport> jaxbUnmarshaller;
    private final JQAssistantConfiguration configuration;
    private final RuleKeyResolver ruleResolver;

    private final ConceptIssueHandler conceptHandler;
    private final ConstraintIssueHandler constraintHandler;

    public JQAssistantSensor(JQAssistantConfiguration configuration, ResourcePerspectives perspectives, ComponentContainer componentContainerc,
            FileSystem moduleFileSystem) throws JAXBException {
        this.configuration = configuration;
        this.fileSystem = moduleFileSystem;
        Map<String, ResourceResolver> languageResourceResolvers = new HashMap<>();
        for (ResourceResolver resolver : componentContainerc.getComponentsByType(ResourceResolver.class)) {
            languageResourceResolvers.put(resolver.getLanguage().toLowerCase(Locale.ENGLISH), resolver);
        }
        ruleResolver = componentContainerc.getComponentByType(RuleKeyResolver.class);
        Map<String, String> namespaceMappings = new HashMap<>();
        namespaceMappings.put("http://www.buschmais.com/jqassistant/core/report/schema/v1.2", "http://www.buschmais.com/jqassistant/core/report/schema/v1.3");
        namespaceMappings.put("http://www.buschmais.com/jqassistant/core/report/schema/v1.0", REPORT_NAMESPACE);
        this.jaxbUnmarshaller = new JAXBUnmarshaller<>(JqassistantReport.class, namespaceMappings);
        this.conceptHandler = new ConceptIssueHandler(perspectives, languageResourceResolvers);
        this.constraintHandler = new ConstraintIssueHandler(perspectives, languageResourceResolvers);
    }

    public boolean shouldExecuteOnProject(Project project) {
        boolean disabled = configuration.isSensorDisabled();
        if (disabled) {
            LOGGER.info("{} is disabled on project {}", JQAssistant.NAME, project.getName());
        } else if (ruleResolver == null) {
            disabled = true;
        }
        return !disabled;
    }

    public void analyse(Project project, SensorContext sensorContext) {
        File reportFile = findReportFile(project);
        if (reportFile != null) {
            LOGGER.debug("Using report found at '{}'.", reportFile.getAbsolutePath());
            JqassistantReport report = readReport(reportFile);
            if (report != null) {
                evaluate(project, sensorContext, report.getGroupOrConceptOrConstraint());
            }
        } else {
            LOGGER.info("No report found at {} for project {}, skipping.", determineConfiguredReportPath(), project.getName());
        }
    }

    @Override
    public String toString() {
        return JQAssistant.NAME;
    }

    private JqassistantReport readReport(File reportFile) {
        if (reportFile.getAbsolutePath().equals(reportFilePath)) {
            return null;
        }
        try {
            JqassistantReport report = jaxbUnmarshaller.unmarshal(new FileInputStream(reportFile));
            reportFilePath = reportFile.getAbsolutePath();
            return report;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read jQAssistant report from file " + reportFile, e);
        }
    }

    private void evaluate(Project project, SensorContext sensorContext, List<ReferencableRuleType> rules) {
        for (ReferencableRuleType rule : rules) {
            if (rule instanceof GroupType) {
                GroupType groupType = (GroupType) rule;
                LOGGER.info("Processing group '{}'", groupType.getId());
                evaluate(project, sensorContext, groupType.getGroupOrConceptOrConstraint());
            }
            if (rule instanceof ExecutableRuleType) {
                ExecutableRuleType ruleType = (ExecutableRuleType) rule;
                if (StatusEnumType.FAILURE.equals(ruleType.getStatus())) {
                    createIssue(project, sensorContext, ruleType);
                }
            }
        }
    }

    private void createIssue(Project project, SensorContext sensorContext, ExecutableRuleType ruleType) {
        final String id = ruleType.getId();
        JQAssistantRuleType jQAssistantRuleType = (ruleType instanceof ConceptType) ? JQAssistantRuleType.Concept : JQAssistantRuleType.Constraint;
        final RuleKey ruleKey = ruleResolver.resolve(jQAssistantRuleType);
        if (ruleKey == null) {
            LOGGER.warn("Cannot resolve rule key for id '{}'. No issue will be created! Rule not active?", id);
        } else {
            switch (jQAssistantRuleType) {
            case Concept:
                sensorContext.newIssue().forRule(ruleKey).save();
                conceptHandler.process(project, sensorContext, (ConceptType) ruleType, ruleKey);
                break;
            case Constraint:
                constraintHandler.process(project, sensorContext, (ConstraintType) ruleType, ruleKey);
                break;
            default:
                LOGGER.warn("Unsupported rule type {}", jQAssistantRuleType);
            }
        }
    }

    /**
     * <ol>
     * <li>Look for report file in current project dir</li>
     * <li>if not found go to parent and look again (recursive up to root
     * project)</li>
     * </ol>
     * Return the report xml file or null if not found. Checks whether
     * {@link JQAssistant#SETTINGS_KEY_REPORT_PATH} is set or not and looks up
     * the passed path or the default build directory.
     *
     * @return reportFile File object of report xml or null if not found.
     */
    private File findReportFile(Project project) {
        if (project == null) {
            return null;
        }
        String configReportPath = determineConfiguredReportPath();
        File baseDir = fileSystem.baseDir();
        File reportFile = new File(baseDir, configReportPath);
        if (reportFile.exists()) {
            return reportFile;
        }
        if (project.isModule()) {
            return findReportFile(project.getParent());
        }
        return null;
    }

    /**
     * The path is relative or absolute.
     */
    private String determineConfiguredReportPath() {
        String configReportPath = configuration.getReportPath();
        if (configReportPath == null || configReportPath.isEmpty()) {
            configReportPath = JQAssistant.SETTINGS_VALUE_DEFAULT_REPORT_FILE_PATH;
        }
        return configReportPath;
    }

    @Override
    public void describe(SensorDescriptor descriptor) {

    }

    @Override
    public void execute(org.sonar.api.batch.sensor.SensorContext context) {

    }
}
