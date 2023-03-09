package org.jqassistant.contrib.sonarqube.plugin.sensor;

import java.io.File;
import java.util.Optional;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.jqassistant.contrib.sonarqube.plugin.JQAssistantConfiguration;
import org.jqassistant.contrib.sonarqube.plugin.language.SourceFileResolver;
import org.jqassistant.schema.report.v1.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.scanner.fs.InputProject;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.sonar.api.batch.rule.Severity.MAJOR;
import static org.sonar.api.batch.rule.Severity.MINOR;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;

@ExtendWith(MockitoExtension.class)
class IssueHandlerTest {

    private static final File PROJECT_PATH = new File(".");

    @Mock
    private SensorContext sensorContext;

    @Mock
    private FileSystem fileSystem;

    @Mock
    private InputProject inputProject;

    @Mock
    private InputFile inputFile;

    @Mock
    private DefaultInputFile defaultInputFile;
    @Mock
    private NewAdHocRule newAdHocRule;

    @Mock
    private NewExternalIssue newExternalIssue;

    @Mock
    private NewIssueLocation newIssueLocation;

    @Mock
    private SourceFileResolver sourceFileResolver;

    @Mock
    private JQAssistantConfiguration configuration;

    private IssueHandler issueHandler;

    @BeforeEach
    public void setUp() {
        issueHandler = new IssueHandler(configuration, sourceFileResolver);
        doReturn(fileSystem).when(sensorContext)
            .fileSystem();
    }

    /**
     * Verifies that invalid concepts are reported on project level
     */
    @Test
    public void invalidConceptOnProjectLevel() {
        ConceptType conceptType = new ConceptType();
        conceptType.setDescription("TestConcept");
        conceptType.setId("test:Concept");
        doReturn(PROJECT_PATH).when(fileSystem)
            .baseDir();
        doReturn(inputProject).when(sensorContext)
            .project();
        stubExternalNewIssue(CODE_SMELL, empty());

        issueHandler.process(sensorContext, PROJECT_PATH, conceptType);

        verifyNewExternalIssue(CODE_SMELL, "test:Concept", "TestConcept", MINOR, empty());
    }

    /**
     * Verifies that invalid concepts are not reported on module level
     */
    @Test
    public void invalidConceptOnModuleLevel() {
        ConceptType conceptType = new ConceptType();
        conceptType.setDescription("TestConcept");
        conceptType.setId("test:Concept");
        doReturn(new File(PROJECT_PATH, "module")).when(fileSystem)
            .baseDir();

        issueHandler.process(sensorContext, PROJECT_PATH, conceptType);

        verify(sensorContext, never()).newExternalIssue();
    }

    /**
     * Verifies that violated constraints without a source location are reported on
     * project level
     */
    @Test
    public void constraintViolationWithoutSourceLocation() {
        ConstraintType constraintType = new ConstraintType();
        constraintType.setDescription("TestConstraint");
        constraintType.setId("test:Constraint");
        constraintType.setResult(createResultType(false));
        doReturn(PROJECT_PATH).when(fileSystem)
            .baseDir();
        doReturn(inputProject).when(sensorContext)
            .project();
        stubExternalNewIssue(CODE_SMELL, empty());

        issueHandler.process(sensorContext, PROJECT_PATH, constraintType);

        verifyNewExternalIssue(CODE_SMELL, "test:Constraint", "TestConstraint", MAJOR, empty());
        verify(newIssueLocation).on(inputProject);
    }

    @Test
    public void constraintViolationAsCodeSmellWithDefaultInputFileLocation() {
        stubDefaultInputFileSourceLocation();
        constraintViolationWithMatchingSourceLocation(CODE_SMELL);
        verify(newIssueLocation).on(defaultInputFile);
    }

    @Test
    public void constraintViolationAsCodeSmellWithInputFileLocation() {
        stubInputFileSourceLocation();
        constraintViolationWithMatchingSourceLocation(CODE_SMELL);
        verify(newIssueLocation).on(inputFile);
    }

    @Test
    public void constraintViolationAsBugWithDefaultInputFIleLocation() {
        stubDefaultInputFileSourceLocation();
        constraintViolationWithMatchingSourceLocation(BUG);
        verify(newIssueLocation).on(defaultInputFile);
    }

    /**
     * Verifies that violated constraints with a source location are reported on the
     * referenced element if it can be resolved.
     */
    private void constraintViolationWithMatchingSourceLocation(org.sonar.api.rules.RuleType ruleType) {
        ConstraintType constraintType = new ConstraintType();
        constraintType.setDescription("TestConstraint");
        constraintType.setId("test:Constraint");
        constraintType.setSeverity(getSeverityType(com.buschmais.jqassistant.core.rule.api.model.Severity.CRITICAL));
        constraintType.setResult(createResultType(true));
        stubExternalNewIssue(ruleType, of(Severity.CRITICAL));

        issueHandler.process(sensorContext, PROJECT_PATH, constraintType);

        verifyNewExternalIssue(ruleType, "test:Constraint", "TestConstraint", MAJOR, of(Severity.CRITICAL));
    }

