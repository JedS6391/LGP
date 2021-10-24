Extensions
**********

Overview
========

As somewhat alluded to when describing the environment component of the system, the system allows custom registered components to be registered to allow for custom functionality where necessary. This is done by mapping a module implementation to a particular module type.

What wasn't mentioned is that there is no set enumeration of module types, meaning that you can create your own module type and own custom module and the system will be able to use it once registered.

This has been done to leave the system open for future developments to LGP and GP in general --- namely, the creation and integration of novel search operators or evolutionary algorithms to extend the system. Alternatively, a less complex module could be included to perform tasks such as logging of the evolutionary process or real-time aggregation of the results.

Example
-------

For the sake of completeness, we will create a custom module implementation and register it to a custom module type to show how the process works. It should be noted that this module will not be used by the system as none of the built-in components are made to use this custom module, but the principle is what is important.

The custom module we'll be creating is a logger which can be injected into the system, to be made available anywhere the environment can be queried.

To start, we create our own module type to which the module implementation will be registered:

.. code-block:: kotlin

    // Using an enum class allows for further module types to
    // be added later.
    enum class CustomModuleType : RegisteredModuleType {
        Logger
    }

Next, we need to build a module implementation which can be registered to this module type. This could be anything as long as it implements the ``Module`` interface, but for our purposes we are building a logger:

.. code-block:: kotlin

    import java.time.LocalDateTime

    enum class LoggerLevel {
        Error,
        Warning,
        Info,
        Debug
    }

    class Logger(
        val name: String,
        val level: LoggerLevel = LoggerLevel.Info
    ) : Module {

        override val information = ModuleInformation(
            description = "A custom logger that will be injected" +
                          "into the evolutionary process."
        )

        fun log(level: LoggerLevel, message: String) {
            if (level <= this.level) {
                val now = LocalDateTime.now()
                val levelName = level.name.toUpperCase()

                println("[$now] ($name-${levelName}): $message")
            }
        }
    }

Our logger offers a fairly basic set of functionalities. It can have a name and a level set and will log messages that are suitable for the level set (if the message request level falls below the logger's set level, the message will be printed).

Finally, when building an ``Environment``, we can simply register an implementation of this module with our new module type and the system will become aware of that module and how to access it. Because we want the logger to be a singleton instance (i.e. the same logger should be returned each time it is requested), we need to make sure the builder returns a specific instance:

.. code-block:: kotlin

    ...

    // Our environment - initialisation details are omitted.
    val env = Environment<Double, Outputs.Single>(
        configLoader,
        constantLoader,
        operationLoader,
        defaultValueProvider,
        fitnessFunction = { mse }
    )

    val logger = Logger(
        name = "EvolutionaryLogger",
        level = LoggerLevel.Info
    )

    // Build up a container for any modules that need to be registered.
    // This is where we'll register our custom logger module as specified above.
    val container = ModuleContainer(
        modules = mapOf(
            CoreModuleType.InstructionGenerator to
            { BaseInstructionGenerator(env) },

            CoreModuleType.ProgramGenerator to
            { BaseProgramGenerator(env) },

            // Our custom logger instance.
            CustomModuleType.Logger to
            { logger }
        )
    )

    // Inform the environment of these modules.
    env.registerModules(container)

Now the logger instance can be accessed from anywhere the environment is visible within the system. None of the built-in modules are set up to use this custom module, but they could be adapted to use this functionality --- meaning the system is extremely malleable to different extensions.

API
===

There are a few relevant APIs for creating custom modules. Firstly, the `nz.co.jedsimson.lgp.core.modules <https://lgp.jedsimson.co.nz/api/html/nz.co.jedsimson.lgp.core.modules/index.html>`_ package provides the definition of the ``Module`` interface which must be implemented in order to create custom modules.

Furthermore, to create a custom module type to allow for a custom module to be registered, the `nz.co.jedsimson.lgp.core.environment <https://lgp.jedsimson.co.nz/api/html/nz.co.jedsimson.lgp.core.environment/index.html>`_ package defines the ``RegisteredModuleType`` interface which must be implemented to create a new module type that is able to be registered within the environment.
