package org.jqassistant.contrib.sonarqube.plugin.language;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.scanner.ScannerSide;

import java.util.Iterator;

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
     * @return The matching resource or <code>null</code> if nothing was found and
     * in case of multiple matches.
     */
    public InputFile resolve(FileSystem fileSystem, String sourceFile) {
        // in SonarQ Java files have the prefix 'src/main/java' for Maven projects
        // we have to handle such nested project structures without specific
        // knowledge about project structures... so use pattern matcher :-)
        Iterator<InputFile> files = fileSystem.inputFiles(fileSystem.predicates().matchesPathPattern("**" + sourceFile)).iterator();
        while (files.hasNext()) {
            InputFile file = files.next();
            if (!files.hasNext()) {
                return file;
            }
            LOGGER.info("Multiple matches for file {}, cannot safely determine source file.", sourceFile);
            return null;
        }
        return null;
    }

}
