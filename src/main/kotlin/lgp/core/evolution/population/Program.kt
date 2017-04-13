package lgp.core.evolution.population

import lgp.core.evolution.instructions.Instruction
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.Module

/**
 * A program in the LGP population.
 */
abstract class Program<T>(
        val instructions: Sequence<Instruction<T>>,
        val registers: RegisterSet<T>
) : Module {
    abstract fun execute()
}