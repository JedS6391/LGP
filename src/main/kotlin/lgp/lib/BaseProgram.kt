package lgp.lib

import lgp.core.evolution.instructions.Instruction
import lgp.core.evolution.population.Program
import lgp.core.evolution.registers.RegisterSet
import lgp.core.evolution.registers.copy
import lgp.core.modules.ModuleInformation

class BaseProgram<T>(instructions: List<Instruction<T>>, registerSet: RegisterSet<T>)
    : Program<T>(instructions.toMutableList(), registerSet) {

    override fun execute() {
        for (instruction in this.instructions) {
            instruction.execute(this.registers)
        }
    }

    override fun copy(): BaseProgram<T> {
        return BaseProgram(
                instructions = this.instructions.map(Instruction<T>::copy),
                registerSet = this.registers.copy()
        )
    }

    override fun toString(): String {
        val sb = StringBuilder()

        this.instructions.map { instruction ->
            sb.append(instruction.toString())
            sb.append('\n')
        }

        return sb.toString()
    }

    override val information = ModuleInformation(
        description = "A simple program that executes instructions sequentially."
    )
}