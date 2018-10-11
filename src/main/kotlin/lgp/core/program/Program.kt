package lgp.core.program

import lgp.core.evolution.fitness.FitnessFunctions
import lgp.core.program.registers.RegisterSet
import lgp.core.modules.Module
import lgp.core.program.instructions.Instruction

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
    val outputRegisterIndex: Int
) : Module {

    /**
     * Used to keep state of the programs fitness value.
     */
    var fitness: Double = FitnessFunctions.UNDEFINED_FITNESS

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

/**
 * Module that can be used to translate programs to external representations.
 *
 * This class primarily exists to make it easy to translate programs from a single place,
 * rather than from internally defined logic.
 */
abstract class ProgramTranslator<TProgram> : Module {

    /**
     * Translates [program] from some internal representation to a concrete representation.
     *
     * This is useful for taking an evolved LGP program and translating it to an output that
     * can be integrated into some other eco-system (e.g. the C programming language).
     *
     * @param program A program to translate.
     * @returns A translated representation of [program] as a string.
     */
    abstract fun translate(program: Program<TProgram>): String
}