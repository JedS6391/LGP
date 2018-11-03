package lgp.lib.operations

import lgp.core.program.instructions.*
import lgp.core.program.registers.Arguments
import lgp.core.modules.ModuleInformation

/**
 * Performs the identity function on a single Double argument.
 */
class Identity : UnaryOperation<Double>(
        func = { args: Arguments<Double> -> args.get(0) }
) {
    override val representation: String
        get() = ""

    override val information = ModuleInformation (
        description = "An operation to copy a single Double argument to the destination register."
    )

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        return "r[$destination] = r[${ operands[0] }]"
    }
}
