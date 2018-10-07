package lgp.core.program

import lgp.core.environment.Environment
import lgp.core.modules.Module
import lgp.core.program.instructions.InstructionGenerator

/**
 * Generates [Program] instances to be used in an LGP operators.
 *
 * The implementation is done as a module, allowing for custom generation logic for programs
 * to be defined. The only requirement is that calls to the [next] method returns a sequence
 * of program samples.
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
     * Generates a sequence of programs by yielding the result of the overridden function [generateProgram].
     *
     * @returns A sequence of programs.
     */
    fun next(): Sequence<Program<T>> = sequence {
        while (true) {
            yield(this@ProgramGenerator.generateProgram())
        }
    }

    /**
     * Generates a single [Program] instance in some way.
     *
     * @returns A program instance.
     */
    abstract fun generateProgram(): Program<T>
}

