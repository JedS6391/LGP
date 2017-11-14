package lgp.core.evolution.instructions

import lgp.core.environment.Environment
import lgp.core.modules.Module

import kotlin.coroutines.experimental.buildSequence

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
     * Generates a sequence of instructions by yielding the result of the overridden function [generateInstruction].
     *
     * @returns A sequence of programs.
     */
    final fun next(): Sequence<Instruction<T>> = buildSequence {
        while (true) {
            yield(this@InstructionGenerator.generateInstruction())
        }
    }

    /**
     * Generates a single [Instruction] instance in some way.
     *
     * @returns An instruction instance.
     */
    abstract fun generateInstruction(): Instruction<T>
}