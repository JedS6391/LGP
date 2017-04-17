package lgp.lib

import lgp.core.environment.Environment
import lgp.core.environment.RegisteredModuleType
import lgp.core.evolution.population.Program
import lgp.core.evolution.population.ProgramGenerator
import lgp.core.evolution.registers.copy
import lgp.core.modules.ModuleInformation
import lgp.lib.BaseInstructionGenerator
import lgp.lib.BaseProgram
import java.util.*

/**
 * @suppress
 */
class BaseProgramGenerator<T>(environment: Environment<T>)
    : ProgramGenerator<T>(environment, instructionGenerator = environment.registeredModule(RegisteredModuleType.InstructionGenerator)) {

    private val rg = Random()

    override fun generateProgram(): Program<T> {
        val length = this.rg.randint(this.environment.config.initialMinimumProgramLength,
                                     this.environment.config.initialMaximumProgramLength)

        val instructions = this.instructionGenerator.next().take(length)

        // Each program gets its own copy of the register set
        val program = BaseProgram(instructions, this.environment.registerSet.copy())

        return program
    }

    override val information: ModuleInformation = object : ModuleInformation {
        override val description: String
            get() = "A simple program generator."
    }

}

// A random integer between a and b.
// a <= b
fun Random.randint(a: Int, b: Int): Int {
    assert(a <= b)

    return this.nextInt((b - a) + 1) + a
}