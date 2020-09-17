package org.jqassistant.contrib.sonarqube.plugin.sensor;

import java.io.File;
import java.util.Optional;

public final class ReportLocator {

    private ReportLocator() {
    }

    /**
     * Return the configured report {@link File}.
     *
     * @param projectDir The project directory (i.e. root module directory).
     * @param moduleDir  The module directory.
     * @param reportPath The configured path to the XML report file.
     * @return The configured {@link File} representing the jQAssistant XML report.
     */
    public static Optional<File> resolveReportFile(File projectDir, File moduleDir, String reportPath) {
        File reportFile = new File(reportPath);
        if (reportFile.isAbsolute() && reportFile.exists()) {
            return Optional.of(reportFile);
        }
        File currentModuleDir = moduleDir;
        while (currentModuleDir.getAbsolutePath().startsWith(projectDir.getAbsolutePath())) {
            reportFile = new File(currentModuleDir, reportPath);
            if (reportFile.exists()) {
                return Optional.of(reportFile);
            }
            currentModuleDir = currentModuleDir.getParentFile();
        }
        return Optional.empty();
    }

}
