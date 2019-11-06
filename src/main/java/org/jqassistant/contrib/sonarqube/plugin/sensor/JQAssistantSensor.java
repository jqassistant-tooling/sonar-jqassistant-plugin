package org.jqassistant.contrib.sonarqube.plugin.sensor;

import com.buschmais.jqassistant.core.report.schema.v1.*;
import com.buschmais.jqassistant.core.shared.xml.JAXBUnmarshaller;
import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.jqassistant.contrib.sonarqube.plugin.JQAssistantConfiguration;
import org.jqassistant.contrib.sonarqube.plugin.language.JavaResourceResolver;
import org.jqassistant.contrib.sonarqube.plugin.language.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scanner.fs.InputProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

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
        File projectPath = getProjectPath(context);
        LOGGER.info("Using project path '{}'.", projectPath);
        File reportFile = getReportFile(projectPath);
        if (reportFile.exists()) {
            LOGGER.info("Found jQAssistant report at '{}'.", reportFile.getAbsolutePath());
            JqassistantReport report = readReport(reportFile);
            if (report != null) {
                evaluate(context, projectPath, report.getGroupOrConceptOrConstraint());
            }
        } else {
            LOGGER.info("No report found at '{}', skipping.", reportFile.getPath());
        }
    }

    private void evaluate(SensorContext context, File projectPath, List<ReferencableRuleType> rules) {
        for (ReferencableRuleType rule : rules) {
            if (rule instanceof GroupType) {
                GroupType groupType = (GroupType) rule;
                LOGGER.info("Processing group '{}'", groupType.getId());
                evaluate(context, projectPath, groupType.getGroupOrConceptOrConstraint());
            }
            if (rule instanceof ExecutableRuleType) {
                ExecutableRuleType ruleType = (ExecutableRuleType) rule;
                if (StatusEnumType.FAILURE.equals(ruleType.getStatus())) {
                    createIssue(context, projectPath, ruleType);
                }
            }
        }
    }

    private void createIssue(SensorContext context, File projectPath, ExecutableRuleType ruleType) {
        JQAssistantRuleType jQAssistantRuleType = (ruleType instanceof ConceptType) ? JQAssistantRuleType.CONCEPT : JQAssistantRuleType.CONSTRAINT;
        Optional<RuleKey> ruleKey = ruleResolver.resolve(jQAssistantRuleType);
        if (ruleKey.isPresent()) {
            switch (jQAssistantRuleType) {
                case CONCEPT:
                    ConceptIssueHandler conceptHandler = new ConceptIssueHandler(context, languageResourceResolvers, projectPath);
                    conceptHandler.process((ConceptType) ruleType, ruleKey.get());
                    break;
                case CONSTRAINT:
                    ConstraintIssueHandler constraintHandler = new ConstraintIssueHandler(context, languageResourceResolvers, projectPath);
                    constraintHandler.process((ConstraintType) ruleType, ruleKey.get());
                    break;
                default:
                    LOGGER.warn("Rule type {} is not supported, skipping.", jQAssistantRuleType);
            }
        } else {
            LOGGER.warn("Cannot resolve rule key for id '{}', no issue will be created. Is the rule not activated?", ruleType.getId());
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

    private Map<String, String> getNamespaceMapping() {
        Map<String, String> namespaceMappings = new HashMap<>();
        namespaceMappings.put("http://www.buschmais.com/jqassistant/core/report/schema/v1.2", "http://www.buschmais.com/jqassistant/core/report/schema/v1.3");
        namespaceMappings.put("http://www.buschmais.com/jqassistant/core/report/schema/v1.0", "http://www.buschmais.com/jqassistant/core/report/schema/v1.3");
        return namespaceMappings;
    }


    private File getProjectPath(SensorContext context) {
        Optional<String> path = configuration.getProjectPath();
        if (path.isPresent()) {
            File file = new File(path.get());
            if (!file.isAbsolute()) {
                throw new IllegalArgumentException("The project path '" + path + "' must be absolute.");
            }
            return file;
        }
        InputProject project = context.project();
        if (project instanceof DefaultInputProject) {
            return ((DefaultInputProject) project).getBaseDir().toFile();
        }
        return context.fileSystem().baseDir();
    }

    /**
     * The path is relative or absolute.
     */
    private File getReportFile(File projectPath) {
        Optional<String> reportPath = configuration.getReportPath();
        if (reportPath.isPresent()) {
            return new File(projectPath, reportPath.get());
        }
        return new File(projectPath, JQAssistant.SETTINGS_VALUE_DEFAULT_REPORT_PATH);
    }

}
