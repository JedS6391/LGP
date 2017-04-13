package lgp.lib.operations

import lgp.core.evolution.instructions.UnaryOperation
import lgp.core.evolution.registers.Arguments
import lgp.core.modules.ModuleInformation

/**
 * Performs the sine function on a single Double argument.
 *
 * @suppress
 */
class Sine : UnaryOperation<Double>(
        func = { args: Arguments<Double> -> Math.sin(args.get(0)) }
) {
    override val representation: String
        get() = "sin"

    override val information: ModuleInformation = object : ModuleInformation {
        override val description: String
            get() = "An operation for performing the sine function on a single Double argument."
    }
}