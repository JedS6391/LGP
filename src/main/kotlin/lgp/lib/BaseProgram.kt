package lgp.lib

import lgp.core.evolution.instructions.Instruction
import lgp.core.evolution.population.Program
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.ModuleInformation

class BaseProgram<T>(instructions: Sequence<Instruction<T>>, registerSet: RegisterSet<T>)
    : Program<T>(instructions, registerSet) {

    override fun execute() {
        for (instruction in this.instructions) {
            instruction.execute(this.registers)
        }
    }

    override val information: ModuleInformation = object : ModuleInformation {
        override val description: String
            get() = "A simple program that executes instructions sequentially."
    }
}