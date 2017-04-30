package lgp.core.evolution.population

import lgp.core.evolution.instructions.Instruction
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.Module

/**
 * An LGP program that is composed of instructions that operate on registers.
 *
 * @param T Type of data the programs instructions operate on.
 * @property instructions A collection of instructions that can be modified.
 * @property registers A set of registers.
 * @property outputRegisterIndex An index of a register that the program uses as output.
 */
abstract class Program<T>(
        var instructions: MutableList<Instruction<T>>,
        val registers: RegisterSet<T>,
        val outputRegisterIndex: Int     // TODO: Add support for multi-output programs
) : Module {

    /**
     * Used to keep state of the programs fitness value.
     */
    var fitness: Double = 0.0

    /**
     * The instructions of this program that are effective.
     */
    var effectiveInstructions: MutableList<Instruction<T>> = mutableListOf()

    /**
     * Executes the program.
     */
    abstract fun execute()

    /**
     * Creates a clone of the given program.
     *
     * When copying a program, a deep copy should be provided,
     * to ensure that when a program is modified, it does not
     * effect the state of other programs.
     *
     * @returns A copy of the given program.
     */
    abstract fun copy(): Program<T>

    /**
     * Provides a way to find an LGP programs effective set of instructions.
     */
    abstract fun findEffectiveProgram()
}