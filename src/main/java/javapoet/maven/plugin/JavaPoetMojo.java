package javapoet.maven.plugin;

import com.squareup.javapoet.JavaFile;
import fj.F2;
import fj.F3;
import fj.P2;
import fj.Try;
import fj.TryEffect;
import fj.Unit;
import fj.data.HashMap;
import fj.data.List;
import fj.data.NonEmptyList;
import fj.data.Option;
import fj.data.Validation;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fj.P.p;
import static fj.Semigroup.nonEmptyListSemigroup;
import static fj.data.HashMap.fromMap;
import static fj.data.List.iterableList;
import static fj.data.NonEmptyList.nel;
import static fj.data.Validation.sequence;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Invokes the given methods with the given parameters; writes the returned JavaFile Stream to the given directory.
 */
@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public final class JavaPoetMojo extends AbstractMojo
{
    private static Validation<NonEmptyList<Exception>, File> directoryFor(
            final String path)
    {
        return Try.f(() ->
        {
            final File file = Paths.get(path).toFile();

            if (file.isFile())
            {
                throw new Exception(format("The path exists and references a file '%s'.", path));
            }

            if (!file.exists() && !file.mkdirs())
            {
                throw new Exception(format("The plugin could not create the directory '%s'.", path));
            }

            return file;
        })._1().f().map(NonEmptyList::nel);
    }

    private static Validation<NonEmptyList<Exception>, List<JavaFile>> javaFileStreamListFor(
            final HashMap<String, String> classNameMethodNameParameterMap)
    {
        final String regexForIdentifier = "[\\p{Alpha}_$][\\p{Alpha}\\p{Digit}_$]*";
        final String regex = format("((%s)(\\.%s)*)\\.(%s)", regexForIdentifier, regexForIdentifier, regexForIdentifier);
        final Pattern pattern = Pattern.compile(regex);
        final int groupForClassName = 1;
        final int groupForMethodName = 4;

        final F2<Matcher, String, Validation<NonEmptyList<Exception>, P2<String, String>>> classNameMethodNameFor = (
                matcher,
                classNameMethodName) -> Option
                        .iif(matcher.matches(), () -> p(matcher.group(groupForClassName), matcher.group(groupForMethodName)))
                        .toValidation(nel(new Exception(format("The plugin could not parse the method '%s'.", classNameMethodName))));

        final F3<String, String, String, Validation<NonEmptyList<Exception>, List<JavaFile>>> javaFileListFor = (
                className,
                methodName,
                parameter) -> Try.f(() -> Class.forName(className).getMethod(methodName, String.class).invoke(null, parameter))._1()
                        .map(object -> iterableList((Iterable<JavaFile>) object))
                        .f().map(NonEmptyList::nel);

        return sequence(nonEmptyListSemigroup(), classNameMethodNameParameterMap.toList()
                .map(p -> classNameMethodNameFor.f(pattern.matcher(p._1()), p._1()).map(pInner -> pInner.append(p._2())))
                .map(validation -> validation.bind(p -> javaFileListFor.f(p._1(), p._2(), p._3()))))
                        .map(List::join);
    }

    static Validation<NonEmptyList<Exception>, List<Unit>> executeHelper(
            final String path,
            final Map<String, String> classNameMethodNameParameterMap)
    {
        requireNonNull(path);
        requireNonNull(classNameMethodNameParameterMap);

        final F2<File, JavaFile, Validation<NonEmptyList<Exception>, Unit>> writeTo = (
                file,
                javaFile) -> TryEffect.f(() -> javaFile.writeTo(file))._1()
                        .f().map(NonEmptyList::nel);

        return directoryFor(path).bind(file -> javaFileStreamListFor(fromMap(classNameMethodNameParameterMap)).bind(list -> sequence(nonEmptyListSemigroup(), list.map(javaFile -> writeTo.f(file, javaFile)))));
    }

    /**
     * A directory.
     */
    @Parameter(alias = "path", defaultValue = "${project.basedir}/src/main/java/")
    private String path;

    /**
     * Associates fully-qualified method names with string parameters.
     */
    @Parameter(alias = "methods", required = true)
    private Map<String, String> classNameMethodNameParameterMap;

    /**
     * Invokes the given methods with the given parameters; writes the returned JavaFile Stream to the given directory.
     */
    @Override
    public void execute()
    {
        executeHelper(path, classNameMethodNameParameterMap)
                .f().forEach(nel -> nel.forEach(exception -> getLog().error(exception)));
    }
}