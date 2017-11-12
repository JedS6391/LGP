Programs
********

Overview
========

Genetic programs in the system are represented by the ``Program`` class. Programs are implemented as modules meaning that some of their logic is open to customisation.

Despite the fairly unrestricted interface of the program modules, LGP describes a particular form of program representation which this system adhere to. The invariants are that a program is comprised of a sequence of instructions and has a set of registers made available to it. The program interface also exposes a method to allow for its effective program to be found.

The ``Program`` interface is designed this way in the name of flexibility; instead of restricting the shape and operation of the component, the individual program logic can be customised to allow for situations where simply executing the instructions is not enough. For example, a control problem may involve moving a robot through an environment. In this case, instructions might be commands such as move left or rotate 90°, and the program would be responsible for gathering input data from the robot's environment and providing it to the instructions.

The built-in program implementation (``lgp.lib.BaseProgram``), provides a simple, single-output program interface. Instructions of the program are executed in sequential order and there is support for branching (i.e. conditional instructions). Furthermore, the ``findEffectiveProgram()`` method implements the intron elimination algorithm as outlined by Brameier, M. F., & Banzhaf, W. (2007) [#f1]_.

The interaction between instructions and programs can be leveraged through the modular interface to adapt the system to the needs of an individual problem.

Program Generator
=================

Program generation is facilitated by the ``ProgramGenerator`` class. Again, in the name of flexibility, this is a modular component as the generation scheme for initial programs may vary (random, effective, maximum-length, constant-length, and variable-length initialization techniques are acknowledge in the literature).

The program generator is expected to act as a stream of programs so that other components in the system can continuously generate new programs.

The built-in program generator (``lgp.lib.BaseProgramGenerator``), creates a random, endless stream of effective programs. Properties of these initial programs can be tuned through the environment.

Program Translator
==================

There are two options to facilitate the translation of genetic programs to representations which can be used outside of the context of the LGP system. Firstly, a ``Program`` implementation can directly produce an output through its ``toString()`` method.

Alternatively, the ``ProgramTranslator`` module, provides an interface for translating a program instance, that allows for custom output to be encapsulated for use in batch processing or from outside of the system.

The built in ``BaseProgram`` class has a corresponding ``BaseProgramTranslator`` which can convert the internal representation to a fully functional program in the C programming language. This translator provides two modes:

1. Main function included with model
2. Standalone function for model.

The first mode (when ``includeMainFunction == true``), will translate to a C program that includes the model as a C function, alongside a main function that parses inputs for the model from the command-line. This is intended to be used when the model is executed from the command-line and is given inputs as arguments.

Secondly --- if ``includeMainFunction`` is set to ``false`` --- the translator will exclude the main function, instead including the model as a standalone function. This mode is better suited to situations where the model is integrated into an existing code base.

The output from ``BaseProgramTranslator`` should compile under most systems using the following command (assuming the output is saved in ``model.c``):

.. code-block:: bash

    gcc -o model model.c

API
===

See `lgp.core.evolution.population <https://jeds6391.github.io/LGP/api/html/lgp.core.evolution.population/index.html>`_ for information on the ``Program`` and ``ProgramGenerator`` interfaces.

API details for the built-in implementations are found in `lgp.lib <https://jeds6391.github.io/LGP/api/html/lgp.lib/index.html>`_.

.. [#f1] Brameier, M. F., & Banzhaf, W. (2007). Linear Genetic Programming. Springer Science & Business Media. https://doi.org/10.1007/978-0-387-31030-5
