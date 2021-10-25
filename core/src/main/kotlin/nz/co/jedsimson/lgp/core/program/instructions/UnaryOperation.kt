package nz.co.jedsimson.lgp.core.program.instructions

import nz.co.jedsimson.lgp.core.program.registers.Arguments

/**
 * An operation that has an arity of one (i.e. its function takes 1 argument).
 */
abstract class UnaryOperation<T>(
    override val function: (Arguments<T>) -> T,
    override val arity: BaseArity = BaseArity.Unary
) : Operation<T>()