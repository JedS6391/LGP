package nz.co.jedsimson.lgp.core.program

import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessFunctions
import nz.co.jedsimson.lgp.core.program.registers.RegisterSet
import nz.co.jedsimson.lgp.core.modules.Module
import nz.co.jedsimson.lgp.core.program.instructions.Instruction
import nz.co.jedsimson.lgp.core.program.instructions.RegisterIndex

/**
 * Represents the output of a [Program].
 *
 * @param TData The type of the program output.
 */
interface Output<TData>

/**
 * A collection of built-in [Output] implementations.
 */
object Outputs {
    /**
     * Used for [Program]s with a single output value.
     *
     * @property value The single output value of a program.
     */
    class Single<TData>(val value: TData) : Output<TData>

    /**
     * Used for [Program]s with multiple output values.
     *
     * @property values The output values of a program.
     */
    class Multiple<TData>(val values: List<TData>): Output<TData>
}

/**
 * An LGP program that is composed of instructions that operate on registers.
 *
 * @param TData Type of data the programs instructions operate on.
 * @param TOutput The type of the program output(s).
 * @property instructions A collection of instructions that can be modified.
 * @property registers A set of registers.
 * @property outputRegisterIndices The indices of register(s) that the program uses as output.
 */
abstract class Program<TData, TOutput : Output<TData>>(
    var instructions: MutableList<Instruction<TData>>,
    val registers: RegisterSet<TData>,
    val outputRegisterIndices: List<RegisterIndex>
) : Module {

    /**
     * Used to keep state of the programs fitness value.
     */
    var fitness: Double = FitnessFunctions.UNDEFINED_FITNESS

    /**
     * The instructions of this program that are effective.
     */
    var effectiveInstructions: MutableList<Instruction<TData>> = mutableListOf()

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