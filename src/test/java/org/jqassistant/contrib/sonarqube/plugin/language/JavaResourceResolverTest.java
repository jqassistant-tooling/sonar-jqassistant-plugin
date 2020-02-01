package org.jqassistant.contrib.sonarqube.plugin.language;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.batch.fs.*;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class JavaResourceResolverTest {

    @Mock
    private FileSystem fileSystem;

    @Mock
    private FilePredicates predicates;

    @Mock
    private FilePredicate predicate;

    private JavaResourceResolver resourceResolver = new JavaResourceResolver();

    @Test
    public void typeMatches() {
        verifySingleFileMatches("Type");
    }

    @Test
    public void fieldMatches() {
        verifySingleFileMatches("Field");
    }

    @Test
    public void methodMatches() {
        verifySingleFileMatches("Method");
    }

    @Test
    public void methodInvocationMatches() {
        verifySingleFileMatches("MethodInvocation");
    }

    @Test
    public void readFieldMatches() {
        verifySingleFileMatches("ReadField");
    }

    @Test
    public void writeFieldMatches() {
        verifySingleFileMatches("WriteField");
    }


    private void verifySingleFileMatches(String sourceType) {
        stubFileSystem();
        Path path = Paths.get("src/test/java/org/jqassistant/contrib/Test.java");
        Iterable<InputFile> it = singletonList(toInputFile(path));
        doReturn(it).when(fileSystem).inputFiles(predicate);

        InputPath result = resourceResolver.resolve(fileSystem, sourceType, "/org/jqassistant/contrib/Test.class", null);

        assertEquals(Paths.get(result.uri()), path.toAbsolutePath());
    }

    @Test
    public void multipleFileMatches() {
        stubFileSystem();
        Path path1 = Paths.get("/src/main/java/org/jqassistant/contrib/Test.java");
        Path path2 = Paths.get("/src/test/java/org/jqassistant/contrib/Test.java");
        Iterable<InputFile> it = asList(toInputFile(path1), toInputFile(path2));
        doReturn(it).when(fileSystem).inputFiles(predicate);

        InputPath result = resourceResolver.resolve(fileSystem, "Type", "/org/jqassistant/contrib/Test.class", null);

        assertNull(result);
    }

    @Test
    public void noFilesMatch() {
        stubFileSystem();
        doReturn(emptyList()).when(fileSystem).inputFiles(predicate);

        InputPath result = resourceResolver.resolve(fileSystem, "Type", "/org/jqassistant/contrib/Test.class", null);

        assertNull(result);
    }

    @Test
    public void unsupportedType() {
        InputPath result = resourceResolver.resolve(fileSystem, "Unsupported", "/org/jqassistant/contrib/Test.class", null);

        assertNull(result);
    }

    private void stubFileSystem() {
        doReturn(predicates).when(fileSystem).predicates();
        doReturn(predicate).when(predicates).matchesPathPattern(anyString());
    }

    private DefaultInputFile toInputFile(Path javaPath) {
        DefaultIndexedFile indexedFile = new DefaultIndexedFile("", javaPath, "", null);
        return new DefaultInputFile(indexedFile, null);
    }
}
