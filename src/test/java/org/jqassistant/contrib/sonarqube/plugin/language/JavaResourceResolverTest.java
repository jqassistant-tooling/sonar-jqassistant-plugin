package org.jqassistant.contrib.sonarqube.plugin.language;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.fs.*;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JavaResourceResolverTest {

    @Mock
    private FileSystem fileSystem;

    @Mock
    private FilePredicates predicates;

    @Test
    public void type() {
        java.io.File javaFile = new File(JavaResourceResolverTest.class.getName().replace('.', '/').concat(".java"));
        DefaultIndexedFile defaultIndexedFile = new DefaultIndexedFile("", Paths.get(javaFile.getPath()), "", null);
        Iterable<InputFile> it = Collections.singletonList((InputFile) new DefaultInputFile(defaultIndexedFile, null));
        when(fileSystem.predicates()).thenReturn(predicates);
        when(fileSystem.inputFiles(Matchers.any(FilePredicate.class))).thenReturn(it);
        JavaResourceResolver resourceResolver = new JavaResourceResolver();
        InputPath result = resourceResolver.resolve(fileSystem, "Type", JavaResourceResolverTest.class.getName().replace('.', '/').concat(".class"),
            JavaResourceResolverTest.class.getName());
        assertEquals(new File(result.uri()), javaFile.getAbsoluteFile());
    }

}
