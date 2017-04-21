package lgp.lib

import lgp.core.evolution.instructions.Instruction
import lgp.core.evolution.population.Program
import lgp.core.evolution.registers.RegisterSet
import lgp.core.evolution.registers.copy
import lgp.core.modules.ModuleInformation

class BaseProgram<T>(instructions: Sequence<Instruction<T>>, registerSet: RegisterSet<T>)
    : Program<T>(instructions, registerSet) {

    override fun execute() {
        for (instruction in this.instructions) {
            instruction.execute(this.registers)
        }
    }

    override fun copy(): BaseProgram<T> {
        return BaseProgram(
                instructions = this.instructions,
                registerSet = this.registers.copy()
        )
    }

    override val information = ModuleInformation(
        description = "A simple program that executes instructions sequentially."
    )
}