Trainers
********

Although an evolution model can be used directly to perform evolution, generally the model will be re-used multiple times as different results are achieved with different runs due to the random nature of LGP.

Overview
========

The concept of trainers provides a common interface for creating ways to evaluate a model. A ``Trainer`` is provided with an environment and a model and implements some logic to evaluate that model depending on the situation.

Generally this will involve training a number of instances of a model and evaluating each model. This way we can gather statistics about how models are performing on average.

The ``lgp.core.evolution.Trainer`` module provides the base concept as well as trainers for various situations. Generally, a trainers main task is to train a set of models and gather results for each model evaluation so that consumers can get information about the different evaluations.

Example
-------

A typical way to use an evolutionary algorithm is to evaluate it multiple times and use that to find an *average best fitness* value to ensure that the result of a *single run* wasn't a one-off occurrence.

Evaluating the model multiple times is supported by two built-in trainers that differ in the method of evaluation - *parallel* or *sequential*. Both these implementations will train multiple instances of the model and provide training results for each.

``DistributedTrainer`` spawns a set of threads and trains the models in parallel, which results in a faster runtime as multiple runs occur at once. In contrast, ``SequentialTrainer`` trains the model in a single thread and only when one model is trained is another training session begun.

To train 10 instances of the model from the previous section in a parallel manner we can use the ``DistributedTrainer``:

.. code-block:: kotlin

    // Build a trainer that will evaluate 10 parallel instances of the model.
    val trainer = Trainers.DistributedTrainer(
        environment,
        model,
        runs = 10
    )

    // Gather the results.
    // Assuming we have a data set loader as in previous sections.
    val result = trainer.train(trainingDatasetLoader.load())

    // Output the best (effective) program for each run.
    result.evaluations.forEachIndexed { run, evaluation ->
        println("Run ${run + 1}")

        evaluation.best.effectiveInstructions.forEach(::println)

        println("\n(fitness = ${evaluation.best.fitness})")
    }

As output, we will be given the effective programs of 10 individuals which were the best as trained by each model. From this we could compute the average fitness of the best individuals trained by the model to gain a metric of how good the models solutions are on average.

API
===

See `lgp.core.evolution/Trainer. <https://jeds6391.github.io/LGP/api/html/lgp.core.evolution/-trainer/index.html>`_

