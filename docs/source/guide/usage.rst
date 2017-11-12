Usage
*****

The system is built using Kotlin and the easiest way to use it is through the Kotlin API. Instructions for installation and usage of the Kotlin compiler, ``kotlinc``, can be found for the `Command Line <https://kotlinlang.org/docs/tutorials/command-line.html>`_ or `IntelliJ IDEA <https://kotlinlang.org/docs/tutorials/getting-started.html>`_.

Installation
============

A JAR containing the core API can be downloaded from the GitHub `releases <https://github.com/JedS6391/LGP/releases/download/v-1.0/LGP-core-1.0.jar>`_ page. The command below can be used to download the JAR from a terminal so that development against the API can begin:

.. code-block:: bash

    curl -L https://github.com/JedS6391/LGP/releases/download/v-1.0/LGP-core-1.0.jar > LGP.jar

.. note:: This command will download the most up-to-date release. For other releases, please see the `GitHub releases page <https://github.com/JedS6391/LGP/releases>`_.

With Kotlin
===========

Here, we'll focus on how to use the system through Kotlin (particularly from the command line) but documentation is provided for using the API through Java.

Assuming that ``kotlinc`` is installed and available at the command line, the first step is to download the core API JAR file as described in the *Installation* section.

Next, create a blank Kotlin file that will contain the problem definition --- typically this would have a filename matching that of the problem:

.. code-block:: bash

    touch MyProblem.kt

We're not going to fully define the problem as that would be a needlessly extensive exercise, so we'll simply show how to import classes from the API and build against the imported classes.

In ``MyProblem.kt``, enter the following content:

.. code-block:: kotlin

    import lgp.core.environment.config.Config
    import lgp.core.evolution.Description
    import lgp.lib.BaseProblem
    import lgp.lib.BaseProblemParameters

    fun main(args: Array<String>) {
        val parameters = BaseProblemParameters(
            name = "My Problem",
            description = Description(
                "A simple example problem definition"
            ),
            // A problem will generally need custom configuration
            config = Config()
        )

        val problem = BaseProblem(parameters)

        println(problem.name)
        println(problem.description)
    }

Here, we use the ``BaseProblem`` implementation to use a default set of parameters that we can quickly test against using a data set (which is omitted here).

To compile, we use ``kotlinc``:

.. code-block:: bash

    kotlinc -cp LGP.jar -no-jdk -no-stdlib MyProblem.kt

This will generate a class file in the directory called ``MyProblemKt.class``. To interpret the class file using the Kotlin interpreter is simple:

.. code-block:: bash

    kotlin -cp LGP.jar MyProblemKt

You should see the following output:

.. code-block:: text

    My Problem
    Description(description=A simple example problem definition)

Alternatively, the same result can be achieved by setting the destination to another JAR file and executing using the Java interpreter:

.. code-block:: bash

    # Compile to a JAR using kotlinc
    kotlinc -cp LGP.jar -no-jdk -no-stdlib -d MyProblem.jar MyProblem.kt

    # Use the Java interpreter to execute the main function
    java -cp LGP.jar:MyProblem.jar MyProblemKt

With Java
=========

The same functionality as above from the perspective of Java is not quite as elegant, but still fully possible. Because Java doesn't offer optional parameters, it makes the Kotlin API slightly harder to use as we have to provide values for any optional parameters.

To start, a new Java file should be created with the name of the main class as per the usual Java specification:

.. code-block:: bash

    touch MyProblem.java

Next, the file can be filled with the following:

.. code-block:: java

    import kotlin.jvm.functions.Function2;
    import lgp.core.environment.config.Config;
    import lgp.core.evolution.Description;
    import lgp.core.evolution.fitness.FitnessCase;
    import lgp.core.evolution.fitness.FitnessFunctions;
    import lgp.lib.BaseProblem;
    import lgp.lib.BaseProblemParameters;

    import java.util.Arrays;
    import java.util.List;

    public class MyProblem {

        static String name = "My Problem";
        static Description description = new Description("A simple example problem definition");
        static String configFilename = null;
        static Config config = new Config();
        static Double[] constants = { -1.0, 0.0, 1.0 };
        static String[] operationClassNames = {
            "lgp.lib.operations.Addition",
            "lgp.lib.operations.Subtraction",
            "lgp.lib.operations.Multiplication",
            "lgp.lib.operations.Division"
        };
        static double defaultRegisterValue = 1.0;
        static Function2<List<Double>, List<FitnessCase<Double>>, Double> fitnessFunction = FitnessFunctions.MSE();
        static int tournamentSize = 20;
        static int maximumSegmentLength = 6;
        static int maximumCrossoverDistance = 5;
        static int maximumSegmentLengthDifference = 3;
        static double macroMutationInsertionRate = 0.67;
        static double macroMutationDeletionRate = 0.33;
        static double microRegisterMutationRate = 0.4;
        static double microOperationMutationRate = 0.4;
        static Long randomStateSeed = null;
        static int runs = 10;

        public static void main(String[] args) {
            BaseProblemParameters parameters = new BaseProblemParameters(
                name,
                description,
                configFilename,
                config,
                Arrays.asList(constants),
                Arrays.asList(operationClassNames),
                defaultRegisterValue,
                fitnessFunction,
                tournamentSize,
                maximumSegmentLength,
                maximumCrossoverDistance,
                maximumSegmentLengthDifference,
                macroMutationInsertionRate,
                macroMutationDeletionRate,
                microRegisterMutationRate,
                microOperationMutationRate,
                randomStateSeed,
                runs
            );

            BaseProblem problem = new BaseProblem(parameters);

            System.out.println(problem.getName());
            System.out.println(problem.getDescription());
        }

    }

This set-up is the same as for the Kotlin API usage example, but is slightly more verbose due to Java's omission of optional parameters as mentioned previously.

To compile and run however, is still fairly straight-forward:

.. code-block:: bash

    # First, compile the code against the LGP API
    javac -cp LGP.jar MyProblem.java

    # Secondly, run the resulting class on the JVM
    java -cp LGP.jar:. MyProblem

If everything went as expected, then the same output should be produced as for the Kotlin example:

.. code-block:: text

    My Problem
    Description(description=A simple example problem definition)

