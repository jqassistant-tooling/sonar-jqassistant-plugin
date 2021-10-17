package org.jqassistant.contrib.sonarqube.plugin.language;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.model.GeneratedFile;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class SourceFileResolverTest {

    @Mock
    private FileSystem fileSystem;

    @Mock
    private FilePredicates predicates;

    @Mock
    private FilePredicate predicate;

    private SourceFileResolver sourceFileResolver = new SourceFileResolver();

    @Test
    public void singleFileMatches() {
        stubFileSystem();
        Path path = Paths.get("src/test/java/org/jqassistant/contrib/Test.java");
        Iterable<InputFile> it = singletonList(toInputFile(path));
        doReturn(it).when(fileSystem).inputFiles(predicate);

        InputFile result = sourceFileResolver.resolve(fileSystem, "/org/jqassistant/contrib/Test.class");

        assertEquals(Paths.get(result.uri()), path.toAbsolutePath());
    }

    @Test
    public void multipleFilesMatch() {
        stubFileSystem();
        Path path1 = Paths.get("/src/main/java/org/jqassistant/contrib/Test.java");
        Path path2 = Paths.get("/src/test/java/org/jqassistant/contrib/Test.java");
        Iterable<InputFile> it = asList(toInputFile(path1), toInputFile(path2));
        doReturn(it).when(fileSystem).inputFiles(predicate);

        InputFile result = sourceFileResolver.resolve(fileSystem, "/org/jqassistant/contrib/Test.class");

        assertNull(result);
    }

    @Test
    public void noFilesMatch() {
        stubFileSystem();
        doReturn(emptyList()).when(fileSystem).inputFiles(predicate);

        InputFile result = sourceFileResolver.resolve(fileSystem, "/org/jqassistant/contrib/Test.class");

        assertNull(result);
    }

    private void stubFileSystem() {
        doReturn(predicates).when(fileSystem).predicates();
        doReturn(predicate).when(predicates).matchesPathPattern(anyString());
    }

    private InputFile toInputFile(Path javaPath) {
        return new GeneratedFile(javaPath);
    }
}