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
	 * Maven properties key to define a default lookup folder for the
	 * jQAssistant report file.
	 */
	public static final String SETTINGS_KEY_REPORT_PATH = "sonar.jqassistant.reportPath";

	/**
	 * Relative path + filename of jQAssistant report file.
	 */
	public static final String SETTINGS_VALUE_DEFAULT_REPORT_FILE_PATH = "target/jqassistant/jqassistant-report.xml";

}
