Fitness
*******

Fitness evaluation is an important part of the evolutionary search process, and is the most time consuming portion of the algorithm --- requiring multiple program evaluations on a set of fitness cases. The resulting fitness evaluations help to guide the evolutionary search towards better performing solutions.

There are three components in the system that are responsible for performing and controlling fitness evaluation: (1) fitness function, (2) fitness context, and (3) fitness evaluator. The following sections will detail each component, as well as the interaction between them.

Fitness Function
================

A fitness function (``lgp.core.evolution.fitness.FitnessFunction``) is really just an ordinary function that has two arguments --- a list of program outputs and a list of fitness cases --- and produces a single real-valued output.

A fitness function is used to measure the error between the expected output values (as determined by the fitness cases) and predicted output values of the model (i.e. an LGP program). An example fitness function, mean-squared error, is given by :eq:`mse`, where :math:`Y` is a vector of :math:`n` expected outputs and :math:`\hat{Y}` is a vector of :math:`n` predicted outputs.

.. math::
    :label: mse

    MSE = \frac{1}{n} \sum_{i=1}^{n}(\hat{Y}_i - Y_i)^2

The API provides a collection of built-in functions, found in the ``lgp.core.evolution.fitness.FitnessFunctions`` object. These can be selected as construction components when building an LGP environment.

Alternatively, one can simply write a function with the signature ``(List<T>, List<FitnessCase<T>>) -> Double`` to implement a custom fitness function for the particular problem being solved. The functionality is encapsulated in the ``FitnessFunction`` abstract class.

Fitness Context
===============

A fitness context (``lgp.core.evolution.fitness.FitnessContext``) essentially maps a program to a set of input-output examples in order to produce a fitness value. This is done by executing the program on each of the input vectors and aggregating the output results. The error can then be measured using a ``FitnessFunction``.

The reason this is encapsulated in its own class is to allow for modularity. The particular ``FitnessContext`` implementation is chosen by the user, meaning that custom logic for aggregating and evaluating results can be implemented --- e.g. for multiple-output programs or weighted outputs.

By default, the API provides the ``SingleOutputFitnessContext`` which will simply gather a single program output from program's specified output register. Each output will used to evaluate the programs fitness using the specified ``FitnessFunction``.

Fitness Evaluator
=================

The fitness evaluator (``lgp.core.evolution.fitness.FitnessEvaluator``) is responsible for combining all the other pieces together. The ``FitnessEvaluator`` simply takes a program and a data set and uses a ``FitnessContext`` to evaluate the fitness. The ``FitnessEvaluator`` does not know about the details contained within the ``FitnessContext`` as that is decided by the user --- it simply adheres to the interface of the other components.

API
===

See `lgp.core.evolution.fitness. <https://jeds6391.github.io/LGP/api/html/lgp.core.evolution.fitness/index.html>`_
