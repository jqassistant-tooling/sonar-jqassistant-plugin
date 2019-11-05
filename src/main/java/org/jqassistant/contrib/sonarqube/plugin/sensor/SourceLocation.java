package org.jqassistant.contrib.sonarqube.plugin.sensor;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sonar.api.batch.fs.InputComponent;

import java.util.Optional;

/**
 * Helper class to hold resource + line number of source together.
 *
 * @author rzozmann
 */
@Builder
@Getter
@ToString
final class SourceLocation {

    private Optional<InputComponent> resource;

    private Optional<Integer> lineNumber;

}
