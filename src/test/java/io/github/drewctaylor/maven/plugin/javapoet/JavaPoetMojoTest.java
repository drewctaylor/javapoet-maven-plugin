package io.github.drewctaylor.maven.plugin.javapoet;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Collections;

import javax.lang.model.element.Modifier;

import static io.github.drewctaylor.maven.plugin.javapoet.JavaPoetMojo.executeHelper;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JavaPoetMojoTest
{
    public static final class JavaFileFactory
    {
        private JavaFileFactory()
        {
        }

        public static Iterable<JavaFile> javaFileListWithoutParameter()
        {
            return Collections.emptyList();
        }

        public static Iterable<JavaFile> javaFileListWithParameter(
                final String name)
        {
            return singletonList(JavaFile.builder(
                    "io.github.drewctaylor.maven.plugin.javapoet",
                    TypeSpec.classBuilder(
                            name)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .build())
                    .build());
        }
    }

    @Test
    void testThrowsNullPointerException()
    {
        assertThrows(NullPointerException.class, () -> executeHelper("", null));
        assertThrows(NullPointerException.class, () -> executeHelper(null, emptyMap()));
    }

    @Test
    void testPathMustBeValid()
    {
        assertEquals(
                singletonList(Exception.class),
                executeHelper("", emptyMap()).stream().map(Object::getClass).collect(toList()));
    }

    @Test
    void testPathMustBeDirectory()
    {
        assertEquals(
                singletonList(Exception.class),
                executeHelper(getClass().getResource("JavaPoetMojoTest.class").getFile(), emptyMap()).stream().map(Object::getClass).collect(toList()));
    }

    @Test
    void testClassNameMethodNameParameterMustBeValid()
    {
        final String pathForTestClasses = getClass().getResource("./../../../").getPath();

        assertEquals(
                singletonList(Exception.class),
                executeHelper(pathForTestClasses, singletonMap("", "")).stream().map(Object::getClass).collect(toList()));

        assertEquals(
                singletonList(Exception.class),
                executeHelper(pathForTestClasses, singletonMap("ClassName", "")).stream().map(Object::getClass).collect(toList()));

        assertEquals(
                singletonList(ClassNotFoundException.class),
                executeHelper(pathForTestClasses, singletonMap("ClassName.methodName", "")).stream().map(Object::getClass).collect(toList()));

        assertEquals(
                singletonList(NoSuchMethodException.class),
                executeHelper(pathForTestClasses, singletonMap("io.github.drewctaylor.maven.plugin.javapoet.JavaPoetMojoTest$JavaFileFactory.methodName", "")).stream().map(Object::getClass).collect(toList()));

        assertEquals(
                singletonList(NoSuchMethodException.class),
                executeHelper(pathForTestClasses, singletonMap("io.github.drewctaylor.maven.plugin.javapoet.JavaPoetMojoTest$JavaFileFactory.javaFileListWithoutParameter", "")).stream().map(Object::getClass).collect(toList()));

        assertEquals(
                singletonList(InvocationTargetException.class),
                executeHelper(pathForTestClasses, singletonMap("io.github.drewctaylor.maven.plugin.javapoet.JavaPoetMojoTest$JavaFileFactory.javaFileListWithParameter", "")).stream().map(Object::getClass).collect(toList()));

        executeHelper(pathForTestClasses, singletonMap("io.github.drewctaylor.maven.plugin.javapoet.JavaPoetMojoTest$JavaFileFactory.javaFileListWithParameter", "TestClass"));

        assertTrue(Paths.get(pathForTestClasses, "io", "github", "drewctaylor", "maven", "plugin", "javapoet", "TestClass.java").toFile().isFile());
    }
}
