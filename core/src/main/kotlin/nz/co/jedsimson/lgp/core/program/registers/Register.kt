package nz.co.jedsimson.lgp.core.program.registers

import nz.co.jedsimson.lgp.core.environment.DefaultValueProvider

typealias RegisterIndex = Int

/**
 * Represents the type of a register.
 */
enum class RegisterType {
    /**
     * A register that contains input values from a data set instance.
     */
    Input,

    /**
     * A register that can be used for calculations and will be loaded
     * with some default value (given by a [DefaultValueProvider]).
     */
    Calculation,

    /**
     * A register that is read-only and will be initialised with values
     * from a set of constants in the LGP environment.
     */
    Constant,

    /**
     * A register that does not fall in some known range will have unknown
     * type. This type primarily serves as a default case when mapping register
     * index ranges to register types.
     */
    Unknown
}

/**
 * A register that is available to an LGP program.
 *
 * Registers contain some value and have a specified index, which
 * defines their position in the context of a [RegisterSet].
 *
 * @param T Type of values the register can contain.
 * @property value A value this register contains.
 * @property index The index of this register in a [RegisterSet].
 */
class Register<T>(var value: T, val index: RegisterIndex) {

    /**
     * Creates a new register that is a clone of [source].
     *
     * @param source A register to create a clone of.
     */
    constructor(source: Register<T>) : this(source.value, source.index)

    /**
     * Converts this register into an argument to be consumed by operations.
     *
     * @returns An [Argument] with the same value that this register contains.
     */
    fun toArgument(): Argument<T> {
        return Argument(this.value)
    }

    override fun toString(): String {
        return "r[${this.index}] = ${this.value}"
    }
}