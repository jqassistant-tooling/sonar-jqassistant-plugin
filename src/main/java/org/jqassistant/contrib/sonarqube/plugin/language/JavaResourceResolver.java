package org.jqassistant.contrib.sonarqube.plugin.language;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.jqassistant.contrib.sonarqube.plugin.sensor.JQAssistantSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import com.google.common.collect.Lists;

/**
 * Implementation of a
 * {@link ResourceResolver}
 * for java elements.
 */
@BatchSide
public class JavaResourceResolver implements ResourceResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(JQAssistantSensor.class);

    private final FileSystem fileSystem;

    public JavaResourceResolver(FileSystem moduleFileSystem) {
        this.fileSystem = moduleFileSystem;
    }

    @Override
    public String getLanguage() {
        return "java";
    }

    @Override
    public InputPath resolve(String nodeType, String nodeSource, String nodeValue) {
        switch (nodeType) {
        case "Type":
        case "Field":
        case "Method":
        case "MethodInvocation":
        case "ReadField":
        case "WriteField":
            // TODO: Using 'nodeSource' is working only on file level, can we
            // identify a method... as resource?
            final String javaFilePath = determineRelativeQualifiedJavaSourceFileName(nodeSource);
            return findMatchingResourceFile(javaFilePath);
        case "Package":
            return findMatchingResourceDirectory(nodeValue.replace('.', '/'));
        default:
            return null;
        }
    }

    /**
     * This resolver can find only resources in the current project, because
     * only such resources are part of the 'index cache'.
     *
     * @return The matching resource or <code>null</code> if nothing was found
     *         and in case of multiple matches.
     */
    private InputFile findMatchingResourceFile(String javaFilePath) {
        // in SonarQ Java files have the prefix 'src/main/java' for Maven
        // projects
        // we have to handle such nested project structures without specific
        // knowledge about project structures... so use pattern matcher :-)
        Iterator<InputFile> files = fileSystem.inputFiles(fileSystem.predicates().matchesPathPattern("**/" + javaFilePath)).iterator();
        while (files.hasNext()) {
            InputFile file = files.next();
            if (files.hasNext()) {
                // ups, more entries?!
                LOGGER.error("Multiple matches for Java file {}, cannot handle source file for violations", javaFilePath);
                return null;
            }
            return file;
        }
        return null;
    }

    private InputDir findMatchingResourceDirectory(String javaPackageDirPath) {
        // for packages (directories) exists no pattern matching api, so we have
        // to check all available source directories for the package
        List<java.io.File> dirs = new ArrayList<>(2);
        dirs.addAll(Lists.newArrayList(fileSystem.files(fileSystem.predicates().hasType(InputFile.Type.MAIN))));
        dirs.addAll(Lists.newArrayList(fileSystem.files(fileSystem.predicates().hasType(InputFile.Type.TEST))));

        java.io.File packageDir;
        for (File dir : dirs) {
            packageDir = new File(dir, javaPackageDirPath);
            if (!packageDir.exists()) {
                continue;
            }
            final InputDir id = fileSystem.inputDir(packageDir);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    /**
     * Convert a given entry like
     * <code>com/buschmais/jqassistant/examples/sonar/project/Bar.class</code>
     * into a source file name like
     * <code>com/buschmais/jqassistant/examples/sonar/project/Bar.java</code>.
     */
    private String determineRelativeQualifiedJavaSourceFileName(String classFileName) {
        if (classFileName == null || classFileName.isEmpty()) {
            return null;
        }
        String result = classFileName;
        if (result.charAt(0) == '/') {
            result = result.substring(1);
        }
        if (result.toLowerCase(Locale.ENGLISH).endsWith(".class")) {
            result = result.substring(0, result.length() - ".class".length());
        }
        // remove nested class fragments
        int index = result.indexOf('$');
        if (index > -1) {
            result = result.substring(0, index);
        }
        return result.concat(".java");
    }
}
