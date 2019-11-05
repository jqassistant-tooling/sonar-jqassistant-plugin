package org.jqassistant.contrib.sonarqube.plugin.language;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.batch.fs.*;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import java.io.File;
import java.nio.file.Paths;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
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

    @Test
    public void type() {
        File javaFile = new File(JavaResourceResolverTest.class.getName().replace('.', '/').concat(".java"));
        DefaultIndexedFile defaultIndexedFile = new DefaultIndexedFile("", Paths.get(javaFile.getPath()), "", null);
        Iterable<InputFile> it = singletonList(new DefaultInputFile(defaultIndexedFile, null));
        doReturn(predicates).when(fileSystem).predicates();
        doReturn(predicate).when(predicates).matchesPathPattern(anyString());
        doReturn(it).when(fileSystem).inputFiles(predicate);
        JavaResourceResolver resourceResolver = new JavaResourceResolver();

        InputPath result = resourceResolver.resolve(fileSystem, "Type", JavaResourceResolverTest.class.getName().replace('.', '/').concat(".class"),
            JavaResourceResolverTest.class.getName());

        assertEquals(new File(result.uri()), javaFile.getAbsoluteFile());
    }

}
