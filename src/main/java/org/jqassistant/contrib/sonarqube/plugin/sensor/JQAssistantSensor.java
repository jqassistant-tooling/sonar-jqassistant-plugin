package org.jqassistant.contrib.sonarqube.plugin.sensor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.jqassistant.contrib.sonarqube.plugin.JQAssistantConfiguration;
import org.jqassistant.contrib.sonarqube.plugin.language.JavaResourceResolver;
import org.jqassistant.contrib.sonarqube.plugin.language.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
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
public class JQAssistantSensor implements Sensor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JQAssistantSensor.class);

    private JQAssistantConfiguration configuration;
    private final RuleKeyResolver ruleResolver;
    private Map<String, ResourceResolver> languageResourceResolvers;

    public JQAssistantSensor(JQAssistantConfiguration configuration, JavaResourceResolver resourceResolver, RuleKeyResolver ruleKeyResolver) {
        this.configuration = configuration;
        this.ruleResolver = ruleKeyResolver;
        this.languageResourceResolvers = new HashMap<>();
        this.languageResourceResolvers.put(resourceResolver.getLanguage().toLowerCase(Locale.ENGLISH), resourceResolver);

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

        File reportFile = findReportFile(context);
        if (reportFile != null) {
            LOGGER.debug("Using report found at '{}'.", reportFile.getAbsolutePath());
            JqassistantReport report = readReport(reportFile);
            if (report != null) {
                evaluate(context, report.getGroupOrConceptOrConstraint());
            }
        } else {
            LOGGER.info("No report found at {}, skipping.", determineConfiguredReportPath());
        }
    }

    private void evaluate(SensorContext context, List<ReferencableRuleType> rules) {
        for (ReferencableRuleType rule : rules) {
            if (rule instanceof GroupType) {
                GroupType groupType = (GroupType) rule;
                LOGGER.info("Processing group '{}'", groupType.getId());
                evaluate(context, groupType.getGroupOrConceptOrConstraint());
            }
            if (rule instanceof ExecutableRuleType) {
                ExecutableRuleType ruleType = (ExecutableRuleType) rule;
                if (StatusEnumType.FAILURE.equals(ruleType.getStatus())) {
                    createIssue(context, ruleType);
                }
            }
        }
    }

    private void createIssue(SensorContext context, ExecutableRuleType ruleType) {
        JQAssistantRuleType jQAssistantRuleType = (ruleType instanceof ConceptType) ? JQAssistantRuleType.Concept : JQAssistantRuleType.Constraint;
        final RuleKey ruleKey = ruleResolver.resolve(jQAssistantRuleType);
        if (ruleKey == null) {
            LOGGER.warn("Cannot resolve rule key for id '{}'. No issue will be created! Rule not active?", ruleType.getId());
        } else {
        	InputProject baseDir = context.project();
            switch (jQAssistantRuleType) {
                case Concept:
                    ConceptIssueHandler conceptHandler =
                        new ConceptIssueHandler(baseDir, languageResourceResolvers);
                    conceptHandler.process(context, (ConceptType) ruleType, ruleKey);
                    break;
                case Constraint:
                    ConstraintIssueHandler constraintHandler = new ConstraintIssueHandler(baseDir, languageResourceResolvers);
                    constraintHandler.process(context, (ConstraintType) ruleType, ruleKey);
                    break;
                default:
                    LOGGER.warn("Unsupported rule type {}", jQAssistantRuleType);
            }
        }
    }

    private JqassistantReport readReport(File reportFile) {
        try {
            JAXBUnmarshaller<JqassistantReport> jaxbUnmarshaller = new JAXBUnmarshaller<>(JqassistantReport.class, getNamespaceMapping());
            return jaxbUnmarshaller.unmarshal(new FileInputStream(reportFile));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read jQAssistant report from file " + reportFile, e);
        }
    }

    private Map<String, String> getNamespaceMapping(){
        Map<String, String> namespaceMappings = new HashMap<>();
        namespaceMappings.put("http://www.buschmais.com/jqassistant/core/report/schema/v1.2", "http://www.buschmais.com/jqassistant/core/report/schema/v1.3");
        namespaceMappings.put("http://www.buschmais.com/jqassistant/core/report/schema/v1.0", "http://www.buschmais.com/jqassistant/core/report/schema/v1.3");
        return namespaceMappings;
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
    private File findReportFile(SensorContext context) {
        String configReportPath = determineConfiguredReportPath();
        File baseDir = context.fileSystem().baseDir();
        File reportFile = new File(baseDir, configReportPath);
        if (reportFile.exists()) {
            return reportFile;
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
}
