package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.jqassistant.schema.report.v2.ExecutableRuleType;
import org.jqassistant.schema.report.v2.RowType;
import org.sonar.api.scanner.ScannerSide;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

/**
 * Calculates unique issue keys.
 */
@ScannerSide
public class IssueKeyProvider {

    String getIssueKey(ExecutableRuleType executableRuleType, RuleType ruleType) {
        return getIssueKey(ruleType, executableRuleType.getId(), null);
    }

    String getIssueKey(ExecutableRuleType executableRuleType, RowType rowType, RuleType ruleType) {
        String rowKey = rowType.getKey();
        if (rowKey != null) {
            return rowKey;
        }
        return getIssueKey(ruleType, executableRuleType.getId(), rowType);
    }

    private String getIssueKey(RuleType ruleType, String ruleId, RowType rowType) {
        StringBuilder id = new StringBuilder(ruleType.name()).append("|")
            .append(ruleId)
            .append("|");
        if (rowType != null) {
            rowType.getColumn()
                .stream()
                .forEach(columnType -> id.append(columnType.getValue()));
        }
        return sha256Hex(id.toString());
    }
}
