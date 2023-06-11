package org.jqassistant.tooling.sonarqube.plugin.sensor;

import org.jqassistant.schema.report.v2.ColumnType;
import org.jqassistant.schema.report.v2.ExecutableRuleType;
import org.jqassistant.schema.report.v2.RowType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Verifies the {@link IssueKeyProvider}.
 */
@ExtendWith(MockitoExtension.class)
class IssueKeyProviderTest {

    private final IssueKeyProvider issueKeyProvider = new IssueKeyProvider();

    @Mock
    private ExecutableRuleType executableRuleType;

    @Test
    void conceptIssueKey() {
        doReturn("test:Concept").when(executableRuleType)
            .getId();

        String issueKey = issueKeyProvider.getIssueKey(executableRuleType, RuleType.CONCEPT);

        assertThat(issueKey).hasSize(64);
        verify(executableRuleType).getId();
    }

    @Test
    void constraintIssueKeyWithRowKey() {
        RowType rowType = mock(RowType.class);
        doReturn("1").when(rowType)
            .getKey();

        String issueKey = issueKeyProvider.getIssueKey(executableRuleType, rowType, RuleType.CONSTRAINT);

        assertThat(issueKey).isEqualTo("1");
        verify(executableRuleType, never()).getId();
        verify(rowType).getKey();
    }

    @Test
    void constraintIssueKeyWithoutRowKey() {
        doReturn("test:Constraint").when(executableRuleType)
            .getId();
        RowType rowType = mock(RowType.class);
        ColumnType columnType = mock(ColumnType.class);
        doReturn(singletonList(columnType)).when(rowType)
            .getColumn();
        doReturn("value").when(columnType)
            .getValue();

        String issueKey = issueKeyProvider.getIssueKey(executableRuleType, rowType, RuleType.CONSTRAINT);

        assertThat(issueKey).hasSize(64);
        verify(rowType).getKey();
        verify(executableRuleType).getId();
        verify(columnType).getValue();
    }
}
