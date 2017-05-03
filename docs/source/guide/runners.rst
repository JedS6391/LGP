Runners
*******

Although an evolution model can be used directly to perform evolution, generally the model will be re-used multiple times as different results are achieved with different runs due to the random nature of LGP.

Overview
========

The concept of runners provides a common interface for creating ways to evaluate a model. A ``Runner`` is provided with an environment and a model and implements some logic to evaluate that model depending on the situation.

The ``lgp.core.evolution.Runner`` module provides the base concept as well as runners for various situations. Generally, a runners main task is to evaluate the model and build up a set of results for each model evaluation so that consumers can get information about the different evaluations.

Example
-------

A typical way to use an evolutionary algorithm is to evaluate it multiple times and use that to find an *average best fitness* value to ensure that the result of a *single run* wasn't a one-off occurrence.

Evaluating the model multiple times is supported by two built-in runners that differ in the method of evaluation - *parallel* or *sequential*.

``DistributedRunner`` spawns a set of threads and evaluates the model in parallel, which results in a faster runtime as multiple runs occur at once. In contrast, ``SequentialRunner`` evaluates the model in a single thread and only when one run is completed is another initiated.

To evaluate the model from the previous section 10 times in a parallel manner we can use the ``DistributedRunner``:

.. code-block:: kotlin

    // Build a runner that will evaluate the model 10 times in parallel.
    val runner = Runners.DistributedRunner(
        environment,
        model,
        runs = 10
    )

    // Gather the results.
    val result = runner.run()

    // Output the best (effective) program for each run.
    result.evaluations.forEachIndexed { run, evaluation ->
        println("Run ${run + 1}")

        evaluation.best.effectiveInstructions.forEach(::println)

        println("\n(fitness = ${evaluation.best.fitness})")
    }

API
===

See `lgp.core.evolution/Runner. <https://jeds6391.github.io/LGP/api/html/lgp.core.evolution/-runner/index.html>`_

