Modules
*******

To make the LGP system malleable to different problem domains, it is designed with the concept of modules. Modules represent a core piece of functionality that the system can use to perform the various operations it needs to.

Any part of the system that is modular can be extended and custom implementations can be provided, as long as the system is made aware of these modules in some way.

There are some restrictions on how modules can be used and what parts of the system are modules, and this document will outline those restrictions and conditions.

The modules available are listed here as a reference:

* ``ComponentLoader``
* ``Operation``
* ``Instruction``
* ``InstructionGenerator``
* ``Program``
* ``ProgramGenerator``
* ``SelectionOperator``
* ``RecombinationOperator``
* ``MutationOperator``
* ``EvolutionModel``

Component Loader
================

A *Component Loader* is a **Module**
