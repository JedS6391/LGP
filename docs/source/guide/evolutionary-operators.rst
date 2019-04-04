Evolutionary Operators
**********************

Overview
========

Evolutionary Operators are a primary component of the system, as they provide the means for the evolutionary process to guide its search through the search space.

There are three main search operators implemented as part of the system --- all of which are implementations of the module interface. What this means is that they particular implementation of search operator has a plug-in like interface, allowing different operators to be used where appropriate.

Implementing a custom operator is as simple as implementing the correct interface: if the operator is an extension of one of the built-in operators (as detailed below), the appropriate abstract class can be implemented. Otherwise, a custom module can be defined and used in an appropriate way as defined within the evolutionary algorithm.

Selection Operator
------------------

The ``SelectionOperator`` abstract class provides the basic outline of a selection operator as used by the system. This interface is used by the built-in evolutionary algorithms, and thus a concrete implementation of this abstract class will be able to provide custom functionality determined by the particular selection scheme to be used.

By default, the system offers implementations of tournament selection and binary tournament selection which will be suitable for a large number of cases.

Recombination Operator
----------------------

``RecombinationOperator`` abstracts away the details of an operator that is used to combine individuals. The interface is used in the same way as other operators, allowing a concrete implementation to dictate the particular behaviour employed by the operator.

By default, the system offers implementation of linear crossover as a ``RecombinationOperator`` which can be used in a range of circumstances.

Mutation Operator
-----------------

A ``MutationOperator`` provides an abstract interface for mutating a given individual from a population. The details of this are up to the implementer, as for the other evolutionary operators --- this means custom mutation operators can be implemented and used by the system where required.

LGP splits mutation operators into two categories: micro and macro mutation. Micro mutation performs mutation on the instruction level by changing properties of individual instructions such as register index, operation type, or constant value. In contrast, macro mutation operates at the program level and either adds or deletes entire instructions.

The ``MicroMutationOperator`` and ``MacroMutationOperator`` classes provide a implementations of the ``MutationOperator`` abstract class that can be used to facilitate both types of mutation. These built-in operators perform effective micro and macro mutations as given by Brameier, M. F., & Banzhaf, W. (2007) [#f1]_.

API
===

See `nz.co.jedsimson.lgp.core.evolution.operators. <https://lgp.jedsimson.co.nz/api/html/nz.co.jedsimson.lgp.core.evolution.operators/index.html>`_

.. [#f1] Brameier, M. F., & Banzhaf, W. (2007). Linear Genetic Programming. Springer Science & Business Media. https://doi.org/10.1007/978-0-387-31030-5
