Evolution Models
****************

Once an environment has been defined for the problem to be solved using LGP, the next step is to build an evolutionary model using that environment.

Overview
========

An ``EvolutionModel`` describes the way that the modules provided to the environment are used to evolve a *solution* to the problem.

All this really means is that an evolution model is an implementation of some algorithm that is used for evolving solutions, such as a steady-state or generational algorithm.

The system allows custom models to be created so that the model can be adapted to the problem being solved, but in general the models provided by the ``lgp.core.evolution.population.Models`` module will provide good performance, as the parameters can be tweaked through the environment that is built.

To use a model is as simple as providing the model with an environment and training the model on a data set for the particular problem being solved. Once the model has been trained, it can be tested on an arbitrary data set in order to form a prediction.

.. note:: It must be ensured that the environment built provides all the components that the evolution model requires. Because the model has complete access to the environment, it can make use of any component the environment is aware of.

Generally, the model will work by using an evolutionary algorithm to train a population of individuals. The best individual from training will be used by the model to form predictions when testing using the model.

It is possible to use a model directly to solve a problem, but in general it is better to define a ``Problem`` as described in the :doc:`next section<problem>`.

Example
-------

An environment provides a context for evolution, and we can build a model within that environment easily:

.. code-block:: kotlin

    // The environment from the previous section
    // without any of the registered dependencies.
    val env = Environment<Double>(
            configLoader,
            constantLoader,
            operationLoader,
            defaultValueProvider,
            fitnessFunction = ce
    )

    // Register the modules that are needed to use
    // the model we wish to use (i.e. every core module type).
    val container = ModuleContainer(
        modules = mutableMapOf(
            // Any modules the system needs.
            ...
        )
    )

    environment.registerModules(container)

    // Build a steady-state model around this environment.
    val model = Models.SteadyState(environment)

    // Define a data set to be used for training. What this data
    // set contains will depend on the problem.
    val trainingDatasetLoader = DatasetLoader<Double> {
        // Could be loaded from a file or built directly.
        ...
    }

    // Train the model on the training data set.
    val result = model.train(trainingDatasetLoader.load())

    // Get the fitness of the best individual from training.
    println(result.best.fitness)

    // To perform a prediction using the trained model is easy.

    // Define a data set to be used for testing. This data set
    // will generally be different to that used for training in order
    // to evaluate the solutions generalisation.
    val testDatasetLoader = DatasetLoader<Double> {
        ...
    }

    // Gather the models predictions for this data set.
    val predictions = model.test(testDatasetLoader.load())

API
===

See `lgp.core.evolution.population/EvolutionModel. <https://jeds6391.github.io/LGP/api/html/lgp.core.evolution.population/-evolution-model/index.html>`_
