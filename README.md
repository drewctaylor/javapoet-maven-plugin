[![Workflow Maven Package](https://github.com/drewctaylor/javapoet-maven-plugin/workflows/workflow-maven-package/badge.svg)](https://github.com/drewctaylor/javapoet-maven-plugin/workflows/workflow-maven-package/badge.svg)
[![Code Coverage](https://codecov.io/gh/drewctaylor/javapoet-maven-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/drewctaylor/javapoet-maven-plugin)

# JavaPoet Maven Plugin

A simple maven plugin that allows you to use [JavaPoet](https://github.com/square/javapoet) to generate sources for a project.

## To Develop a JavaFile Generator

To develop a JavaFile generator,  

1) write a `public static` method 
2) that has exactly one `java.lang.String` parameter and 
3) returns `java.lang.Iterable<com.squareup.javapoet.JavaFile>`.

For example:

```java
package your.name;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

public final class JavaFileFactory1 {

    private JavaFileFactory1()
    {
    }

    public static Iterable<JavaFile> javaFileIterable(final String name)
    {
        return singletonList(JavaFile.builder(
                "your.name",
                TypeSpec.classBuilder(
                        name)
                        .addModifiers(PUBLIC, FINAL)
                        .build())
                .build());
    }
}
```

Then, package the method as a maven dependency.

## To Configure the Plugin

To configure maven to use the plugin:

1) Update your `~/.m2/settings.xml` to include your github username or github email address and your [github personal access token](https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line).

    For example:

    ```xml
    <settings>
        <servers>
            <server>
                <id>github</id>
                <username>your-github-username-or-email-address</username>
                <password>your-github-personal-access-token</password>
            </server>
        </servers>
    </settings>
    ```

2) Update your `pom.xml` to include a reference to the plugin repository.

    For example:

    ```xml
    <pluginRepositories>
        <pluginRepository>
            <id>github</id>
            <name>GitHub Packages</name>
            <url>https://maven.pkg.github.com/drewctaylor/javapoet-maven-plugin</url>
        </pluginRepository>
    </pluginRepositories>
    ```

3) Update your `pom.xml` to include the plugin. 

    You must configure the plugin to specify
    
    1) the methods to execute,
    2) the parameters to pass to the methods, and
    3) the directory for the generated `.java` files.
    
    For example:
    
    ```xml
    <plugin>
        <groupId>io.github.drewctaylor</groupId>
        <artifactId>javapoet-maven-plugin</artifactId>
        <version>1.0.3</version>
        <configuration>
            <methods>
                <your.name.JavaFileFactory1.javaFileIterable>ClassName1</your.name.JavaFileFactory1.javaFileIterable>
                <your.name.JavaFileFactory2.javaFileIterable>ClassName2</your.name.JavaFileFactory2.javaFileIterable>
            </methods>
            <path>${project.basedir}/src/main/java</path>
        </configuration>
        <dependencies>
            <dependency>
                <groupId>your-group-id</groupId>
                <artifactId>your-artifact-id</artifactId>
                <version>your-version</version>
            </dependency>
        </dependencies>
        <executions>
            <execution>
                <goals>
                    <goal>generate-sources</goal>
                </goals>
                <phase>generate-sources</phase>
            </execution>
        </executions>
    </plugin>
    ```
    
    In the above example: 
    
    The `<dependencies>` element specifies the dependencies that contain the methods that the plugin should invoke.
    
    The `<methods>` element contains two elements, `<your.name.JavaFileFactory1.javaFileIterable>` and `<your.name.JavaFileFactory2.javaFileIterable>`. The names of the elements specify the methods the plugin should invoke (method: `javaFileIterable`; class: `JavaFileFactory1`; package: `your.name`), and the content of the element indicates the value the plugin should pass to the method (`ClassName1`). 
    
    The `<path>` element specifies the directory to which the plugin should write the generated `.java` files.
