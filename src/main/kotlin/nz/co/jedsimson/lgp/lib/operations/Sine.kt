package nz.co.jedsimson.lgp.lib.operations

import nz.co.jedsimson.lgp.core.program.instructions.*
import nz.co.jedsimson.lgp.core.program.registers.Arguments
import nz.co.jedsimson.lgp.core.modules.ModuleInformation

/**
 * Performs the sine function on a single Double argument.
 */
class Sine : UnaryOperation<Double>(
        func = { args: Arguments<Double> -> Math.sin(args.get(0)) }
) {
    override val representation: String
        get() = "sin"

    override val information = ModuleInformation (
        description = "An operation for performing the sine function on a single Double argument."
    )

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        return "r[$destination] = sin(r[${ operands[0] }])"
    }
}
