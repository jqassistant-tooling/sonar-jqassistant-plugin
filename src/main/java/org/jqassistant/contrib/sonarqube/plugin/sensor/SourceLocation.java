package org.jqassistant.contrib.sonarqube.plugin.sensor;

import org.sonar.api.batch.fs.InputPath;

/**
 * Helper class to hold resource + line number of source together.
 *
 * @author rzozmann
 *
 */
final class SourceLocation {
	final InputPath resource;
	final Integer lineNumber;

	SourceLocation(InputPath resource, Integer lineNumber) {
		this.resource = resource;
		this.lineNumber = lineNumber;
	}
}
