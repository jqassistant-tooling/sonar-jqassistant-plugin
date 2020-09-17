package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReportLocatorTest {

    private static final File PROJECT_DIR = new File("src/test/resources/project-root");
    private static final String REPORT_PATH = "build/jqassistant-report.xml";
    public static final String NON_EXISTING_REPORT_PATH = "build/non-existing-jqassistant-report.xml";

    @Test
    public void relativePathWithReportInModule() {
        File moduleDir = new File("src/test/resources/project-root/module2");
        Optional<File> reportFile = ReportLocator.resolveReportFile(PROJECT_DIR, moduleDir, REPORT_PATH);
        assertThat(reportFile).isPresent().contains(new File(moduleDir, REPORT_PATH));
    }

    @Test
    public void relativePathWithWithReportInParent() {
        File moduleDir = new File("src/test/resources/project-root/module1");
        Optional<File> reportFile = ReportLocator.resolveReportFile(PROJECT_DIR, moduleDir, REPORT_PATH);
        assertThat(reportFile).isPresent().contains(new File(PROJECT_DIR, REPORT_PATH));
    }

    @Test
    public void noNExistingRelativePath() {
        File moduleDir = new File("src/test/resources/project-root/module1");
        Optional<File> reportFile = ReportLocator.resolveReportFile(PROJECT_DIR, moduleDir, NON_EXISTING_REPORT_PATH);
        assertThat(reportFile).isEmpty();
    }

    @Test
    public void absolutePath() {
        File moduleDir = new File("src/test/resources/project-root/module2");
        Optional<File> reportFile = ReportLocator.resolveReportFile(PROJECT_DIR, moduleDir, new File(PROJECT_DIR, REPORT_PATH).getAbsolutePath());
        assertThat(reportFile).isPresent().contains(new File(PROJECT_DIR, REPORT_PATH).getAbsoluteFile());
    }

    @Test
    public void nonExistingAbsolutePath() {
        File moduleDir = new File("src/test/resources/project-root/module2");
        Optional<File> reportFile = ReportLocator.resolveReportFile(PROJECT_DIR, moduleDir, new File(PROJECT_DIR, NON_EXISTING_REPORT_PATH).getAbsolutePath());
        assertThat(reportFile).isEmpty();
    }
}
