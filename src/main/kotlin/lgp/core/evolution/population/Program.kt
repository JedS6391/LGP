package lgp.core.evolution.population

import lgp.core.evolution.instructions.Instruction
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.Module

/**
 * A program in the LGP population.
 */
abstract class Program<T>(
        var instructions: MutableList<Instruction<T>>,
        val registers: RegisterSet<T>,
        val outputRegisterIndex: Int     // TODO: Add support for multi-output programs
) : Module {

    var fitness: Double = 0.0

    // Empty to begin
    var effectiveInstructions: MutableList<Instruction<T>> = mutableListOf()

    abstract fun execute()

    abstract fun copy(): Program<T>

    abstract fun findEffectiveProgram()
}