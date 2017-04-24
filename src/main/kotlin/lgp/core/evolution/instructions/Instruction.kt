package lgp.core.evolution.instructions

import lgp.core.evolution.registers.Register
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.Module

abstract class Instruction<T> : Module {
    abstract var destination: Register<T>

    abstract var operands: MutableList<Register<T>>

    abstract var operation: Operation<T>

    abstract fun execute(registers: RegisterSet<T>)

    abstract fun copy(): Instruction<T>
}