    private void verifyNewExternalIssue(org.sonar.api.rules.RuleType ruleType, String ruleId, String description, Severity ruleSeverity,
        Optional<Severity> issueSeverity) {
        verify(sensorContext).newAdHocRule();
        verify(newAdHocRule).engineId(JQAssistant.NAME);
        verify(newAdHocRule).ruleId(ruleId);
        verify(newAdHocRule).description(description);
        verify(newAdHocRule).severity(ruleSeverity);

        verify(configuration).getIssueType();
        verify(sensorContext).newExternalIssue();
        verify(newExternalIssue).engineId(JQAssistant.NAME);
        verify(newExternalIssue).ruleId(ruleId);
        issueSeverity.ifPresent(severity -> verify(newExternalIssue).severity(severity));
        verify(newExternalIssue).type(ruleType);
        verify(newExternalIssue).newLocation();
    }

    private SeverityType getSeverityType(com.buschmais.jqassistant.core.rule.api.model.Severity severity) {
        SeverityType severityType = new SeverityType();
        severityType.setValue(com.buschmais.jqassistant.core.rule.api.model.Severity.CRITICAL.getValue());
        severityType.setLevel(severity.getLevel());
        return severityType;
    }

    private ResultType createResultType(boolean includeSourceLocation) {
        ResultType resultType = new ResultType();
        ColumnsHeaderType columnsHeaderType = new ColumnsHeaderType();
        columnsHeaderType.setCount(1);
        ColumnHeaderType columnHeaderType = new ColumnHeaderType();
        columnHeaderType.setValue("Value");
        columnHeaderType.setPrimary(true);
        columnsHeaderType.getColumn()
            .add(columnHeaderType);
        resultType.setColumns(columnsHeaderType);

        RowsType rowsType = new RowsType();
        RowType rowType = new RowType();
        ColumnType columnType = new ColumnType();
        columnType.setName("Value");
        columnType.setValue("Test");

        if (includeSourceLocation) {
            ElementType elementType = new ElementType();
            elementType.setLanguage("Java");
            elementType.setValue("WriteField");
            columnType.setElement(elementType);
            SourceLocationType sourceType = new SourceLocationType();
            sourceType.setFileName("com/buschmais/jqassistant/examples/sonar/project/Bar.java");
            sourceType.setStartLine(16);
            sourceType.setEndLine(18);
            columnType.setSource(sourceType);
        }

        rowType.getColumn()
            .add(columnType);
        rowsType.getRow()
            .add(rowType);
        resultType.setRows(rowsType);
        return resultType;
    }

    private void stubExternalNewIssue(org.sonar.api.rules.RuleType ruleType, Optional<Severity> issueSeverity) {
        doReturn(ruleType).when(configuration)
            .getIssueType();

        doReturn(newAdHocRule).when(sensorContext)
            .newAdHocRule();
        doReturn(newAdHocRule).when(newAdHocRule)
            .engineId(anyString());
        doReturn(newAdHocRule).when(newAdHocRule)
            .ruleId(anyString());
        doReturn(newAdHocRule).when(newAdHocRule)
            .name(anyString());
        doReturn(newAdHocRule).when(newAdHocRule)
            .description(anyString());
        doReturn(newAdHocRule).when(newAdHocRule)
            .type(any(org.sonar.api.rules.RuleType.class));
        doReturn(newAdHocRule).when(newAdHocRule)
            .severity(any(Severity.class));

        doReturn(newExternalIssue).when(sensorContext)
            .newExternalIssue();
        doReturn(newExternalIssue).when(newExternalIssue)
            .type(any(org.sonar.api.rules.RuleType.class));
        doReturn(newIssueLocation).when(newExternalIssue)
            .newLocation();
        doReturn(newExternalIssue).when(newExternalIssue)
            .engineId(anyString());
        doReturn(newExternalIssue).when(newExternalIssue)
            .ruleId(anyString());
        issueSeverity.ifPresent(severity -> doReturn(newExternalIssue).when(newExternalIssue)
            .severity(any(Severity.class)));
        doReturn(newExternalIssue).when(newExternalIssue)
            .at(newIssueLocation);
        stubNewIssueLocation();
    }

    private void stubNewIssueLocation() {
        doReturn(newIssueLocation).when(newIssueLocation)
            .message(any(String.class));
        doReturn(newIssueLocation).when(newIssueLocation)
            .on(any(InputComponent.class));
    }

    private void stubDefaultInputFileSourceLocation() {
        when(sourceFileResolver.resolve(any(FileSystem.class), any(String.class), anyBoolean())).thenReturn(of(defaultInputFile));
        doReturn(20).when(defaultInputFile)
            .lineLength(18);
        doReturn(mock(TextRange.class)).when(defaultInputFile)
            .newRange(16, 0, 18, 20);
    }

    private void stubInputFileSourceLocation() {
        when(sourceFileResolver.resolve(any(FileSystem.class), any(String.class), anyBoolean())).thenReturn(of(inputFile));
        doReturn(mock(TextRange.class)).when(inputFile)
            .selectLine(16);
    }
}
