Environment
***********

.. note:: The examples in this document are expressed using the Kotlin language, but translation to Java is trivial.

The first step in using the LGP system is to build an environment for the problem being solved. This environment acts as a central repository for core components of the LGP system, such as configuration information, datasets, modules for various operations performed during evolution, etc. It can be thought of as the context in which the LGP system is being used as the environment used will directly influence the results.

The components needed to build an environment are split into three main categories:

1. *Construction Components*
2. *Initialisation Components*
3. *Registered Components*

The order of the components is important, as the environment needs certain components in order to be built, but other components depend on the environment. The order of components will be discussed further in the following sections.

Construction Components
=======================

These components are required when building an ``Environment`` instance and should be passed to the constructor (hence construction components). These components form the base information required to resolve further components.

To build an environment, the following construction components are required:

* ``ConfigLoader``
* ``ConstantLoader``
* ``DatasetLoader``
* ``OperationLoader``
* ``DefaultValueProvider``
* ``FitnessFunction``

To find further information about these components, see `the API documentation. <https://jeds6391.github.io/LGP/>`_

These components are primarily those related to loading information into the environment (at initialisation time), or functionality that is used throughout the system but is customisable.

For example, to build up an ``Environment`` instance with the correct construction components.

.. note:: The type for the various loaders is specified explicitly in the example, but generally the type will be inferred from the arguments when using the Kotlin API. This examples uses the `Double` type, meaning that programs generated will operate on registers containing Double-precision floating-point format numbers.

.. code-block:: kotlin

    // Configuration.
    val configLoader = JsonConfigLoader(
        // This loader loads configuration information from a JSON file.
        filename = "/path/to/some/configuration/file.json"
    )

    // Load the configuration so we can use information from it.
    val config = configLoader.load()

    // Constants.
    val constantLoader = GenericConstantLoader<Double>(
        // Load constants from the configuration file
        // (but they could come from anywhere).
        constants = config.constants,

        // We parse the strings in the configuration file as doubles.
        parseFunction = String::toDouble
    )

    // Dataset.
    val datasetLoader = CsvDatasetLoader<Double>(
        // Using a CSV file so we use a CsvDatasetLoader.
        filename = "/path/to/some/data/file.csv",

        // A function to parse attributes from the dataset file.
        parseFunction = String::toDouble
    )

    // Operations.
    val operationLoader = DefaultOperationLoader<Double>(
        // We're using the operations specified in the
        // configuration file.
        operationNames = config.operations
    )

    // Default register value provider.
    // Constant value provider always returns the value given
    // when it is initialised.
    val defaultValueProvider = DefaultValueProviders.constantValueProvider<Double>(1.0)

    // Fitness function. We'll use the classification error
    // implementation from the fitness functions module.
    val ce = FitnessFunctions.CE({ o ->
        // Map output to class by rounding down to nearest value
       Math.floor(o)
    })


    // We've declared all our dependencies, so we can build an LGP
    // environment. When constructing an environment, any
    // initialisation components will be resolved.
    val env = Environment<Double>(
            configLoader,
            constantLoader,
            datasetLoader,
            operationLoader,
            defaultValueProvider,
            fitnessFunction = ce
    )

Initialisation Components
=========================

These components are automatically initialised by an environment when a set of suitable construction components have been given. These components are generally associated with a loader and are a sort of global state that isn't affected by the LGP system, for example:

- Configuration
- Constants
- Dataset
- Operations
- Register Set

The Register Set is slightly different in that it depends on information provided by the construction dependencies and is initialised internally as a global reference register set, so that programs can acquire a fresh register set at any time.

Registered Components
=====================

Registered components are essentially those that are circular in their dependency graph. That is, a registered component requires a reference to the environment in order to operate, but the environment also needs a reference to the component itself so that it can be accessed within the context of the LGP system, hence these components have to be resolved after the environment has been built.

Generally, registered dependencies will be custom implementations of core components used during the evolution process, such as custom generation schemes for instructions and programs. The reason these components generally have a dependency on the environment is that they are designed to be as flexible as possible, therefore allowing custom components to have access to the entire environment is useful.

To illustrate how registered components are used - continuing from the above example.

.. code-block:: kotlin

    ...

    // Our environment.
    val env = Environment<Double>(
            configLoader,
            constantLoader,
            datasetLoader,
            operationLoader,
            defaultValueProvider,
            fitnessFunction
    )

    // Now that we have an environment with resolved construction
    // and initialisation dependencies, we can resolve the
    // registered dependencies.

    // Build up a container for any modules that need to be registered.
    // The container acts as a way for the environment to resolve
    // dependencies. There should be an appropriate builder function
    // for each RegisteredModuleType value
    val container = ModuleContainer(
        modules = mapOf(
            RegisteredModuleType.InstructionGenerator to
            { BaseInstructionGenerator(env) },

            RegisteredModuleType.ProgramGenerator to
            { BaseProgramGenerator(env) }
        )
    )

    // Inform the environment of these modules
    env.registerModules(container)


With all components resolved, the environment is ready to be used for the main process of evolution.
