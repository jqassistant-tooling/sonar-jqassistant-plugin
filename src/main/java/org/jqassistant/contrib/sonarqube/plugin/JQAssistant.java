package org.jqassistant.contrib.sonarqube.plugin;

/**
 * Defines constants for the jQAssistant plugin.
 */
public final class JQAssistant {

	/**
	 * Private constructor.
	 */
	private JQAssistant() {
	}

	/**
	 * The repository key.
	 */
	public static final String KEY = "jqassistant";

	/**
	 * The repository name.
	 */
	public static final String NAME = "jQAssistant";

    /**
     * The absolute path to the project directory, may be empty for single-module projects.
     */
    public static final String SETTINGS_KEY_PROJECT_PATH = "sonar.jqassistant.projectPath";

    /**
     * Defines the path for the jQAssistant XML report relative to the root directory of the project.
     */
	public static final String SETTINGS_KEY_REPORT_PATH = "sonar.jqassistant.reportPath";

	/**
	 * The default value of the jQAssistant XML report.
	 */
	public static final String SETTINGS_VALUE_DEFAULT_REPORT_PATH = "target/jqassistant/jqassistant-report.xml";

}
