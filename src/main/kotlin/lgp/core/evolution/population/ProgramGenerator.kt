package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.evolution.instructions.InstructionGenerator
import lgp.core.modules.Module
import kotlin.coroutines.experimental.buildSequence

/**
 * Generates [Program] instances to be used in an LGP population.
 *
 * The implementation is done as a module, allowing for custom generation logic for programs
 * to be defined. The only requirement is that calls to the [next] method returns a sequence
 * of program instances.
 *
 * An implementation is expected to provide an [Environment], in order to provide access to any
 * component loaded into the the LGP environment, as well as an [InstructionGenerator]. Instruction
 * generators themselves are modules, so each can be defined as a module.
 *
 * @property environment A reference to an LGP environment.
 * @property instructionGenerator A reference to an instruction generator.
 */
abstract class ProgramGenerator<T>(val environment: Environment<T>, val instructionGenerator: InstructionGenerator<T>) : Module {
    /**
     * Generates a sequence of programs by yielding the result
     * of the overridden function [generateProgram].
     *
     * @returns A sequence of programs.
     */
    final fun next(): Sequence<Program<T>> = buildSequence {
        while (true) {
            yield(this@ProgramGenerator.generateProgram())
        }
    }

    /**
     * Should generate a program instance in some way.
     *
     * @returns A program instance.
     */
    abstract fun generateProgram(): Program<T>
}

