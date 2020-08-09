package io.github.drewctaylor.maven.plugin.javapoet;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import fj.Unit;
import fj.data.List;
import fj.data.NonEmptyList;
import fj.data.Validation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;

import javax.lang.model.element.Modifier;

import static fj.P.p;
import static fj.data.HashMap.arrayHashMap;
import static io.github.drewctaylor.maven.plugin.javapoet.JavaPoetMojo.executeHelper;
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

        public static java.util.List<JavaFile> javaFileListWithoutParameter()
        {
            return Collections.emptyList();
        }

        public static java.util.List<JavaFile> javaFileListWithParameter(
                final String name)
        {
            return Collections.singletonList(JavaFile.builder(
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
        assertThrows(NullPointerException.class, () -> executeHelper(null, new HashMap<>(0)));
    }

    @Test
    void testPathMustBeValid()
    {
        assertEquals(
                Validation.<Class<?>, List<Unit>>fail(Exception.class),
                executeHelper("", new HashMap<>(0)).f().map(NonEmptyList::head).f().map(Object::getClass));
    }

    @Test
    void testPathMustBeDirectory()
    {
        assertEquals(
                Validation.<Class<?>, List<Unit>>fail(Exception.class),
                executeHelper(getClass().getResource("JavaPoetMojoTest.class").getFile(), new HashMap<>(0)).f().map(NonEmptyList::head).f().map(Object::getClass));
    }

    @Test
    void testClassNameMethodNameParameterMustBeValid()
    {
        final String pathForTestClasses = getClass().getResource("./../../../").getPath();

        assertEquals(
                Validation.<Class<?>, List<Unit>>fail(Exception.class),
                executeHelper(pathForTestClasses, arrayHashMap(p("", "")).toMap()).f().map(NonEmptyList::head).f().map(Object::getClass));

        assertEquals(
                Validation.<Class<?>, List<Unit>>fail(Exception.class),
                executeHelper(pathForTestClasses, arrayHashMap(p("ClassName", "")).toMap()).f().map(NonEmptyList::head).f().map(Object::getClass));

        assertEquals(
                Validation.<Class<?>, List<Unit>>fail(ClassNotFoundException.class),
                executeHelper(pathForTestClasses, arrayHashMap(p("ClassName.methodName", "")).toMap()).f().map(NonEmptyList::head).f().map(Object::getClass));

        assertEquals(
                Validation.<Class<?>, List<Unit>>fail(NoSuchMethodException.class),
                executeHelper(pathForTestClasses, arrayHashMap(p("io.github.drewctaylor.maven.plugin.javapoet.JavaPoetMojoTest$JavaFileFactory.methodName", "")).toMap()).f().map(NonEmptyList::head).f().map(Object::getClass));

        assertEquals(
                Validation.<Class<?>, List<Unit>>fail(NoSuchMethodException.class),
                executeHelper(pathForTestClasses, arrayHashMap(p("io.github.drewctaylor.maven.plugin.javapoet.JavaPoetMojoTest$JavaFileFactory.javaFileListWithoutParameter", "")).toMap()).f().map(NonEmptyList::head).f().map(Object::getClass));

        assertEquals(
                Validation.<Class<?>, List<Unit>>fail(InvocationTargetException.class),
                executeHelper(pathForTestClasses, arrayHashMap(p("io.github.drewctaylor.maven.plugin.javapoet.JavaPoetMojoTest$JavaFileFactory.javaFileListWithParameter", "")).toMap()).f().map(NonEmptyList::head).f().map(Object::getClass));

        executeHelper(pathForTestClasses, arrayHashMap(p("io.github.drewctaylor.maven.plugin.javapoet.JavaPoetMojoTest$JavaFileFactory.javaFileListWithParameter", "TestClass")).toMap());

        assertTrue(Paths.get(pathForTestClasses, "io", "github", "drewctaylor", "maven", "plugin", "javapoet", "TestClass.java").toFile().isFile());
    }
}
