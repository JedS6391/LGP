package lgp.lib.operations

import lgp.core.evolution.instructions.BinaryOperation
import lgp.core.evolution.registers.Arguments
import lgp.core.modules.ModuleInformation

/**
 * Performs subtraction on two Double arguments.
 */
class Subtraction : BinaryOperation<Double>(
        func = { args: Arguments<Double> ->
            args.get(0) - args.get(1)
        }
) {
    override val representation = " - "

    override val information = ModuleInformation(
        description = "An operation for performing the subtraction function on two Double arguments."
    )
}