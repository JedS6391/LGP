package nz.co.jedsimson.lgp.core.program.registers

import nz.co.jedsimson.lgp.core.program.registers.RegisterIndex
import kotlin.random.Random

/**
 * Generates an infinite random sequence of registers from the register set given.
 *
 * @param T The type of value the registers contain.
 * @property registerSet A set of registers to choose random registers from.
 */
class RandomRegisterGenerator<T>(internal val randomState: Random, private val registerSet: RegisterSet<T>) {

    /**
     * Provides an infinite, random sequence of registers.
     *
     * @returns A sequence of registers.
     */
    fun next(): Sequence<Register<T>> = generateSequence {
        val idx = randomState.nextInt(registerSet.count)

        // Let's just be extra cautious
        assert(0 <= idx && idx <= registerSet.count)

        registerSet.register(idx)
    }

    /**
     * Returns a sequence of registers with the specified type.
     *
     * @param type A type of register to filter by.
     * @returns A sequence of registers such that type(register) == type.
     */
    fun next(type: RegisterType): Sequence<Register<T>> {
        // Keep taking from the sequence until we get a register we're looking for.
        return this.next().filter { r ->
            this.registerSet.registerType(r.index) == type
        }
    }

    /**
     * Returns a sequence of integers where [predicate] determines
     * whether the next register is of type [a] or type [b].
     *
     * @param a The first register type.
     * @param b The second register type.
     * @param predicate A function that determines between register type a and b.
     */
    fun next(a: RegisterType, b: RegisterType, predicate: () -> Boolean): Sequence<Register<T>> = generateSequence {
        val typeToGive = if (predicate()) a else b

        this.next(typeToGive).first()
    }
}

/**
 * Gets [count] registers with a random distribution of [RegisterType.Input] and [RegisterType.Calculation].
 *
 * @param count The number of registers to generate.
 */
fun <TRegister> RandomRegisterGenerator<TRegister>.getRandomInputAndCalculationRegisters(
    count: Int
): MutableList<RegisterIndex> {
    val registers = this.next(
        a = RegisterType.Calculation,
        b = RegisterType.Input,
        // 50% probability of an input or calculation register
        predicate = { this.randomState.nextDouble() < 0.5 }
    )
    .take(count)
    .map(Register<TRegister>::index)

    return registers.toMutableList()
}