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
import java.io.InputStream;
import java.util.*;

import static org.jqassistant.contrib.sonarqube.plugin.sensor.JQAssistantRuleType.CONCEPT;
import static org.jqassistant.contrib.sonarqube.plugin.sensor.JQAssistantRuleType.CONSTRAINT;

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
        File projectDir = getProjectDirectory(context);
        File baseDir = context.fileSystem().baseDir();
        File reportFile = configuration.getReportFile(projectDir, baseDir);
        if (reportFile.exists()) {
            LOGGER.info("Found jQAssistant report at '{}'.", reportFile.getAbsolutePath());
            JqassistantReport report = readReport(reportFile);
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
                ExecutableRuleType ruleType = (ExecutableRuleType) rule;
                if (StatusEnumType.FAILURE.equals(ruleType.getStatus())) {
                    createIssue(context, projectPath, ruleType);
                }
            }
        }
    }

    private void createIssue(SensorContext context, File projectPath, ExecutableRuleType executableRuleType) {
        JQAssistantRuleType ruleType = getRuleType(executableRuleType);
        Optional<RuleKey> ruleKey = ruleResolver.resolve(ruleType);
        if (ruleKey.isPresent()) {
            IssueHandler issueHandler = new IssueHandler(context, languageResourceResolvers, projectPath);
            issueHandler.process(ruleType, executableRuleType, ruleKey.get());
        } else {
            LOGGER.warn("Cannot resolve rule key for id '{}', no issue will be created. Is the rule not activated?", executableRuleType.getId());
        }
    }

    private JQAssistantRuleType getRuleType(ExecutableRuleType executableRuleType) {
        if (executableRuleType instanceof ConceptType) {
            return CONCEPT;
        } else if (executableRuleType instanceof ConstraintType) {
            return CONSTRAINT;
        } else {
            throw new IllegalArgumentException("Rule type not supported; " + executableRuleType.getClass());
        }
    }


    private JqassistantReport readReport(File reportFile) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = new FileInputStream(reportFile)) {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            JAXBUnmarshaller<JqassistantReport> jaxbUnmarshaller = new JAXBUnmarshaller<>(JqassistantReport.class, getNamespaceMapping());
            return jaxbUnmarshaller.unmarshal(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read jQAssistant report from file " + reportFile, e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private Map<String, String> getNamespaceMapping() {
        Map<String, String> namespaceMappings = new HashMap<>();
        namespaceMappings.put("http://www.buschmais.com/jqassistant/core/report/schema/v1.2", "http://www.buschmais.com/jqassistant/core/report/schema/v1.3");
        namespaceMappings.put("http://www.buschmais.com/jqassistant/core/report/schema/v1.0", "http://www.buschmais.com/jqassistant/core/report/schema/v1.3");
        return namespaceMappings;
    }


}
