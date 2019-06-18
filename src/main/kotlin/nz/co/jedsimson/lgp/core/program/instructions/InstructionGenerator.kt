package nz.co.jedsimson.lgp.core.program.instructions

import nz.co.jedsimson.lgp.core.environment.EnvironmentDefinition
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.modules.Module

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
 * @param TProgram The type that the instructions generated operate on.
 */
abstract class InstructionGenerator<TProgram, TOutput : Output<TProgram>>(
    val environment: EnvironmentDefinition<TProgram, TOutput>
) : Module {

    /**
     * Generates a sequence of instructions by yielding the result of the overridden function [generateInstruction].
     *
     * @returns A sequence of programs.
     */
    fun next(): Sequence<Instruction<TProgram>> = sequence {
        while (true) {
            yield(this@InstructionGenerator.generateInstruction())
        }
    }

    /**
     * Generates a single [Instruction] instance in some way.
     *
     * @returns An instruction instance.
     */
    abstract fun generateInstruction(): Instruction<TProgram>
}