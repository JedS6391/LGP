package lgp.core.evolution.registers

/**
 * An [Argument] is essentially just the value of a particular [Register],
 * but in the context of an [lgp.core.evolution.instructions.Operation].
 *
 * When the value of a register is to be passed as an operand to some
 * operation (and the function that operation applies), it should be mapped
 * to an argument type.
 *
 * @param T Type of value this argument represents.
 * @property value The value itself.
 */
data class Argument<out T>(val value: T)