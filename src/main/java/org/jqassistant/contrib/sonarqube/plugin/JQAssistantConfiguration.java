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

import java.util.List;

import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import com.google.common.collect.ImmutableList;

/**
 * Define settings for jQAssistant affecting the execution while SONAR run.
 *
 * @author rzozmann
 *
 */
@ScannerSide
public class JQAssistantConfiguration {

    public static final String DISABLED = "sonar.jqassistant.disabled";

    private final Configuration settings;

    public JQAssistantConfiguration(Configuration settings) {
        this.settings = settings;
    }

    public String getReportPath() {
        return settings.get(JQAssistant.SETTINGS_KEY_REPORT_PATH).get();
    }

    /**
     *
     * @return FALSE if jQAssistant is enabled on project.
     */
    public boolean isSensorDisabled() {
        return settings.getBoolean(DISABLED).get();
    }

    public static List<PropertyDefinition> getPropertyDefinitions() {
        String subCategory = "jQAssistant";
        return ImmutableList.of(
                PropertyDefinition.builder(JQAssistant.SETTINGS_KEY_REPORT_PATH).defaultValue(JQAssistant.SETTINGS_VALUE_DEFAULT_REPORT_FILE_PATH)
                        .category(CoreProperties.CATEGORY_GENERAL).subCategory(subCategory).name("jQAssistant Report")
                        .description(
                                "Path to the jQAssistant report file containing data by checks. The path may be absolute or relative to the project base directory.")
                        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE).build(),
                PropertyDefinition.builder(JQAssistantConfiguration.DISABLED).defaultValue(Boolean.toString(false)).name("Disable")
                        .category(CoreProperties.CATEGORY_GENERAL).subCategory(subCategory).description("Do not execute jQAssistant.")
                        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE).type(PropertyType.BOOLEAN).build());
    }

}
