package org.jqassistant.contrib.sonarqube.plugin.sensor;

import java.io.File;
import java.util.Optional;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static lombok.AccessLevel.PRIVATE;

/**
 * Locator for the jQAssistant XML report.
 */
public final class ReportLocator {

    @Builder
    @Getter
    @RequiredArgsConstructor(access = PRIVATE)
    @EqualsAndHashCode
    @ToString
    public static class ReportLocation {

        private final File moduleDirectory;

        private final File reportFile;

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportLocator.class);

    private ReportLocator() {
    }

    /**
     * Return the configured report {@link File}.
     * <p>
     * Absolute paths are resolved directly, relative paths are scanned starting
     * from moduleDir upwards to projectDir until the first file is found.
     *
     * @param projectDir
     *            The project directory (i.e. root module directory).
     * @param moduleDir
     *            The module directory.
     * @param reportPath
     *            The configured path to the XML report file.
     *
     * @return The configured {@link File} representing the jQAssistant XML report.
     */
    public static Optional<ReportLocation> resolveReportFile(File projectDir, File moduleDir, String reportPath) {
        LOGGER.debug("Using jQAssistant report path {}.", reportPath);
        File reportFile = new File(reportPath);
        if (reportFile.isAbsolute() && reportFile.exists()) {
            LOGGER.info("Found jQAssistant XML report with absolute path {}.", reportFile);
            return Optional.of(ReportLocation.builder().moduleDirectory(projectDir).reportFile(reportFile).build());
        }
        LOGGER.debug("Scanning for jQAssistant XML report with relative path starting from module {}.", moduleDir);
        File currentModuleDir = moduleDir;
        while (currentModuleDir.getAbsolutePath().startsWith(projectDir.getAbsolutePath())) {
            reportFile = new File(currentModuleDir, reportPath);
            if (reportFile.exists()) {
                return Optional.of(ReportLocation.builder().moduleDirectory(currentModuleDir).reportFile(reportFile).build());
            }
            currentModuleDir = currentModuleDir.getParentFile();
        }
        return Optional.empty();
    }

}
