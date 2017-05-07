package lgp.lib.operations

import lgp.core.evolution.instructions.BinaryOperation
import lgp.core.evolution.instructions.BranchOperation
import lgp.core.evolution.registers.Arguments
import lgp.core.modules.ModuleInformation

/**
 * TODO
 */
class IfGreater : BranchOperation<Double>(
        func = { args: Arguments<Double> ->
            if (args.get(0) > args.get(1)) 1.0 else 0.0
        }
) {
    override val representation = " > "

    override val information = ModuleInformation(
            description = ""
    )
}

/**
 * TODO
 */
class IfLessThanOrEqualTo : BranchOperation<Double>(
        func = { args: Arguments<Double> ->
            if (args.get(0) <= args.get(1)) 1.0 else 0.0
        }
) {
    override val representation = " < "

    override val information = ModuleInformation(
            description = ""
    )
}