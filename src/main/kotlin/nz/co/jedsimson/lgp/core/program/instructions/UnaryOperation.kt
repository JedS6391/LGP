package nz.co.jedsimson.lgp.core.program.instructions

import nz.co.jedsimson.lgp.core.program.registers.Arguments

/**
 * An operation that has an arity of one (i.e. its function takes 1 argument).
 */
abstract class UnaryOperation<T>(func: (Arguments<T>) -> T) : Operation<T>(BaseArity.Unary, func) {

    /**
     * Applies the operations function to the argument(s) given.
     *
     * If the number of arguments given does not match the operations arity,
     * then an exception will be thrown.
     *
     * @param arguments A collection of arguments (should be at most 1).
     * @throws ArityException When the number of arguments does not match the arity.
     * @returns A mapping of the arguments to some value.
     */
    override fun execute(arguments: Arguments<T>): T {
        return when {
            arguments.size() != this.arity.number -> throw ArityException("UnaryOperation takes 1 argument but was given ${arguments.size()}.")
            else -> this.func(arguments)
        }
    }
}