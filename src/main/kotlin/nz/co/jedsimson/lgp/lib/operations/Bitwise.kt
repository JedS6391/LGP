package nz.co.jedsimson.lgp.lib.operations

import nz.co.jedsimson.lgp.core.program.instructions.*
import nz.co.jedsimson.lgp.core.program.registers.Arguments
import nz.co.jedsimson.lgp.core.modules.ModuleInformation

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

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        return "r[$destination] = r[${ operands[0] }] > 0.0 ? 0.0 : 1.0"
    }
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

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        return "r[$destination] = (r[${ operands[0] }] > 0.0 & r[${ operands[1] }] > 0.0) ? 1.0 : 0.0"
    }
}

class Or : BinaryOperation<Double>(Or.Companion::or) {

    companion object {
        fun or(args: Arguments<Double>): Double {
            val a = args.get(0).toBoolean()
            val b = args.get(1).toBoolean()

            return a.or(b).toDouble()
        }
    }

    override val representation = " | "

    override val information = ModuleInformation(
            description = "An operation for performing the bitwise or function on two Double arguments."
    )

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        return "r[$destination] = (r[${ operands[0] }] > 0.0 | r[${ operands[1] }] > 0.0) ? 1.0 : 0.0"
    }
}

class ExclusiveOr : BinaryOperation<Double>(ExclusiveOr.Companion::xor) {

    companion object {
        fun xor(args: Arguments<Double>): Double {
            val a = args.get(0).toBoolean()
            val b = args.get(1).toBoolean()

            return a.xor(b).toDouble()
        }
    }

    override val representation = " ^ "

    override val information = ModuleInformation(
            description = "An operation for performing the bitwise xor function on two Double arguments."
    )

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        return "r[$destination] = (r[${ operands[0] }] > 0.0 ^ r[${ operands[1] }] > 0.0) ? 1.0 : 0.0"
    }
}

fun Double.toBoolean(): Boolean {
    return this > 0
}

fun Boolean.toDouble(): Double {
    return if (this) 1.0 else 0.0
}
