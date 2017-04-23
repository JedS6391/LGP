package lgp.core.evolution.population

import lgp.core.evolution.instructions.BranchOperation
import lgp.core.evolution.instructions.Instruction
import lgp.core.evolution.registers.RegisterSet
import lgp.core.evolution.registers.copy
import lgp.core.modules.Module

/**
 * A program in the LGP population.
 */
abstract class Program<T>(
        var instructions: MutableList<Instruction<T>>,
        val registers: RegisterSet<T>
) : Module {

    var fitness: Double = 0.0

    // Empty to begin
    var effectiveInstructions: MutableList<Instruction<T>> = mutableListOf()

    // TODO: Make this configurable
    val outputRegisterIdx = 0

    abstract fun execute()

    abstract fun copy(): Program<T>

    abstract fun findEffectiveProgram()
}