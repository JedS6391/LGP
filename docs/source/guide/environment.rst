Environment
***********

The first step in using the LGP system is to build an environment for the problem being solved.

Overview
========

This environment acts as a central repository for core *components* of the LGP system, such as configuration information, modules for various operations performed during evolution, etc.

It can be thought of as the *context* in which the LGP system is being used, as the environment used will directly influence the results.

The components needed to build an environment are split into three main categories:

1. *Construction Components*
2. *Initialisation Components*
3. *Registered Components*

The order the components are enumerated in *is* important, as the environment needs certain components in order to be built, whereas other components depend on the environment being constructed first. The order of components will be discussed further in the following sections.

Construction Components
=======================

These components are required when building an ``Environment`` instance and should be passed to the constructor (hence construction components). These components form the base information required to resolve any further components.

To build an environment, the following construction components are required:

* ``ConfigLoader``
* ``ConstantLoader``
* ``OperationLoader``
* ``DefaultValueProvider``
* ``FitnessFunction``

.. note:: To find further information about these components, see `the API documentation. <https://jeds6391.github.io/LGP/api/html/lgp.core.environment/index.html>`_

These components are primarily those related to loading information into the environment (at initialisation time), or functionality that is used throughout the system but is customisable.

Example
-------

Building up an ``Environment`` instance with the correct construction components is the main initiation step to getting started using the LGP system, and as such requires a bit of dependency gathering.

.. note:: The type for the various loaders is specified explicitly in the example, but generally the type will be inferred from the arguments when using the Kotlin API. This examples uses the `Double` type, meaning that programs generated will operate on registers containing Double-precision floating-point format numbers.

.. code-block:: kotlin

    // Configuration.
    // Here, we load configuration information from a JSON file.
    val configLoader = JsonConfigLoader(
        filename = "/path/to/some/configuration/file.json"
    )

    // Pre-load the configuration so we can use information from it.
    val config = configLoader.load()

    // Constants.
    // Load constants from the configuration file
    // (although they could come from anywhere).
    val constantLoader = GenericConstantLoader<Double>(
        constants = config.constants,
        // Parse the strings in the configuration file as doubles.
        parseFunction = String::toDouble
    )

    // Operations.
    // We're using the operations specified in the config file.
    val operationLoader = DefaultOperationLoader<Double>(
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
            operationLoader,
            defaultValueProvider,
            fitnessFunction = ce
    )

This will create an environment with the construction components given and begin the process of loading any initialisation components.

Initialisation Components
=========================

These components are automatically loaded by an environment when a set of suitable construction components have been given. The components are generally associated with a ``ComponentLoader`` and are a sort of *global state* that isn't affected by the LGP system, for example:

- Configuration
- Constants
- Operations
- Register Set

The Register Set is slightly different in that it depends on information provided by the construction dependencies and is initialised internally as a *global reference* register set, so that programs can acquire a fresh register set at any time.

Nothing special needs to be done for initialisation components --- provided that the construction components given were valid, the components will be automatically loaded as appropriate and operate behind-the-scenes.

Registered Components
=====================

Registered components are essentially those that have a circular dependency graph.

That is, a registered component requires a reference to the environment in order to operate, but the environment also needs a reference to the component itself so that it can be accessed within the context of the LGP system --- hence these components have to be resolved after the environment has been built.

Generally, registered dependencies will be custom implementations of core components used during the evolution process, such as custom generation schemes for instructions and programs, or custom search operators.

The reason these components generally have a dependency on the environment is that they are designed to be as flexible as possible, and thus enabling custom components access to the entire environment is useful.

When registering these components, it is done by associating a module type (i.e. the type of component) with a builder for that module. A builder is really just a function that can build a new instance of that module.

Example
-------

To illustrate how registered components are used --- continuing from the above example.

.. code-block:: kotlin

    ...

    // Our environment.
    val env = Environment<Double>(
            configLoader,
            constantLoader,
            operationLoader,
            defaultValueProvider,
            fitnessFunction = ce
    )

    // Now that we have an environment with resolved construction
    // and initialisation dependencies, we can resolve the
    // registered dependencies.

    // Build up a container for any modules that need to be registered.
    // The container acts as a way for the environment to resolve
    // dependencies in bulk.
    val container = ModuleContainer(
        modules = mapOf(
            CoreModuleType.InstructionGenerator to
            { BaseInstructionGenerator(env) },

            CoreModuleType.ProgramGenerator to
            { BaseProgramGenerator(env) },

            // More module registrations as necessary
            ...
        )
    )

    // Inform the environment of these modules.
    env.registerModules(container)

    // Alternatively, we can register modules one-by-one.
    environment.registerModule(
        CoreModuleType.SelectionOperator,
        { TournamentSelection(environment, tournamentSize = 2) }
    )


With all components resolved, the environment is ready to be used for the main process of evolution: execution of the evolutionary algorithm.

.. note::

    It is only necessary to provide a builder for modules types that are guaranteed to be requested from the environment (i.e. they are a dependency)

    If the environment is being used by some custom consumer, then it is permitted to only provide builders for module types that it will request.

    If a module is requested that hasn't been registered with a builder then an exception detailing the missing module will be thrown.

API
===

See `lgp.core.environment. <https://jeds6391.github.io/LGP/api/html/lgp.core.environment/index.html>`_


