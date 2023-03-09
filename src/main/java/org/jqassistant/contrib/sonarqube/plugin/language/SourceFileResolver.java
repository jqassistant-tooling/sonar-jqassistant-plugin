package org.jqassistant.contrib.sonarqube.plugin.language;

import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.scanner.ScannerSide;

import static java.util.Optional.empty;
import static java.util.Optional.of;

@ScannerSide
public class SourceFileResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceFileResolver.class);

    /**
     * This resolver can find only resources in the current project, because only
     * such resources are part of the 'index cache'.
     *
     * @param fileSystem
     *     The {@link FileSystem}.
     * @param sourceFile
     *     The name of the source file.
     * @param isRelative
     *     <code>true</code> if the path to the source file is relative to a parent artifact
     * @return The matching resource or <code>null</code> if nothing was found and
     * in case of multiple matches.
     */
    public Optional<InputFile> resolve(FileSystem fileSystem, String sourceFile, boolean isRelative) {
        // in SonarQ Java files have the prefix 'src/main/java' for Maven projects
        // we have to handle such nested project structures without specific
        // knowledge about project structures... so use pattern matcher :-)
        String relativeSourceFile = relativize(fileSystem, sourceFile, isRelative);
        Iterator<InputFile> files = fileSystem.inputFiles(fileSystem.predicates()
                .matchesPathPattern("**" + relativeSourceFile))
            .iterator();
        while (files.hasNext()) {
            InputFile file = files.next();
            if (files.hasNext()) {
                LOGGER.info("Multiple matches for file {}, cannot safely determine source file.", sourceFile);
                return empty();
            }
            return of(file);
        }
        return empty();
    }

    private String relativize(FileSystem fileSystem, String sourceFile, boolean relative) {
        if (relative) {
            return sourceFile;
        }
        return Paths.get(fileSystem.baseDir().getPath()).relativize(Paths.get(sourceFile)).toString();
    }
}
