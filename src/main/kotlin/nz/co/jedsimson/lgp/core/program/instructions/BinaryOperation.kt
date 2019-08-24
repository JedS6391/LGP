package nz.co.jedsimson.lgp.core.program.instructions

import nz.co.jedsimson.lgp.core.program.registers.Arguments

/**
 * An operation that has an arity of two (i.e. its function takes 2 arguments).
 */
abstract class BinaryOperation<T>(
    override val function: (Arguments<T>) -> T,
    override val arity: BaseArity = BaseArity.Binary
) : Operation<T>()