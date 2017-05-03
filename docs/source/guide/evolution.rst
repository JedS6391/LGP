Evolution Models
****************

Once an environment has been defined for the problem to be solved using LGP, the next step is to build an evolutionary model using that environment.

Overview
========

An ``EvolutionModel`` describes the way that the modules provided to the environment are used to evolve a *solution* to the problem.

All this really means is that an evolution model is an implementation of some algorithm that is used for evolving solutions, such as a steady-state or generational algorithm.

The system allows custom models to be created so that the model can be adapted to the problem being solved, but in general the models provided by the `lgp.core.evolution.population.Models` module will provide good performance, as the parameters can be tweaked through the environment that is built.

To use a model is as simple as providing the model with an environment and starting the process of evolution using that model. A model should return some result when it is completed, which provides information about the best individual found, the final population, and any statistics from evolution that the model wishes to expose.

.. note:: It must be ensured that the environment built provides all the components that the evolution model requires. Because the model has complete access to the environment, it can make use of any component the environment is aware of.

Example
-------

An environment provides a context for evolution, and we can build a model within that environment easily:

.. code-block:: kotlin

    // The environment from the previous section
    // without any of the registered dependencies.
    val env = Environment<Double>(
            configLoader,
            constantLoader,
            datasetLoader,
            operationLoader,
            defaultValueProvider,
            fitnessFunction = ce
    )

    // Register the modules that are needed to use
    // the model we wish to use (i.e. every core module type).
    val container = ModuleContainer(
        modules = mutableMapOf(
            CoreModuleType.InstructionGenerator to {
                BaseInstructionGenerator(environment)
            },
            CoreModuleType.ProgramGenerator to {
                BaseProgramGenerator(
                    environment,
                    sentinelTrueValue = 1.0
                )
            },
            CoreModuleType.SelectionOperator to {
                TournamentSelection(
                    environment,
                    tournamentSize = 2
                )
            },
            CoreModuleType.RecombinationOperator to {
                LinearCrossover(
                    environment,
                    maximumSegmentLength = 6,
                    maximumCrossoverDistance = 5,
                    maximumSegmentLengthDifference = 3
                )
            },
            CoreModuleType.MacroMutationOperator to {
                MacroMutationOperator(
                    environment,
                    insertionRate = 0.67,
                    deletionRate = 0.33
                )
            },
            CoreModuleType.MicroMutationOperator to {
                MicroMutationOperator(
                    environment,
                    registerMutationRate = 0.5,
                    operatorMutationRate = 0.3,
                    constantMutationFunc = { v ->
                        v + (Random().nextGaussian())
                    }
                )
            }
        )
    )

    environment.registerModules(container)

    // Build a steady-state model around this environment.
    val model = Models.SteadyState(environment)

    // Use the model to perform evolution and collect the result.
    val result = model.evolve()

    println(result.best.fitness)

API
===

See `lgp.core.evolution.population/EvolutionModel. <https://jeds6391.github.io/LGP/api/html/lgp.core.evolution.population/-evolution-model/index.html>`_
