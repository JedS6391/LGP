package nz.co.jedsimson.lgp.core.program.instructions

import nz.co.jedsimson.lgp.core.environment.operations.OperationLoader
import nz.co.jedsimson.lgp.core.modules.Module
import nz.co.jedsimson.lgp.core.modules.ModuleFactory
import nz.co.jedsimson.lgp.core.program.registers.Arguments
import nz.co.jedsimson.lgp.core.program.registers.RegisterIndex

/**
 * The type of function that operations perform.
 *
 * @param T The type of the arguments
 * @param arguments A set of arguments
 * @returns A value of the same type as the arguments.
 */
typealias Function<T> = (arguments: Arguments<T>) -> T

/**
 * A primitive that encapsulates the action of executing a function on a set of arguments.
 *
 * An operation is the composition of an [Arity] and some function that it can perform on [Arguments] given to it.
 *
 * Operations are specified as [Module]s, meaning that custom operations can be implemented and use
 * when generating instructions for an individual in the population.
 *
 * It is generally expected that operations will be loaded using an [OperationLoader], but they could also be
 * resolved using a [ModuleFactory], like other modules.
 *
 * Operations should have some representation associated with them, so that the action that they convey can
 * be expressed in a textual form.
 *
 * @param TData Type of arguments that the operation operates on.
 */
abstract class Operation<TData> : Module {

    /**
     * Represents how many arguments this operations [function] takes.
     */
    abstract val arity: Arity

    /**
     * A function mapping arguments to some value (i.e. the behaviour of this operation).
     */
    abstract val function: Function<TData>

    /**
     * A way to express an operation in a textual format.
     */
    abstract val representation: String

    /**
     * Executes an operation in some way.
     *
     * A base implementation is provided which:
     *   - Validates that the correct number of arguments is provided.
     *   - Applies [function] to the given [arguments].
     *
     * This function is marked as `open` to allow extension if the base implementation does
     * not provide the behaviour necessary for other use cases.
     *
     * @param arguments A set of arguments to the function.
     * @throws ArityException When the number of arguments does not match the arity.
     * @return A concrete value of type [TData].
     */
    open fun execute(arguments: Arguments<TData> ): TData {
        return when {
            arguments.size != this.arity.number -> throw ArityException(
                "Operation expected ${this.arity.number} argument(s) but was given ${arguments.size}."
            )
            else -> this.function(arguments)
        }
    }

    /**
     * Provides a string representation of this operation.
     *
     * @param operands The registers used by the [Instruction] that this [Operation] belongs to.
     * @param destination The destination register of the [Instruction] this [Operation] belongs to.
     * @return A string representation of this operation.
     */
    abstract fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String
}
