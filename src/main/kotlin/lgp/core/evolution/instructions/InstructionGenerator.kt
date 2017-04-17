package lgp.core.evolution.instructions

import lgp.core.environment.Environment
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.Module

/**
 * A generator of instructions in an LGP system.
 *
 * The functionality is left up to the implementer, but the basic skeleton exposes a simple API,
 * that allows the instruction generator to be iterated over as a sequence of instructors, for example:
 *
 * ```
 * val instructionGenerator = SomeInstructionGenerator()
 *
 * // Iterate through the sequence of instructions
 * for (instruction in instructionGenerator.next()) {
 *     doSomethingWith(instruction)
 * }
 * ```
 *
 * An instruction generator expects that a set of registers is provided to it. Note that this register
 * set will provide registers
 *
 * @param T The type that the instructions generated operate on.
 */
abstract class InstructionGenerator<T>(val environment: Environment<T>) : Module {

    /**
     * Gives a sequence of instructions.
     */
    abstract fun next(): Sequence<Instruction<T>>
}