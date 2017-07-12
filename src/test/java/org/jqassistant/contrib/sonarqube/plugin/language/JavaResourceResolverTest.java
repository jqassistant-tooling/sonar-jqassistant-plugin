package org.jqassistant.contrib.sonarqube.plugin.language;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JavaResourceResolverTest {

    @Mock
    private FileSystem fileSystem;

    @Mock
    FilePredicates predicates;

    @Test
    public void type() {
        java.io.File javaFile = new File(JavaResourceResolverTest.class.getName().replace('.', '/').concat(".java"));
        Iterable<InputFile> it = Collections.singletonList((InputFile) new DefaultInputFile("", javaFile.getPath()));
        for (InputFile file : it) {
            ((DefaultInputFile) file).setModuleBaseDir(Paths.get(""));
        }
        when(fileSystem.predicates()).thenReturn(predicates);
        when(fileSystem.inputFiles(Matchers.any(FilePredicate.class))).thenReturn(it);
        JavaResourceResolver resourceResolver = new JavaResourceResolver(fileSystem);
        InputPath result = resourceResolver.resolve("Type", JavaResourceResolverTest.class.getName().replace('.', '/').concat(".class"),
                JavaResourceResolverTest.class.getName());
        assertEquals(new File(result.absolutePath()), javaFile);
    }

}
