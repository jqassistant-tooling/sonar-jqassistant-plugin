package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.jqassistant.contrib.sonarqube.plugin.JQAssistant;
import org.jqassistant.contrib.sonarqube.plugin.language.JavaResourceResolver;
import org.jqassistant.schema.report.v1.ColumnHeaderType;
import org.jqassistant.schema.report.v1.ColumnType;
import org.jqassistant.schema.report.v1.ColumnsHeaderType;
import org.jqassistant.schema.report.v1.ConceptType;
import org.jqassistant.schema.report.v1.ConstraintType;
import org.jqassistant.schema.report.v1.ElementType;
import org.jqassistant.schema.report.v1.ResultType;
import org.jqassistant.schema.report.v1.RowType;
import org.jqassistant.schema.report.v1.RowsType;
import org.jqassistant.schema.report.v1.SeverityType;
import org.jqassistant.schema.report.v1.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.fs.internal.DefaultTextRange;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.scanner.fs.InputProject;

import java.io.File;
import java.util.Optional;

import static com.buschmais.jqassistant.core.rule.api.model.Severity.CRITICAL;
import static org.jqassistant.contrib.sonarqube.plugin.sensor.RuleType.CONCEPT;
import static org.jqassistant.contrib.sonarqube.plugin.sensor.RuleType.CONSTRAINT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class IssueHandlerTest {

    private static final Rule CONCEPT_RULE = Rule.create(JQAssistant.KEY, CONCEPT.getKey(), CONCEPT.getDescription());

    private static final Rule CONSTRAINT_RULE = Rule.create(JQAssistant.KEY, CONSTRAINT.getKey(), CONSTRAINT.getDescription());

    private static final File PROJECT_PATH = new File(".");

    @Mock
    private SensorContext sensorContext;

    @Mock
    private FileSystem fileSystem;

    @Mock
    private InputProject inputProject;

    @Mock
    private NewIssue newIssue;

    @Mock
    private NewAdHocRule newAdHocRule;

    @Mock
    private NewExternalIssue newExternalIssue;

    @Mock
    private NewIssueLocation newIssueLocation;

    @Mock
    private JavaResourceResolver resourceResolver;

    @Mock
    private RuleKeyResolver ruleResolver;

    private IssueHandler issueHandler;

    @BeforeEach
    public void setUp() {
        doReturn("Java").when(resourceResolver).getLanguage();
        issueHandler = new IssueHandler(resourceResolver, ruleResolver);
        doReturn(fileSystem).when(sensorContext).fileSystem();
    }


    /**
     * Verifies that invalid concepts are reported on project level
     */
    @Test
    public void invalidConceptOnProjectLevel() {
        ConceptType conceptType = new ConceptType();
        conceptType.setDescription("TestConcept");
        conceptType.setId("test:Concept");
        doReturn(PROJECT_PATH).when(fileSystem).baseDir();
        doReturn(Optional.of(CONCEPT_RULE.ruleKey())).when(ruleResolver).resolve(CONCEPT);
        doReturn(inputProject).when(sensorContext).project();
        stubNewIssue();

        issueHandler.process(sensorContext, PROJECT_PATH, conceptType);

        verify(sensorContext).newIssue();
        verify(newIssue).forRule(CONCEPT_RULE.ruleKey());
        verify(newIssue).newLocation();
        verify(newIssueLocation).message("[test:Concept] The concept could not be applied: TestConcept");
    }

    /**
     * Verifies that invalid concepts are not reported on module level
     */
    @Test
    public void invalidConceptOnModuleLevle() {
        ConceptType conceptType = new ConceptType();
        conceptType.setDescription("TestConcept");
        conceptType.setId("test:Concept");
        doReturn(new File(PROJECT_PATH, "module")).when(fileSystem).baseDir();

        issueHandler.process(sensorContext, PROJECT_PATH, conceptType);

        verify(sensorContext, never()).newIssue();
    }

    /**
     * Verifies that violated constraints without a source location are reported on project level
     */
    @Test
    public void constraintViolationWithoutSourceLocation() {
        ConstraintType constraintType = new ConstraintType();
        constraintType.setDescription("TestConstraint");
        constraintType.setId("test:Constraint");
        constraintType.setResult(createResultType(false));
        doReturn(PROJECT_PATH).when(fileSystem).baseDir();
        doReturn(Optional.of(CONSTRAINT_RULE.ruleKey())).when(ruleResolver).resolve(CONSTRAINT);
        doReturn(inputProject).when(sensorContext).project();
        stubNewIssue();

        issueHandler.process(sensorContext, PROJECT_PATH, constraintType);

        verify(sensorContext).newIssue();
        verify(newIssue).forRule(CONSTRAINT_RULE.ruleKey());
        verify(newIssue).newLocation();
        verify(newIssueLocation).on(inputProject);
        verify(newIssueLocation).message("[test:Constraint] TestConstraint\nValue:Test\n");
    }

    /**
     * Verifies that violated constraints with a source location are reported on the referenced element if it can be resolved.
     */
    @Test
    public void constraintViolationWithMatchingSourceLocation() {
        ConstraintType constraintType = new ConstraintType();
        constraintType.setDescription("TestConstraint");
        constraintType.setId("test:Constraint");
        constraintType.setSeverity(getSeverityType(CRITICAL));
        constraintType.setResult(createResultType(true));
        stubExternalNewIssue();
        stubSourceLocation();

        issueHandler.process(sensorContext, PROJECT_PATH, constraintType);

        verify(sensorContext).newAdHocRule();
        verify(newAdHocRule).engineId(JQAssistant.NAME);
        verify(newAdHocRule).ruleId("test:Constraint");
        verify(newAdHocRule).description("TestConstraint");
        verify(newAdHocRule).severity(Severity.MAJOR);

        verify(sensorContext).newExternalIssue();
        verify(newExternalIssue).engineId(JQAssistant.NAME);
        verify(newExternalIssue).ruleId("test:Constraint");
        verify(newExternalIssue).severity(Severity.CRITICAL);
        verify(newExternalIssue).newLocation();
    }

    /**
     * Verifies that violated constraints with a source location are not reported on the referenced element if it cannot be resolved (e.g. in another module).
     */
    @Test
    public void constraintViolationWithoutMatchingSourceLocation() {
        ConstraintType constraintType = new ConstraintType();
        constraintType.setDescription("TestConstraint");
        constraintType.setId("test:Constraint");
        constraintType.setResult(createResultType(true));

        issueHandler.process(sensorContext, PROJECT_PATH, constraintType);

        verify(sensorContext, never()).newIssue();
    }

    private SeverityType getSeverityType(com.buschmais.jqassistant.core.rule.api.model.Severity severity) {
        SeverityType severityType = new SeverityType();
        severityType.setValue(CRITICAL.getValue());
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
        columnsHeaderType.getColumn().add(columnHeaderType);
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
            SourceType sourceType = new SourceType();
            sourceType.setName("com/buschmais/jqassistant/examples/sonar/project/Bar.class");
            sourceType.setLine(16);
            columnType.setSource(sourceType);
        }

        rowType.getColumn().add(columnType);
        rowsType.getRow().add(rowType);
        resultType.setRows(rowsType);
        return resultType;
    }

    private void stubNewIssue() {
        doReturn(newIssue).when(sensorContext).newIssue();
        doReturn(newIssueLocation).when(newIssue).newLocation();
        doReturn(newIssue).when(newIssue).forRule(any(RuleKey.class));
        doReturn(newIssue).when(newIssue).at(newIssueLocation);
        stubNewIssueLocation();
    }

    private void stubExternalNewIssue() {
        doReturn(newAdHocRule).when(sensorContext).newAdHocRule();
        doReturn(newAdHocRule).when(newAdHocRule).engineId(anyString());
        doReturn(newAdHocRule).when(newAdHocRule).ruleId(anyString());
        doReturn(newAdHocRule).when(newAdHocRule).name(anyString());
        doReturn(newAdHocRule).when(newAdHocRule).description(anyString());
        doReturn(newAdHocRule).when(newAdHocRule).type(any(org.sonar.api.rules.RuleType.class));
        doReturn(newAdHocRule).when(newAdHocRule).severity(any(Severity.class));

        doReturn(newExternalIssue).when(sensorContext).newExternalIssue();
        doReturn(newExternalIssue).when(newExternalIssue).type(any(org.sonar.api.rules.RuleType.class));
        doReturn(newIssueLocation).when(newExternalIssue).newLocation();
        doReturn(newExternalIssue).when(newExternalIssue).engineId(anyString());
        doReturn(newExternalIssue).when(newExternalIssue).ruleId(anyString());
        doReturn(newExternalIssue).when(newExternalIssue).severity(any(Severity.class));
        doReturn(newExternalIssue).when(newExternalIssue).at(newIssueLocation);
        stubNewIssueLocation();
    }

    private void stubNewIssueLocation() {
        doReturn(newIssueLocation).when(newIssueLocation).message(any(String.class));
        doReturn(newIssueLocation).when(newIssueLocation).on(any(InputComponent.class));
    }

    private void stubSourceLocation() {
        InputPath javaResource = mock(InputPath.class, withSettings().extraInterfaces(InputFile.class));
        when(resourceResolver.resolve(any(FileSystem.class), any(String.class), any(String.class), any(String.class))).thenReturn(javaResource);
        when(((InputFile) javaResource).selectLine(16))
            .thenReturn(new DefaultTextRange(new DefaultTextPointer(16, 0), new DefaultTextPointer(16, 1)));
    }
}
