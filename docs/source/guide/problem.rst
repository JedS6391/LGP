Problems and Solutions
**********************

The combination of an environment and evolution model provides everything necessary to start searching for solutions for a problem.

However, in the name of clean code the system provides a few mechanisms which can be used to make the task of defining the problem easier and clearer.

Overview
========

Problem
-------

A ``Problem`` encapsulates the details of a problem and the components that can be used to find solutions for it.

At the highest level a problem is a wrapper that has a set of data attributes (name, description, training/testing datasets), and a collection of dependencies.

The general operation of a problem is to define the components needed by filling in the problem skeleton:

- Name
- Description
- Configuration Loader
- Constant Loader
- Operation Loader
- Default Value Provider
- Fitness Function
- Registered Modules

It may be noticed that these are essentially the components required to build an ``Environment``. This is no coincidence, as the next step of filling out a problem's skeleton is to implement a method to initialise an environment for that problem --- provided by the ``initialiseEnvironment()`` method.

Following that, a model for the problem should be defined and initialised using the ``initialiseModel()`` method. This method should build an EA around the environment initialised previously.

Finally, a method for solving the problem can be defined. This functionality can use the environment and model of the problem to search for solutions. The return value of the method is left open so that the solution can be adapted for the problem as necessary. The ``solve()`` method contains the skeleton necessary for implementing this final part of the problem. For example, one could save results directly to a file or perform analysis of the results to produce plots.

.. note:: It must be ensured that the environment built provides all the components that the evolution model requires. Because the model has complete access to the environment, it can make use of any component the environment is aware of.

Solution
--------

A ``Solution`` to a problem is left as open as possible to allow for arbitrarily complex solutions. In general, a solution will contain the result of a prediction using the model trained for the problem, but there are situations where it makes sense to return multiple predictions or statistics.

Example
-------

The ``lgp.examples`` package provides examples of how to define a problem and how solutions to that problem can be obtained. The package is available for viewing on `GitHub <https://github.com/JedS6391/LGP/tree/master/src/main/kotlin/lgp/examples>`_.

API
===

See `nz.co.jedsimson.lgp.core.evolution <https://lgp.jedsimson.co.nz/api/html/nz.co.jedsimson.lgp.core.evolution/index.html>`_
