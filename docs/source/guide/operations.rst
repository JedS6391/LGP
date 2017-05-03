Operations
**********

Operations define a way to map the value in a register to some other value based on the properties of that operation.

These operations change the values of the registers during the execution of an LGP program and directly influence the final result.

Built-In Operations
===================

The `lgp.lib.operations <https://jeds6391.github.io/LGP/api/html/lgp.lib.operations/index.html>`_ module defines a set of built-in operations that can be used in a few common situations, but the purpose of this document is to describe how the ``Operation`` API can be used to build custom operations suitable to a particular problem domain.

Basics
======

Operations are an abstract concept in the API, which at their core are composed of an `Arity`_ and a `Function`_.

The execution of an operation means that an operations' function is applied to a set of :math:`n` arguments, where :math:`n` is equal to the operations' arity.

.. warning:: An operation should validate that the number of arguments it is given to apply its function to matches its arity.

Arity
=====

An ``Arity`` defines a way for an operation to specify how many arguments it expects.

Generally, the built-in ``BaseArity`` which provides values for unary and binary functions will be suitable to most problems, but to allow for further levels of arity to be handled, the ``Arity`` interface can be implemented.

Example
-------

It is simple to define an arity suitable for operations with three operands:

.. code-block:: kotlin

    enum class CustomArity : Arity {
        Ternary {
            // Each enum type needs to override the number property.
            override val number: Int = 3
        }
    }

Function
========

The ``Function<T>`` type is really a type alias for ``(Arguments<T>) -> T``.

That is to say, a function type takes a collection of arguments of some type ``T`` and maps those arguments to a value in the domain of ``T``.

Because functions have a simple interface (they are really just a lambda function), it is straight-forward to define custom functions. The only *gotcha* with functions is that they are disjoint from the arity, so care must be taken to ensure that when executing the function at the operation level, the number of arguments is checked. This will be made clearer in the `Operation`_ section.

Example
-------

For now, lets imagine a function that takes 3 arguments and computes the function :eq:`f`.

.. math::
    :label: f

    f(x, y, z) = x + y - z

This function is going to operate on double values to keep it simple, but the function could operate on values of any type.

To translate this into a form the LGP system understands is straight-forward:

.. code-block:: kotlin

    val ternaryFunc = { args: Arguments<Double> ->
    	// Here we have access to the arguments. We can just assume
    	// that 3 arguments have been given and let the consumer of
    	// this function deal with any validation logic.

    	args.get(0) + args.get(1) - args.get(2)
    }

In our case, the arguments don't have names and are simply positional but the functionality is the same.

Clearly, this function is not one that would be particularly useful, but it is meant to demonstrate how easy it is to define a custom function to be used in the context of an operation.

Operation
=========

As described earlier, operations are really just a composition of an ``Arity`` and a ``Function<T>``.

To provide an implementation of an operation, an arity and function must be given as constructor parameters, but additional logic is required to complete the implementation.

The ``information`` field of an operation is essentially an object that provides some information about the operation, since operations are modules in the LGP system.

The ``representation`` field of an operation expects some string that describes the function, so that it can be printed. This is important when exporting an operation in the context of an instruction.

Furthermore, the function ``execute(arguments: Arguments<T> ): T`` must be overridden. This function is used to apply the operations function to the given arguments. This method is where any validation logic to ensure that the number of arguments given matches the arity should be done.

Example
-------

Let's finish off the example by using our ternary arity and ternary function to define a ternary operation that operates on double values, and then building an instruction that uses that operation.

Starting with a class definition that provides the correct dependencies to the base ``Operation`` class:

.. code-block:: kotlin

    class TernaryOperation : Operation<Double>(
        // Our arity for operations with 3 operands
        arity = CustomArity.Ternary,

        // Our function, x + y - z
        func = ternaryFunc
    )

This means that a ``TernaryOperation`` is an ``Operation`` that applies ``ternaryFunc`` to a set of double values, with the number of elements in the set determined by ``CustomArity.Ternary``.

Next, the actual implementation of the base class:

.. code-block:: kotlin

    // Provide some description of this module.
    override val information = ModuleInformation(
        description = "An operation for performing a " +
                      "custom ternary function."
    )

    // Provide a way to represent this operation.
    // The way this representation is consumed should
    // be defined by the type of instruction that uses
    // this operation.
    // In this case we provide a simple format string.
    override val representation = "%s + %s - %s"

    // The core method that applies this operations
    // function to a set of arguments.
    override fun execute(arguments: Arguments<Double>): Double {
        return when {
            // Short-circuit
            arguments.size() != this.arity.number -> {
                throw ArityException(
                    "TernaryOperations takes 3 argument but " +
                    "was given ${arguments.size()}."
                )
            }
            else -> this.func(arguments)
        }
    }

Putting this all together gives us a custom operation that operates on 3 double values and performs the function :eq:`f`:

.. code-block:: kotlin

    class TernaryOperation : Operation<Double>(
        arity = CustomArity.Ternary,
        func = ternaryFunc
    ) {

        override val information = ModuleInformation(
            description = "An operation for performing a " +
                          "custom ternary function."
        )

        override val representation = "%s + %s - %s"

        override fun execute(arguments: Arguments<Double>): Double {
            return when {
                arguments.size() != this.arity.number -> {
                    throw ArityException(
                        "TernaryOperations takes 3 argument but " +
                        "was given ${arguments.size()}."
                    )
                }
                else -> this.func(arguments)
            }
        }
    }

API
===

See `lgp.core.evolution.instructions. <https://jeds6391.github.io/LGP/api/html/lgp.core.evolution.instructions/index.html>`_





