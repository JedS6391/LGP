package nz.co.jedsimson.lgp.core.program

import nz.co.jedsimson.lgp.core.environment.EnvironmentDefinition
import nz.co.jedsimson.lgp.core.modules.Module
import nz.co.jedsimson.lgp.core.program.instructions.InstructionGenerator

/**
 * Generates [Program] instances to be used in an LGP population.
 *
 * The implementation is done as a module, allowing for custom generation logic for programs
 * to be defined. The only requirement is that calls to the [next] method returns a sequence
 * of program samples.
 *
 * An implementation is expected to provide an [EnvironmentDefinition], in order to provide access to any
 * component loaded into the the LGP environment, as well as an [InstructionGenerator]. Instruction
 * generators themselves are modules, so each can be defined as a module.
 *
 * @property environment A reference to an LGP environment.
 * @property instructionGenerator A reference to an instruction generator.
 */
abstract class ProgramGenerator<TProgram, TOutput : Output<TProgram>>(
    val environment: EnvironmentDefinition<TProgram, TOutput>,
    private val instructionGenerator: InstructionGenerator<TProgram, TOutput>
) : Module {

    /**
     * Generates a sequence of programs by yielding the result of the overridden function [generateProgram].
     *
     * @returns A sequence of programs.
     */
    fun next(): Sequence<Program<TProgram, TOutput>> = sequence {
        while (true) {
            yield(this@ProgramGenerator.generateProgram())
        }
    }

    /**
     * Generates a single [Program] instance in some way.
     *
     * @returns A program instance.
     */
    abstract fun generateProgram(): Program<TProgram, TOutput>
}

