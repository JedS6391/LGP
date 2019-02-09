# LGP

> *A robust LGP implementation on the JVM using Kotlin.*

[![license][license-image]][license-url]
[![docs][docs-image]][docs-url]
[![build][travis-image]][travis-url]
[![build][maven-image]][maven-url]


## About

An implementation of [Linear Genetic Programming](https://en.wikipedia.org/wiki/Linear_genetic_programming) that follows that outlined by *Linear Genetic Programming* (Brameier, M. F. and Banzhaf, W. 2007).

The core API is implemented in [Kotlin](https://kotlinlang.org) which allows for easily interoperability with Java (and other JVM languages), while adding the benefit of modern programming language features.

To get started with how to use the system, see the [documentation](http://lgp.readthedocs.io/en/latest/).

If you find the system useful or have any queries, please feel free to:

- Create a [new issue](https://github.com/JedS6391/LGP/issues/new)
- Contact me via my [website](http://www.jedsimson.co.nz/contact)
- Send me an [email](mailto:jed.simson@gmail.com?Subject=LGP)

## Installation

A JAR containing the core API can be downloaded from the [releases](https://github.com/JedS6391/LGP/releases/download/v-1.1/LGP-core-1.1.jar) page. The command below can be used to download the JAR from a terminal so that development against the API can begin:

```
curl -L https://github.com/JedS6391/LGP/releases/download/v-1.1/LGP-core-1.1.jar > LGP.jar
```

## Usage

The system is built using Kotlin and the easiest way to use it is through the Kotlin API. Instructions for installation and usage of the Kotlin compiler, `kotlinc`, can be found for the [Command Line](https://kotlinlang.org/docs/tutorials/command-line.html) or [IntelliJ IDEA](https://kotlinlang.org/docs/tutorials/getting-started.html). 

Here, we'll focus on how to use the system through Kotlin (particularly from the command line) but documentation is provided for using the API through Java.

Assuming that `kotlinc` is installed and available at the command line, the first step is to download the core API JAR file as described in the *Installation* section.

Next, create a blank Kotlin file that will contain the problem definition --- typically this would have a filename matching that of the problem:

```
touch MyProblem.kt
```

We're not going to fully define the problem as that would be a needlessly extensive exercise, so we'll simply show how to import classes from the API and build against the imported classes.

In `MyProblem.kt`, enter the following content:

```
import lgp.core.environment.config.Configuration
import lgp.core.evolution.Description
import lgp.lib.BaseProblem
import lgp.lib.BaseProblemParameters

fun main(args: Array<String>) {
    val parameters = BaseProblemParameters(
            name = "My Problem",
            description = Description("A simple example problem definition"),
            // A problem will generally need custom configuration
            config = Config()
    )

    val problem = BaseProblem(parameters)

    println(problem.name)
    println(problem.description)
}
```

Here, we use the `BaseProblem` implementation to use a default set of parameters that we can quickly test against using a data set (which is omitted here).

To compile, we use `kotlinc`:

```
kotlinc -cp LGP.jar -no-jdk -no-stdlib MyProblem.kt
```

This will generate a class file in the directory called `MyProblemKt.class`. To interpret the class file using the Kotlin interpreter is simple:

```
kotlin -cp LGP.jar:. MyProblemKt
```

You should see the following output:

```
My Problem
Description(description=A simple example problem definition)
```

Alternatively, the same result can be achieved by setting the destination to another JAR file and executing using the Java interpreter:

```
# Compile to a JAR using kotlinc 
kotlinc -cp LGP.jar -no-jdk -no-stdlib -d MyProblem.jar MyProblem.kt

# Use the Java interpreter to execute the main function
java -cp LGP.jar:MyProblem.jar MyProblemKt
```

Please refer to the [usage guide](http://lgp.readthedocs.io/en/latest/guide/usage.html#with-java) for instructions on using the API from the context of a Java program.

[license-image]: https://img.shields.io/github/license/mashape/apistatus.svg?style=flat
[license-url]: https://github.com/JedS6391/LGP/blob/master/LICENSE
[docs-image]: https://readthedocs.org/projects/lgp/badge/?version=stable&style=flat
[docs-url]: http://lgp.readthedocs.io/en/latest/
[travis-image]: https://img.shields.io/travis/JedS6391/LGP/master.svg?style=flat
[travis-url]: https://travis-ci.org/JedS6391/LGP
[maven-image]: https://img.shields.io/maven-central/v/nz.co.jedsimson.lgp/LGP.svg?label=Maven%20Central&style=flat
[maven-url]: https://search.maven.org/search?q=g:%22nz.co.jedsimson.lgp%22%20AND%20a:%22LGP%22)
