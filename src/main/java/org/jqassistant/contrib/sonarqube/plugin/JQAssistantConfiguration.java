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

import org.sonar.api.PropertyType;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rules.RuleType;
import org.sonar.api.scanner.ScannerSide;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.sonar.api.rules.RuleType.CODE_SMELL;

/**
 * Define settings for jQAssistant affecting the execution while SONAR run.
 *
 * @author rzozmann
 */
@ScannerSide
public class JQAssistantConfiguration {

    public static final String DISABLED = "sonar.jqassistant.disabled";

    /**
     * Defines the path for the jQAssistant XML report relative to the root
     * directory of the project.
     */
    public static final String REPORT_PATH = "sonar.jqassistant.reportPath";

    /**
     * The default value of the jQAssistant XML report.
     */
    public static final String DEFAULT_REPORT_PATH = "target/jqassistant/jqassistant-report.xml";

    /**
     * Defines the issue type which shall be created, allowed values are defined by {@link org.sonar.api.rules.RuleType}.
     */
    public static final String ISSUE_TYPE = "sonar.jqassistant.issueType";

    public static final String CATEGORY_JQASSISTANT = "jQAssistant";

    private final Configuration sonarConfiguration;

    public JQAssistantConfiguration(Configuration sonarConfiguration) {
        this.sonarConfiguration = sonarConfiguration;
    }

    /**
     * Return the configured report path as {@link String}.
     *
     * @return The configured report path representing the jQAssistant XML report.
     */
    public String getReportFile() {
        return sonarConfiguration.get(REPORT_PATH).orElse(DEFAULT_REPORT_PATH);
    }

    /**
     * @return FALSE if jQAssistant is enabled on project.
     */
    public boolean isSensorDisabled() {
        Optional<Boolean> disabled = sonarConfiguration.getBoolean(DISABLED);
        return disabled.isPresent() && disabled.get();
    }

    /**
     * Return the configured issue type as {@link RuleType}.
     *
     * @return The issue type.
     */
    public RuleType getIssueType() {
        return sonarConfiguration.get(ISSUE_TYPE).map(issueType -> RuleType.valueOf(issueType.toUpperCase(Locale.getDefault()))).orElse(CODE_SMELL);
    }

    public static List<PropertyDefinition> getPropertyDefinitions() {
        return asList(
            PropertyDefinition.builder(REPORT_PATH).category(CATEGORY_JQASSISTANT).subCategory(JQAssistant.NAME).name("jQAssistant Report Path")
                .description("Absolute or relative path to the jQAssistant XML report file (default: '<projectRoot>/" + DEFAULT_REPORT_PATH + "').")
                .onQualifiers(Qualifiers.PROJECT).build(),
            PropertyDefinition.builder(JQAssistantConfiguration.DISABLED).defaultValue(Boolean.toString(false)).name("Disable")
                .category(CATEGORY_JQASSISTANT).subCategory(JQAssistant.NAME).description("Disable the jQAssistant sensor.")
                .onQualifiers(Qualifiers.PROJECT).type(PropertyType.BOOLEAN).build(),
            PropertyDefinition.builder(JQAssistantConfiguration.ISSUE_TYPE).defaultValue(CODE_SMELL.toString()).name("Issue Type")
                .category(CATEGORY_JQASSISTANT).subCategory(JQAssistant.NAME).description("The issue type to create.")
                .onQualifiers(Qualifiers.PROJECT).options(RuleType.names().stream().collect(toList())).type(PropertyType.SINGLE_SELECT_LIST).build());
    }
}
