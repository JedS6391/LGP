package lgp.lib.operations

import lgp.core.evolution.instructions.BinaryOperation
import lgp.core.evolution.instructions.UnaryOperation
import lgp.core.evolution.registers.Arguments
import lgp.core.modules.ModuleInformation

/**
 * Performs the bitwise not function on a single Double argument.
 *
 * To achieve this, the double input value is converted to a boolean
 * by mapping zero values to false and non-zero values to true.
 *
 * The resulting boolean is then mapped back to a double value such
 * that true maps to 1.0 and false maps to 0.0.
 */
class Not : UnaryOperation<Double>(Not.Companion::not) {

    companion object {
        fun not(args: Arguments<Double>): Double {
            val mapped = args.get(0).toBoolean()

            return mapped.not().toDouble()
        }
    }

    override val representation: String
        get() = "~"

    override val information = ModuleInformation (
            description = "An operation for performing the bitwise not function on a single Double argument."
    )
}

class And : BinaryOperation<Double>(And.Companion::and) {

    companion object {
        fun and(args: Arguments<Double>): Double {
            val a = args.get(0).toBoolean()
            val b = args.get(1).toBoolean()

            return a.and(b).toDouble()
        }
    }

    override val representation = " & "

    override val information = ModuleInformation(
            description = "An operation for performing the bitwise and function on two Double arguments."
    )
}

class Or : BinaryOperation<Double>(And.Companion::and) {

    companion object {
        fun and(args: Arguments<Double>): Double {
            val a = args.get(0).toBoolean()
            val b = args.get(1).toBoolean()

            return a.or(b).toDouble()
        }
    }

    override val representation = " | "

    override val information = ModuleInformation(
            description = "An operation for performing the bitwise or function on two Double arguments."
    )
}

fun Double.toBoolean(): Boolean {
    return this > 0
}

fun Boolean.toDouble(): Double {
    return if (this) 1.0 else 0.0
}