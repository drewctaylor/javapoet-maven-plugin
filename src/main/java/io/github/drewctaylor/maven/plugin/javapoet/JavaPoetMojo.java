package io.github.drewctaylor.maven.plugin.javapoet;

import com.squareup.javapoet.JavaFile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static java.util.stream.StreamSupport.stream;

/**
 * Invoke the given methods with the given string parameters; write the returned JavaFile Stream to the given directory.
 */
@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public final class JavaPoetMojo extends AbstractMojo
{
    private static <T> BiFunction<Function<List<Exception>, T>, BiFunction<String, String, T>, T> classNameMethodNameFor(
            final Matcher matcher,
            final String classNameMethodName)
    {
        final int groupForClassName = 1;
        final int groupForMethodName = 4;

        return matcher.matches() ?
                (
                        fExceptionList,
                        fClassNameMethodName) -> fClassNameMethodName.apply(matcher.group(groupForClassName), matcher.group(groupForMethodName)) :
                (
                        fExceptionList,
                        fClassNameMethodName) -> fExceptionList.apply(singletonList(new Exception(format("The plugin could not parse the method '%s'.", classNameMethodName))));
    }

    private static <T> BiFunction<Function<List<Exception>, T>, Function<List<JavaFile>, T>, T> javaFileList(
            final String className,
            final String methodName,
            final String parameter)
    {
        try
        {
            final List<JavaFile> javaFileList = stream(((Iterable<JavaFile>) Class.forName(className).getMethod(methodName, String.class).invoke(null, parameter)).spliterator(), false).collect(toList());
            return (
                    fExceptionList,
                    fJavaFileIterable) -> fJavaFileIterable.apply(javaFileList);
        }
        catch (final ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception)
        {
            return (
                    fExceptionList,
                    fJavaFileList) -> fExceptionList.apply(singletonList(exception));
        }
    }

    private static <T> BiFunction<Function<List<Exception>, T>, Function<File, T>, T> directoryFor(
            final String path)
    {
        final File file = Paths.get(path).toFile();

        return file.isFile() ?
                (
                        fExceptionList,
                        fFile) -> fExceptionList.apply(singletonList(new Exception(format("The path exists and references a file '%s'.", path)))) :
                !file.exists() && !file.mkdirs() ?
                        (
                                fExceptionList,
                                fFile) -> fExceptionList.apply(singletonList(new Exception(format("The plugin could not create the directory '%s'.", path)))) :
                        (
                                fExceptionList,
                                fFile) -> fFile.apply(file);
    }

    private static <T> BiFunction<Function<List<Exception>, T>, Function<List<JavaFile>, T>, T> javaFileStreamListFor(
            final Map<String, String> classNameMethodNameParameterMap)
    {
        final String regexForIdentifier = "[\\p{Alpha}_$][\\p{Alpha}\\p{Digit}_$]*";
        final String regex = format("((%s)(\\.%s)*)\\.(%s)", regexForIdentifier, regexForIdentifier, regexForIdentifier);
        final Pattern pattern = Pattern.compile(regex);

        return classNameMethodNameParameterMap.keySet().stream()
                .map(classNameMethodName -> JavaPoetMojo.<BiFunction<Function<List<Exception>, T>, Function<List<JavaFile>, T>, T>>classNameMethodNameFor(pattern.matcher(classNameMethodName), classNameMethodName).apply(
                        exceptionList -> (
                                fExceptionList,
                                fJavaFileList) -> fExceptionList.apply(exceptionList),
                        (
                                className,
                                methodName) -> JavaPoetMojo.<BiFunction<Function<List<Exception>, T>, Function<List<JavaFile>, T>, T>>javaFileList(className, methodName, classNameMethodNameParameterMap.get(classNameMethodName)).apply(
                                        exceptionList -> (
                                                fExceptionList,
                                                fJavaFileList) -> fExceptionList.apply(exceptionList),
                                        javaFileList -> (
                                                fExceptionList,
                                                fJavaFileList) -> fJavaFileList.apply(javaFileList))))
                .reduce(
                        (
                                fExceptionList,
                                fJavaFileList) -> fJavaFileList.apply(emptyList()),
                        (
                                validation1,
                                validation2) -> (
                                        fExceptionList,
                                        fJavaFileList) -> validation1.apply(
                                                exceptionList -> validation2.apply(
                                                        exceptionListInner -> fExceptionList.apply(concat(exceptionList.stream(), exceptionListInner.stream()).collect(toList())),
                                                        javaFileListInner -> fExceptionList.apply(exceptionList)),
                                                javaFileList -> validation2.apply(
                                                        exceptionListInner -> fExceptionList.apply(exceptionListInner),
                                                        javaFileListInner -> fJavaFileList.apply(concat(javaFileList.stream(), javaFileListInner.stream()).collect(toList())))));
    }

    private static Stream<Exception> writeTo(
            final File file,
            final JavaFile javaFile)
    {
        try
        {
            javaFile.writeTo(file);
            return empty();
        }
        catch (final IOException ioException)
        {
            return of(ioException);
        }
    }

    static List<Exception> executeHelper(
            final String path,
            final Map<String, String> classNameMethodNameParameterMap)
    {
        requireNonNull(path);
        requireNonNull(classNameMethodNameParameterMap);

        return JavaPoetMojo.<List<Exception>>directoryFor(path).apply(
                identity(),
                directory -> JavaPoetMojo.<List<Exception>>javaFileStreamListFor(classNameMethodNameParameterMap).apply(
                        identity(),
                        javaFileList -> javaFileList.stream().flatMap(javaFile -> writeTo(directory, javaFile)).collect(toList())));
    }

    /**
     * A path to which to write a java file.
     */
    @Parameter(alias = "path", defaultValue = "${project.basedir}/src/main/java/")
    private String path;

    /**
     * A map from a fully-qualified method name to a string parameter.
     */
    @Parameter(alias = "methods", required = true)
    private Map<String, String> methods;

    /**
     * Invoke a method with a string parameter; write a returned JavaFile Stream to the given directory.
     */
    @Override
    public void execute()
    {
        executeHelper(path, methods).forEach(exception -> getLog().error(exception));
    }
}