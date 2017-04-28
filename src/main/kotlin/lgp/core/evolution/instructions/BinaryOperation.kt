package lgp.core.evolution.instructions

import lgp.core.evolution.registers.Arguments

/**
 * An operation that has an arity of two (i.e. its function takes 2 arguments).
 */
abstract class BinaryOperation<T>(func: (Arguments<T>) -> T) : Operation<T>(BaseArity.Binary, func) {

    /**
     * Applies the operations function to the argument(s) given.
     *
     * If the number of arguments given does not match the operations arity,
     * then an exception will be thrown.
     *
     * @param arguments A collection of arguments (should be at most 2).
     * @throws ArityException When the number of arguments does not match the arity.
     * @returns A mapping of the arguments to some value.
     */
    override fun execute(arguments: Arguments<T>): T {
        return when {
            arguments.size() != this.arity.number -> throw ArityException("BinaryOperation takes 2 argument but was given ${arguments.size()}.")
            else -> this.func(arguments)
        }
    }
}

abstract class BranchOperation<T>(func: (Arguments<T>) -> T) : BinaryOperation<T>(func)