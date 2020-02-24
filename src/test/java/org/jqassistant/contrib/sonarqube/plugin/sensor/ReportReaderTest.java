package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.jqassistant.schema.report.v1.JqassistantReport;
import org.jqassistant.schema.report.v1.ReferencableRuleType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link ReportReader}.
 */
class ReportReaderTest {

    private ReportReader reportReader = ReportReader.getInstance();

    @Test
    void validReport() throws URISyntaxException {
        File file = new File(JQAssistantSensorTest.class.getResource("/jqassistant-report-no-issue.xml").toURI().getPath());

        JqassistantReport report = reportReader.read(file);

        assertThat(report).isNotNull();
        List<ReferencableRuleType> ruleTypes = report.getGroupOrConceptOrConstraint();
        assertThat(ruleTypes).hasSize(1);
    }


    @Test
    void textFile() throws URISyntaxException {
        File file = new File(JQAssistantSensorTest.class.getResource("/invalid-report.txt").toURI().getPath());

        assertThrows(IllegalStateException.class, () -> reportReader.read(file));
    }
}
