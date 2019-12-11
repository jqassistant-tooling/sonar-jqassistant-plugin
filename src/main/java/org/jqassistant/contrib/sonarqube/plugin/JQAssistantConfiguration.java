/*
 * SonarQube Java
 * Copyright (C) 2010-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.jqassistant.contrib.sonarqube.plugin;

import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.scanner.ScannerSide;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

/**
 * Define settings for jQAssistant affecting the execution while SONAR run.
 *
 * @author rzozmann
 */
@ScannerSide
public class JQAssistantConfiguration {

    public static final String DISABLED = "sonar.jqassistant.disabled";

    /**
     * Defines the path for the jQAssistant XML report relative to the root directory of the project.
     */
    public static final String REPORT_PATH = "sonar.jqassistant.reportPath";

    /**
     * The default value of the jQAssistant XML report.
     */
    public static final String DEFAULT_REPORT_PATH = "target/jqassistant/jqassistant-report.xml";

    private final Configuration settings;

    public JQAssistantConfiguration(Configuration settings) {
        this.settings = settings;
    }

    /**
     * The path is relative or absolute.
     *
     * @param projectDir The project directory (i.e. root module directory).
     * @param moduleDir  The module directory.
     */
    public File getReportFile(File projectDir, File moduleDir) {
        Optional<String> reportPath = settings.get(REPORT_PATH);
        if (reportPath.isPresent()) {
            File reportFile = new File(reportPath.get());
            return reportFile.isAbsolute() ? reportFile : new File(moduleDir, reportPath.get());
        }
        return new File(projectDir, JQAssistantConfiguration.DEFAULT_REPORT_PATH).getAbsoluteFile();
    }

    /**
     * @return FALSE if jQAssistant is enabled on project.
     */
    public boolean isSensorDisabled() {
        Optional<Boolean> disabled = settings.getBoolean(DISABLED);
        return disabled.isPresent() && disabled.get();
    }

    public static List<PropertyDefinition> getPropertyDefinitions() {
        return asList(
            PropertyDefinition.builder(REPORT_PATH)
                .category(CoreProperties.CATEGORY_GENERAL).subCategory(JQAssistant.NAME).name("jQAssistant Report Path")
                .description(
                    "Absolute or relative path to the jQAssistant XML report file (default: '" + DEFAULT_REPORT_PATH + "').")
                .onQualifiers(Qualifiers.PROJECT).build(),
            PropertyDefinition.builder(JQAssistantConfiguration.DISABLED).defaultValue(Boolean.toString(false)).name("Disable")
                .category(CoreProperties.CATEGORY_GENERAL).subCategory(JQAssistant.NAME).description("Disable the jQAssistant sensor.")
                .onQualifiers(Qualifiers.PROJECT).type(PropertyType.BOOLEAN).build());
    }

}
