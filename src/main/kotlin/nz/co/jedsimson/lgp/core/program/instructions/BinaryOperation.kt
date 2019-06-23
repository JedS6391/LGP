package nz.co.jedsimson.lgp.core.program.instructions

import nz.co.jedsimson.lgp.core.program.registers.Arguments

/**
 * An operation that has an arity of two (i.e. its function takes 2 arguments).
 */
abstract class BinaryOperation<T>(override val function: (Arguments<T>) -> T) : Operation<T>() {

    override val arity = BaseArity.Binary

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
            else -> this.function(arguments)
        }
    }
}

abstract class BranchOperation<T>(func: (Arguments<T>) -> T) : BinaryOperation<T>(func)