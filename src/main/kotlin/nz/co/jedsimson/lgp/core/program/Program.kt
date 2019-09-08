package nz.co.jedsimson.lgp.core.program

import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessFunctions
import nz.co.jedsimson.lgp.core.program.registers.RegisterSet
import nz.co.jedsimson.lgp.core.modules.Module
import nz.co.jedsimson.lgp.core.program.instructions.Instruction
import nz.co.jedsimson.lgp.core.program.registers.RegisterIndex

/**
 * An LGP program that is composed of instructions that operate on registers.
 *
 * @param TData Type of data the programs instructions operate on.
 * @param TOutput The type of the program output(s).
 */
abstract class Program<TData, TOutput : Output<TData>> : Module {

    /**
     * Represents the fitness of this [Program].
     *
     * When a [Program] is first initialised, the value will be [FitnessFunctions.UNDEFINED_FITNESS].
     */
    open var fitness: Double = FitnessFunctions.UNDEFINED_FITNESS

    /**
     * The instructions in this [Program] that are effective.
     *
     * By default, this list is empty. It is expected that a call to [Program.findEffectiveProgram]
     * would populate this list.
     */
    open var effectiveInstructions: MutableList<Instruction<TData>> = mutableListOf()

    /**
     * The instructions in this [Program].
     *
     * This collection can be modified to modify the behaviour of this [Program].
     */
    abstract var instructions: MutableList<Instruction<TData>>

    /**
     * The set of registers that this [Program] runs against.
     */
    abstract val registers: RegisterSet<TData>

    /**
     * The indices of register(s) that the program uses as output.
     */
    abstract val outputRegisterIndices: List<RegisterIndex>

    /**
     * Retrieves the program output.
     */
    abstract fun output(): TOutput

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
    abstract fun copy(): Program<TData, TOutput>

    /**
     * Provides a way to find an LGP programs effective set of instructions.
     */
    abstract fun findEffectiveProgram()
}