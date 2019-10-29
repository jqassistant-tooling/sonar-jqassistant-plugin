package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.sonar.api.batch.fs.InputComponent;

/**
 * Helper class to hold resource + line number of source together.
 *
 * @author rzozmann
 *
 */
final class SourceLocation {
	final InputComponent resource;
	final Integer lineNumber;

	SourceLocation(InputComponent resource, Integer lineNumber) {
		this.resource = resource;
		this.lineNumber = lineNumber;
	}
}
