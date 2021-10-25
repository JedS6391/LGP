# LGP

> *A robust LGP implementation on the JVM using Kotlin.*

[![license][license-image]][license-url]
[![docs][docs-image]][docs-url]
[![build][build-image]][build-url]
[![code-coverage][code-coverage-image]][code-coverage-url]
[![maven-core][maven-image-core]][maven-url-core]
[![maven-lib][maven-image-lib]][maven-url-lib]
[![DOI][doi-image]][doi-url]

## About

An implementation of [Linear Genetic Programming](https://en.wikipedia.org/wiki/Linear_genetic_programming) that follows that outlined by *Linear Genetic Programming* (Brameier, M. F. and Banzhaf, W. 2007).

The framework is implemented in [Kotlin](https://kotlinlang.org) which allows for easily interoperability with Java (and other JVM languages), while adding the benefit of modern programming language features.

To get started with how to use the framework, see the [documentation](http://lgp.readthedocs.io/en/latest/).

If you find the framework useful or have any queries, please feel free to:

- Create a [new issue](https://github.com/JedS6391/LGP/issues/new)
- Contact me via my [website](http://www.jedsimson.co.nz/contact)
- Send me an [email](mailto:jed.simson@gmail.com?Subject=LGP)

## Installation

<small>*Note: The LGP framework requires JDK 8 (Java 1.8).*</small>

A JAR containing the core API can be downloaded from the [releases](https://github.com/JedS6391/LGP/releases/) page. Each version will have its artefact uploaded here.

Alternatively, the package is available on Maven central, so you can reference the package as a dependency using the format appropriate for your package manager (see [here](https://search.maven.org/artifact/nz.co.jedsimson.lgp/LGP) for a full list). For example, to add to an existing Gradle build script:

```gradle
repositories {
    mavenCentral()
}

dependencies {
    compile "nz.co.jedsimson.lgp:LGP:<VERSION>"
    // To get the full source, include the sources package
    compile "nz.co.jedsimson.lgp:LGP:<VERSION>:sources"
}
```

## Tests

The test suite for the framework can be run with the following gradle command:

```bash
./gradlew test --info --rerun-tasks
```

## Usage

### Examples

A set of example usages can be found in the [LGP-examples](https://github.com/JedS6391/LGP-examples) repository. The examples cover a few different problem configurations, including:

- Programs with a single or multiple outputs
- Reading dataset from a file
- Generating a dataset
- Custom fitness functions
- Usage from Java

### Getting started

The framework is built using Kotlin and the easiest way to use it is through the Kotlin API. Instructions for installation and usage of the Kotlin compiler, `kotlinc`, can be found for the [Command Line](https://kotlinlang.org/docs/tutorials/command-line.html) or [IntelliJ IDEA](https://kotlinlang.org/docs/tutorials/getting-started.html). 

Here, we'll focus on how to use the framework through Kotlin (particularly from the command line) but documentation is provided for using the API through Java. This guide assumes you want to directly use the JAR file and not through another build system.

Assuming that `kotlinc` is installed and available at the command line, the first step is to download the core API JAR file as described in the *Installation* section. You will also want to download the [LGP-lib](https://github.com/JedS6391/LGP-lib/releases) package which provides implementations of core components, particularly `BaseProblem` which we will use in this example.

Next, create a blank Kotlin file that will contain the problem definition --- typically this would have a filename matching that of the problem:

```bash
touch MyProblem.kt
```

We're not going to fully define the problem as that would be a needlessly extensive exercise, so we'll simply show how to import classes from the API and build against the imported classes.

In `MyProblem.kt`, enter the following content:

```kotlin
import nz.co.jedsimson.lgp.core.environment.config.Configuration
import nz.co.jedsimson.lgp.core.evolution.Description
import nz.co.jedsimson.lgp.lib.base.BaseProblem
import nz.co.jedsimson.lgp.lib.base.BaseProblemParameters

fun main(args: Array<String>) {
    val parameters = BaseProblemParameters(
        name = "My Problem",
        description = Description("A simple example problem definition"),
        config = Configuration()
    )

    val problem = BaseProblem(parameters)

    println(problem.name)
    println(problem.description)
}
```

Here, we use the `BaseProblem` implementation to use a default set of parameters that we can quickly test against using a data set (which is omitted here).

To compile, we use `kotlinc`:

```bash
kotlinc -cp LGP-core.jar:LGP-lib.jar -no-jdk -no-stdlib MyProblem.kt
```

This will generate a class file in the directory called `MyProblemKt.class`. To interpret the class file using the Kotlin interpreter is simple:

```bash
kotlin -cp LGP-core.jar:LGP-lib.jar:. MyProblemKt
```

You should see the following output:

```text
My Problem
Description(description=A simple example problem definition)
```

Please refer to the [usage guide](http://lgp.readthedocs.io/en/latest/guide/usage.html#with-java) for instructions on using the API from the context of a Java program.

[license-image]: https://img.shields.io/github/license/mashape/apistatus.svg?style=flat
[license-url]: https://github.com/JedS6391/LGP/blob/master/LICENSE
[docs-image]: https://readthedocs.org/projects/lgp/badge/?version=stable&style=flat
[docs-url]: http://lgp.readthedocs.io/en/latest/
[build-image]: https://img.shields.io/github/workflow/status/JedS6391/LGP/Release
[build-url]: https://github.com/JedS6391/LGP/actions/workflows/release.yml
[maven-image-core]: https://img.shields.io/maven-central/v/nz.co.jedsimson.lgp/LGP.svg?label=core&style=flat
[maven-url-core]: https://search.maven.org/search?q=g:%22nz.co.jedsimson.lgp%22%20AND%20a:%22LGP%22
[maven-image-lib]: https://img.shields.io/maven-central/v/nz.co.jedsimson.lgp/LGP-lib.svg?label=lib&style=flat
[maven-url-lib]: https://search.maven.org/search?q=g:%22nz.co.jedsimson.lgp%22%20AND%20a:%22LGP-lib%22
[code-coverage-image]:https://img.shields.io/codecov/c/github/JedS6391/LGP.svg
[code-coverage-url]:https://codecov.io/gh/JedS6391/LGP/branch/develop/
[doi-image]:https://joss.theoj.org/papers/10.21105/joss.01337/status.svg
[doi-url]:https://doi.org/10.21105/joss.01337

