Operations
**********

About
=====

Operations are a core concept in LGP as they define a way to map the value in a register to some other value based on the properties of that operation.

These operations change the values of the registers during the execution of an LGP program and directly influence the final result.

In this implementation, operations are a component of instructions. Instructions are core to an LGP program, as a sequence of instructions is an essential component to a program. For more information on instructions, see **TODO**.

The ``lgp.lib.operations`` module defines a set of default operations that can be used in a few common situations (see `the API documentation <https://jeds6391.github.io/LGP/api/html/lgp.lib.operations/index.html>`_ for more on the default operations), but the ``Operation`` API can be used to build custom operations suitable to a particular problem domain.

API
===

Operations are an abstract concept in the API, which are composed of an arity (i.e. how many arguments does the operation have), and a function that operates on a set of arity arguments and produces a value that has the same type as the arguments.

The ``lgp.core.evolution.instructions`` module provides types for defining operations.

Arity
-----

The ``Arity`` interface defines a way for an operation to specify how many arguments it expects. A base implementation of ``Arity`` is provided as ``BaseArity`` which provides values for unary and binary functions. To allow for further levels of arity to be handled, the ``Arity`` interface can be implemented, for example if operations that take 3 arguments were required.

.. code-block:: kotlin

    enum class CustomArity : Arity {
        Ternary {
        	// Each enum type needs to override the number property.
        	override val number: Int
        		get() = 3
        }
    }

Function
--------

The ``Function<T>`` type is simply a type alias for ``(Arguments<T>) -> T``. That is to say, a function type takes a collection of ``Argument``s of some type ``T`` and maps those arguments to a value in the domain of ``T``.

Because functions have a simple interface (they are really just a lambda function), it is straight-forward to define custom functions. The only *gotcha* with functions is that they are disjoint from the arity, so care must be taken to ensure that when executing the function at the operation level, the number of arguments is checked. This will be made clearer in the **Operation** section.

For now, lets imagine a function that takes 3 arguments (perhaps it will belong to an operation that has a ternary arity as defined in the previous section), :math:`x`, :math:`y`, :math:`z` and computes the value :math:`x + y - z`. This function is going to operate on double values to keep it simple, but any type for the function could be used.

.. code-block:: kotlin

    val func = { args: Arguments<Double> ->
    	// Here we have access to the arguments. We can just assume
    	// that 3 arguments have been given and let the consumer of
    	// this function deal with any validation logic.

    	args.get(0) + args.get(1) - args.get(2)
    }

In our case, the arguments don't have names and are simply positional but the functionality is the same.

Clearly, this function is not one that would be particularly useful, but it is meant to demonstrate how easy it is to define a custom function to be used in the context of an operation.

Operation
---------

An ``Operation<T>`` is really just a composition of an ``Arity`` and a ``Function<T>``. To provide an implementation of an operation, an arity and function must be given as constructor parameters, but additional logic is required to complete the implementation.

The ``representation`` field of an operation expects some string that describes the function, so that it can be printed. This is important when exporting an operation in the context of an instruction.

Furthermore, the function ``execute(arguments: Arguments<T> ): T`` must be overridden. This function is used to apply the operations function to the given arguments. This method is where any validation logic to ensure that the number of arguments given matches the arity should be done.







