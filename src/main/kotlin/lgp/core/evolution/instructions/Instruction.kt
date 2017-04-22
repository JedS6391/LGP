package lgp.core.evolution.instructions

import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.Module

abstract class Instruction<T> : Module {
    abstract val operation: Operation<T>

    abstract fun execute(registers: RegisterSet<T>)

    abstract fun copy(): Instruction<T>
}