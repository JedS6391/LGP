package lgp.lib.operations

import lgp.core.evolution.instructions.BinaryOperation
import lgp.core.evolution.registers.Arguments
import lgp.core.modules.ModuleInformation

/**
 * Performs addition on two Double arguments.
 */
class Addition : BinaryOperation<Double>(Addition.Companion::add) {

    companion object {
        fun add(args: Arguments<Double>): Double {
            return args.get(0) + args.get(1)
        }
    }

    override val representation = " + "

    override val information = ModuleInformation(
            description = "An operation for performing the addition function on two Double arguments."
    )
}

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

/**
 * Performs multiplication on two Double arguments.
 */
class Multiplication : BinaryOperation<Double>(
        func = { args: Arguments<Double> ->
            args.get(0) * args.get(1)
        }
) {
    override val representation = " * "

    override val information = ModuleInformation(
            description = "An operation for performing the multiplication function on two Double arguments."
    )
}

/**
 * Performs division on two Double arguments.
 */
class Division : BinaryOperation<Double>(
        func = { args: Arguments<Double> ->
            when {
                // if (r_k ≠ 0) r_i := r_j /r_k
                args.get(1) != 0.0 -> args.get(0) * args.get(1)
                // else r_i := r_j + C_undef
                else -> args.get(0) + 1.0
            }

        }
) {
    override val representation = " / "

    override val information = ModuleInformation(
            description = "An operation for performing the division function on two Double arguments."
    )
}

/**
 * Performs exponentiation on two Double arguments.
 */
class Exponent : BinaryOperation<Double>(
        func = { args: Arguments<Double> ->
            when {
                // if (|r_k| <= 10) r_i := |r_j| ^ r_k
                Math.abs(args.get(1)) <= 10 -> Math.pow(Math.abs(args.get(0)), args.get(1))
                // else r_i := r_j + r_k + C_undef
                else -> args.get(0) + args.get(1) + 1.0
            }
        }
) {
    override val representation = " ^ "

    override val information = ModuleInformation(
            description = "An operation for performing the exponent function on two Double arguments."
    )
}