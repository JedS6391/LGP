package lgp.core.evolution.registers

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
class Register<T>(var value: T, val index: Int) {

    // TODO: Deep copy value?
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