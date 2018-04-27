package org.jqassistant.contrib.sonarqube.plugin.language;

import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputPath;

/**
 * Defines an interface for resolving resources representing language specific
 * elements, e.g. java classes.
 */
@ScannerSide
public interface ResourceResolver {

	/**
	 * Return the language this resolver represents.
	 *
	 * @return The language.
	 */
	String getLanguage();

	/**
	 * Resolve the resource for an element of a given type.
	 * @param nodeType
	 *            The type declaration in report.
	 * @param nodeSource
	 * 			The source name producing the node element in report (e.g. the class file name for java classes).
	 * @param nodeValue
	 *            The value of the node element in report (e.g. the class name).
	 *
	 * @return The resource or <code>null</code> if not resolved.
	 */
	InputPath resolve(String nodeType, String nodeSource, String nodeValue);
}
