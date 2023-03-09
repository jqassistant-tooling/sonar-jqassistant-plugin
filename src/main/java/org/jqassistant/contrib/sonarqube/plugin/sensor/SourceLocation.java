package org.jqassistant.contrib.sonarqube.plugin.sensor;

import java.util.Optional;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sonar.api.batch.fs.InputFile;

/**
 * Helper class to hold resource + line number of source together.
 *
 * @author rzozmann
 */
@Builder
@Getter
@ToString
final class SourceLocation {

    private InputFile inputFile;

    private Optional<Integer> startLine;

    private Optional<Integer> endLine;
}
