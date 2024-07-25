package org.jqassistant.tooling.sonarqube.plugin.sensor;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReportLocatorTest {

    private static final File PROJECT_DIR = new File("src/test/resources/project-root");
    private static final String MODULE_2 = "src/test/resources/project-root/module2";
    private static final String MODULE_1 = "src/test/resources/project-root/module1";

    private static final String REPORT_PATH = "build/jqassistant-report.xml";
    private static final String NON_EXISTING_REPORT_PATH = "build/non-existing-jqassistant-report.xml";

    @Test
    void relativePathWithReportInModule() {
        File moduleDir = new File(MODULE_2);
        Optional<ReportLocator.ReportLocation> reportLocation = ReportLocator.resolveReportFile(PROJECT_DIR, moduleDir, REPORT_PATH);
        assertThat(reportLocation).isPresent()
                .contains(ReportLocator.ReportLocation.builder().moduleDirectory(moduleDir).reportFile(new File(moduleDir, REPORT_PATH)).build());
    }

    @Test
    void relativePathWithWithReportInParent() {
        File moduleDir = new File(MODULE_1);
        Optional<ReportLocator.ReportLocation> reportLocation = ReportLocator.resolveReportFile(PROJECT_DIR, moduleDir, REPORT_PATH);
        assertThat(reportLocation).isPresent()
                .contains(ReportLocator.ReportLocation.builder().moduleDirectory(PROJECT_DIR).reportFile(new File(PROJECT_DIR, REPORT_PATH)).build());
    }

    @Test
    void noNExistingRelativePath() {
        File moduleDir = new File(MODULE_1);
        Optional<ReportLocator.ReportLocation> reportLocation = ReportLocator.resolveReportFile(PROJECT_DIR, moduleDir, NON_EXISTING_REPORT_PATH);
        assertThat(reportLocation).isEmpty();
    }

    @Test
    void absolutePath() {
        File moduleDir = new File(MODULE_2);
        Optional<ReportLocator.ReportLocation> reportLocation = ReportLocator.resolveReportFile(PROJECT_DIR, moduleDir, new File(PROJECT_DIR, REPORT_PATH).getAbsolutePath());
        assertThat(reportLocation).isPresent()
                .contains(ReportLocator.ReportLocation.builder().moduleDirectory(PROJECT_DIR).reportFile(new File(PROJECT_DIR, REPORT_PATH).getAbsoluteFile()).build());
    }

    @Test
    void nonExistingAbsolutePath() {
        File moduleDir = new File(MODULE_2);
        Optional<ReportLocator.ReportLocation> reportLocation = ReportLocator.resolveReportFile(PROJECT_DIR, moduleDir,
                new File(PROJECT_DIR, NON_EXISTING_REPORT_PATH).getAbsolutePath());
        assertThat(reportLocation).isEmpty();
    }
}
